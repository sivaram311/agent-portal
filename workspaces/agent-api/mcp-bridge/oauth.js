/**
 * MCP-standard OAuth 2.1 (RFC 8414 + RFC 9728 + RFC 7591 DCR + PKCE).
 * Tuned for Gemini Spark — authorization_code only, public clients preferred.
 */
import { createHash, randomBytes, randomUUID, timingSafeEqual } from 'node:crypto';
import express from 'express';

export function createOauth({
  publicBase,
  hostOrigin,
  mcpPathPrefix = '/mcp',
  fallbackClientId,
  fallbackClientSecret,
  accessTokenTtlSec = 3600,
}) {
  const codes = new Map();
  const tokens = new Map();
  /** @type {Map<string, { secret: string|null, redirectUris: string[], authMethod: string }>} */
  const clients = new Map();

  if (fallbackClientId) {
    clients.set(fallbackClientId, {
      secret: fallbackClientSecret || null,
      redirectUris: [],
      authMethod: fallbackClientSecret ? 'client_secret_post' : 'none',
    });
  }

  function safeEqual(a, b) {
    const ba = Buffer.from(String(a ?? ''));
    const bb = Buffer.from(String(b ?? ''));
    if (ba.length !== bb.length) return false;
    return timingSafeEqual(ba, bb);
  }

  function issueAccessToken(clientId) {
    const accessToken = randomBytes(32).toString('hex');
    tokens.set(accessToken, {
      clientId,
      exp: Date.now() + accessTokenTtlSec * 1000,
    });
    return accessToken;
  }

  function isValidAccessToken(token) {
    if (!token) return false;
    const row = tokens.get(token);
    if (!row || row.refresh) return false;
    if (Date.now() >= row.exp) {
      tokens.delete(token);
      return false;
    }
    return true;
  }

  function parseBasicAuth(req) {
    const h = req.headers.authorization || '';
    if (!h.startsWith('Basic ')) return null;
    try {
      const decoded = Buffer.from(h.slice(6), 'base64').toString('utf8');
      const i = decoded.indexOf(':');
      if (i < 0) return null;
      return {
        id: decodeURIComponent(decoded.slice(0, i)),
        secret: decodeURIComponent(decoded.slice(i + 1)),
      };
    } catch {
      return null;
    }
  }

  function verifyPkce(verifier, challenge, method) {
    if (!challenge) return true;
    const m = (method || 'S256').toUpperCase();
    if (m === 'PLAIN') return safeEqual(verifier, challenge);
    const hash = createHash('sha256').update(verifier || '').digest('base64url');
    return safeEqual(hash, challenge);
  }

  function authServerMeta() {
    return {
      issuer: publicBase,
      authorization_endpoint: `${publicBase}/authorize`,
      token_endpoint: `${publicBase}/token`,
      registration_endpoint: `${publicBase}/register`,
      response_types_supported: ['code'],
      grant_types_supported: ['authorization_code', 'refresh_token'],
      code_challenge_methods_supported: ['S256'],
      token_endpoint_auth_methods_supported: ['none', 'client_secret_post', 'client_secret_basic'],
      scopes_supported: ['mcp'],
      revocation_endpoint_auth_methods_supported: ['none'],
    };
  }

  function resourceMeta(resourceUrl) {
    return {
      resource: resourceUrl,
      authorization_servers: [publicBase],
      bearer_methods_supported: ['header'],
      scopes_supported: ['mcp'],
    };
  }

  /** RFC 9728 path-inserted metadata URLs for a resource. */
  function resourceMetadataUrlFor(resourceUrl) {
    const u = new URL(resourceUrl);
    const path = u.pathname.replace(/\/$/, '') || '';
    return `${u.protocol}//${u.host}/.well-known/oauth-protected-resource${path}`;
  }

  function mount(app) {
    const sendAs = (_req, res) => res.json(authServerMeta());

    // Under /mcp prefix (nginx strips /mcp/)
    app.get('/.well-known/oauth-authorization-server', sendAs);
    app.get('/.well-known/oauth-authorization-server/sse', sendAs);
    app.get('/.well-known/oauth-authorization-server/mcp', sendAs);

    // Root-style paths when nginx proxies /.well-known/oauth-* → bridge
    app.get(`${mcpPathPrefix ? '' : ''}/.well-known/oauth-authorization-server${mcpPathPrefix}`, sendAs);
    app.get(`/.well-known/oauth-authorization-server${mcpPathPrefix}`, sendAs);
    app.get(`/.well-known/oauth-authorization-server${mcpPathPrefix}/sse`, sendAs);
    app.get(`/.well-known/oauth-authorization-server${mcpPathPrefix}/mcp`, sendAs);
    app.get(`/.well-known/openid-configuration${mcpPathPrefix}`, sendAs);

    const resources = [
      publicBase,
      `${publicBase}/sse`,
      `${publicBase}/mcp`,
      `${hostOrigin}${mcpPathPrefix}`,
      `${hostOrigin}${mcpPathPrefix}/sse`,
      `${hostOrigin}${mcpPathPrefix}/mcp`,
    ];

    for (const resource of new Set(resources)) {
      const u = new URL(resource);
      const pathSuffix = u.pathname.replace(/\/$/, '') || '';
      // Served when request hits bridge via /mcp/.well-known/... (strip) OR root /.well-known/...
      app.get(`/.well-known/oauth-protected-resource${pathSuffix}`, (_req, res) => {
        res.json(resourceMeta(resource));
      });
      // Also path after strip when resource is /mcp/sse → suffix /sse relative to mount
      if (pathSuffix.startsWith(mcpPathPrefix)) {
        const rel = pathSuffix.slice(mcpPathPrefix.length) || '';
        app.get(`/.well-known/oauth-protected-resource${rel}`, (_req, res) => {
          res.json(resourceMeta(resource));
        });
        if (rel === '') {
          app.get('/.well-known/oauth-protected-resource', (_req, res) => {
            res.json(resourceMeta(resource));
          });
        }
      }
    }

    // Explicit aliases Spark/Gemini try most often
    app.get('/.well-known/oauth-protected-resource/mcp/sse', (_req, res) => {
      res.json(resourceMeta(`${hostOrigin}${mcpPathPrefix}/sse`));
    });
    app.get('/.well-known/oauth-protected-resource/mcp/mcp', (_req, res) => {
      res.json(resourceMeta(`${hostOrigin}${mcpPathPrefix}/mcp`));
    });
    app.get('/.well-known/oauth-protected-resource/mcp', (_req, res) => {
      res.json(resourceMeta(`${hostOrigin}${mcpPathPrefix}`));
    });
    app.get('/.well-known/oauth-protected-resource/sse', (_req, res) => {
      res.json(resourceMeta(`${publicBase}/sse`));
    });

    /** RFC 7591 Dynamic Client Registration — public PKCE client (Spark's preferred path). */
    app.post('/register', (req, res) => {
      const body = req.body || {};
      const redirectUris = Array.isArray(body.redirect_uris) ? body.redirect_uris : [];
      const clientId = `spark-${randomUUID()}`;
      clients.set(clientId, {
        secret: null,
        redirectUris,
        authMethod: 'none',
      });
      res.status(201).json({
        client_id: clientId,
        client_id_issued_at: Math.floor(Date.now() / 1000),
        client_secret_expires_at: 0,
        redirect_uris: redirectUris,
        grant_types: ['authorization_code', 'refresh_token'],
        response_types: ['code'],
        token_endpoint_auth_method: 'none',
        client_name: body.client_name || 'Gemini Spark',
      });
    });

    app.get('/authorize', (req, res) => {
      const {
        client_id,
        redirect_uri,
        response_type,
        state,
        code_challenge,
        code_challenge_method,
        scope,
      } = req.query;

      if (!client_id || !clients.has(String(client_id))) {
        return res.status(400).send('Unknown client_id — use Dynamic Client Registration or reconnect without Advanced credentials');
      }
      if (response_type && response_type !== 'code') {
        return res.status(400).send('Only response_type=code is supported');
      }
      if (!redirect_uri) {
        return res.status(400).send('redirect_uri required');
      }
      if (!code_challenge) {
        return res.status(400).send('PKCE code_challenge required');
      }
      const method = String(code_challenge_method || 'S256').toUpperCase();
      if (method !== 'S256') {
        return res.status(400).send('Only S256 PKCE is supported');
      }

      const code = randomUUID().replace(/-/g, '');
      codes.set(code, {
        clientId: String(client_id),
        redirectUri: String(redirect_uri),
        challenge: String(code_challenge),
        method,
        scope: scope ? String(scope) : 'mcp',
        exp: Date.now() + 5 * 60 * 1000,
      });

      // Simple consent page (auto-continue) — standard browser OAuth UX
      const next = new URL(String(redirect_uri));
      next.searchParams.set('code', code);
      if (state) next.searchParams.set('state', String(state));
      const dest = next.toString();
      res.setHeader('Content-Type', 'text/html; charset=utf-8');
      res.send(`<!doctype html><html><head><meta charset="utf-8"><title>Authorize Agent Portal</title>
<style>body{font-family:system-ui;max-width:480px;margin:10vh auto;padding:1.5rem;background:#0f172a;color:#f8fafc}
button{background:#14b8a6;border:0;color:#042f2e;padding:.75rem 1.25rem;border-radius:8px;font-weight:600;cursor:pointer;width:100%}
a{color:#5eead4}</style></head><body>
<h1>Agent Portal MCP</h1>
<p>Gemini Spark wants access to your Agent Portal (Cursor CLI) tools.</p>
<p><button onclick="location.href=${JSON.stringify(dest)}">Allow</button></p>
<p style="opacity:.7;font-size:.9rem">Or <a href="${dest}">continue</a>.</p>
<script>setTimeout(function(){location.href=${JSON.stringify(dest)}},800)</script>
</body></html>`);
    });

    app.post('/token', express.urlencoded({ extended: false }), (req, res) => {
      const body = req.body || {};
      const basic = parseBasicAuth(req);
      const id = body.client_id || basic?.id;
      const secret = body.client_secret || basic?.secret;
      const grant = body.grant_type;

      if (grant === 'authorization_code') {
        const client = clients.get(id);
        if (!client) {
          return res.status(401).json({ error: 'invalid_client' });
        }
        if (client.authMethod !== 'none') {
          if (!secret || !client.secret || !safeEqual(secret, client.secret)) {
            return res.status(401).json({ error: 'invalid_client' });
          }
        }
        const row = codes.get(body.code);
        if (!row || Date.now() >= row.exp || row.clientId !== id) {
          return res.status(400).json({ error: 'invalid_grant' });
        }
        if (body.redirect_uri && body.redirect_uri !== row.redirectUri) {
          return res.status(400).json({ error: 'invalid_grant', error_description: 'redirect_uri mismatch' });
        }
        if (!verifyPkce(body.code_verifier || '', row.challenge, row.method)) {
          return res.status(400).json({ error: 'invalid_grant', error_description: 'pkce failed' });
        }
        codes.delete(body.code);
        const accessToken = issueAccessToken(id);
        const refreshToken = randomBytes(24).toString('hex');
        tokens.set(refreshToken, {
          clientId: id,
          exp: Date.now() + 30 * 24 * 3600 * 1000,
          refresh: true,
        });
        return res.json({
          access_token: accessToken,
          token_type: 'Bearer',
          expires_in: accessTokenTtlSec,
          refresh_token: refreshToken,
          scope: row.scope || 'mcp',
        });
      }

      if (grant === 'refresh_token') {
        const row = tokens.get(body.refresh_token);
        if (!row || !row.refresh || Date.now() >= row.exp) {
          return res.status(400).json({ error: 'invalid_grant' });
        }
        const client = clients.get(row.clientId);
        if (!client) {
          return res.status(401).json({ error: 'invalid_client' });
        }
        if (client.authMethod !== 'none') {
          if (!secret || !client.secret || !safeEqual(secret, client.secret)) {
            return res.status(401).json({ error: 'invalid_client' });
          }
        }
        const accessToken = issueAccessToken(row.clientId);
        return res.json({
          access_token: accessToken,
          token_type: 'Bearer',
          expires_in: accessTokenTtlSec,
          scope: 'mcp',
        });
      }

      return res.status(400).json({
        error: 'unsupported_grant_type',
        error_description: 'Use authorization_code (OAuth) with PKCE — Gemini Spark standard flow',
      });
    });
  }

  return {
    mount,
    isValidAccessToken,
    resourceMetadataUrlFor,
    publicBase,
  };
}
