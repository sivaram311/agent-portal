/**
 * Agent Portal → Gemini Spark MCP bridge (SSE + Streamable HTTP).
 *
 * Spark URL (behind nginx): https://agent-portal.delena.buzz/mcp/sse
 */
import express from 'express';
import { randomUUID } from 'node:crypto';
import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { SSEServerTransport } from '@modelcontextprotocol/sdk/server/sse.js';
import { StreamableHTTPServerTransport } from '@modelcontextprotocol/sdk/server/streamableHttp.js';
import { CallToolRequestSchema, ListToolsRequestSchema } from '@modelcontextprotocol/sdk/types.js';
import { createOauth } from './oauth.js';

const PORT = Number(process.env.PORT || 5430);
const PORTAL_URL = (process.env.PORTAL_URL || 'http://127.0.0.1:5080').replace(/\/$/, '');
const CSS_AUTH_URL = (process.env.CSS_AUTH_URL || PORTAL_URL).replace(/\/$/, '');
const CSS_CLIENT_ID = process.env.CSS_CLIENT_ID || 'agent-portal';
const CSS_USERNAME = process.env.CSS_USERNAME || '';
const CSS_PASSWORD = process.env.CSS_PASSWORD || '';
const PORTAL_API_KEY = process.env.PORTAL_API_KEY || '';
const MCP_BEARER_TOKEN = process.env.MCP_BEARER_TOKEN || '';
/** Public path prefix Spark uses (nginx strips this before proxy). */
const MCP_BASE_PATH = (process.env.MCP_BASE_PATH || '/mcp').replace(/\/$/, '');
const HOST_ORIGIN = (process.env.MCP_HOST_ORIGIN || 'https://agent-portal.delena.buzz').replace(
  /\/$/,
  ''
);
const PUBLIC_BASE =
  process.env.MCP_PUBLIC_BASE || `${HOST_ORIGIN}${MCP_BASE_PATH}`;
const MCP_OAUTH_CLIENT_ID = process.env.MCP_OAUTH_CLIENT_ID || 'agent-portal-spark';
const MCP_OAUTH_CLIENT_SECRET = process.env.MCP_OAUTH_CLIENT_SECRET || '';
const DEFAULT_WORKSPACE = process.env.DEFAULT_WORKSPACE || 'agent-api';
const DEFAULT_PROVIDER = process.env.DEFAULT_PROVIDER || 'cursor';
/** Polling defaults for waitForIdle. timeoutMs (env/arg) wins over maxAttempts. */
const DEFAULT_WAIT_MAX_ATTEMPTS = Number(process.env.MCP_WAIT_MAX_ATTEMPTS || 300);
const DEFAULT_WAIT_INTERVAL_MS = Number(process.env.MCP_WAIT_INTERVAL_MS || 1000);
const DEFAULT_WAIT_TIMEOUT_MS = process.env.MCP_WAIT_TIMEOUT_MS
  ? Number(process.env.MCP_WAIT_TIMEOUT_MS)
  : null;

const oauth = createOauth({
  publicBase: PUBLIC_BASE,
  hostOrigin: HOST_ORIGIN,
  mcpPathPrefix: MCP_BASE_PATH,
  fallbackClientId: MCP_OAUTH_CLIENT_ID,
  fallbackClientSecret: MCP_OAUTH_CLIENT_SECRET || null,
});

let cachedToken = null;
let tokenExpiresAt = 0;

const tools = [
  {
    name: 'portal_health',
    description: 'Probe Agent Portal health and capability badges.',
    inputSchema: { type: 'object', properties: {} },
  },
  {
    name: 'list_sessions',
    description: 'List Agent Portal sessions (active and archived).',
    inputSchema: { type: 'object', properties: {} },
  },
  {
    name: 'create_session',
    description: 'Create a new Cursor/Antigravity agent session.',
    inputSchema: {
      type: 'object',
      properties: {
        title: { type: 'string', description: 'Session title' },
        workspacePath: {
          type: 'string',
          description: `Workspace path (default: ${DEFAULT_WORKSPACE})`,
        },
        provider: {
          type: 'string',
          enum: ['cursor', 'antigravity'],
          description: `Provider (default: ${DEFAULT_PROVIDER})`,
        },
      },
      required: ['title'],
    },
  },
  {
    name: 'send_prompt',
    description:
      'Send a prompt to a session, wait until the run leaves STREAMING, return the latest assistant reply.',
    inputSchema: {
      type: 'object',
      properties: {
        sessionId: { type: 'string', description: 'Session UUID' },
        prompt: { type: 'string', description: 'User prompt text' },
        timeoutMs: {
          type: 'number',
          description:
            'Max ms to wait for idle (overrides MCP_WAIT_TIMEOUT_MS / maxAttempts). On timeout, returns an error with sessionId.',
        },
      },
      required: ['sessionId', 'prompt'],
    },
  },
  {
    name: 'get_session_transcript',
    description: 'Fetch the full message transcript for a session.',
    inputSchema: {
      type: 'object',
      properties: {
        sessionId: { type: 'string', description: 'Session UUID' },
      },
      required: ['sessionId'],
    },
  },
  {
    name: 'cancel_run',
    description: 'Cancel an in-flight agent run for a session.',
    inputSchema: {
      type: 'object',
      properties: {
        sessionId: { type: 'string', description: 'Session UUID' },
      },
      required: ['sessionId'],
    },
  },
  {
    name: 'machine_context',
    description: 'Fetch redacted live host Machine Gateway context.',
    inputSchema: { type: 'object', properties: {} },
  },
  {
    name: 'machine_chat',
    description:
      'Start a Machine Gateway chat (async accept). Optionally wait and return the assistant reply.',
    inputSchema: {
      type: 'object',
      properties: {
        message: { type: 'string', description: 'User message' },
        mode: {
          type: 'string',
          enum: ['observe', 'advise', 'act', 'ops'],
          description: 'Gateway mode (default act)',
        },
        provider: {
          type: 'string',
          enum: ['cursor', 'antigravity'],
        },
        sessionId: {
          type: 'string',
          description: 'Reuse an existing gateway session UUID',
        },
        waitForReply: {
          type: 'boolean',
          description: 'If true, poll until reply (default true)',
        },
        timeoutMs: {
          type: 'number',
          description:
            'Max ms to wait for idle when waitForReply=true (overrides MCP_WAIT_TIMEOUT_MS / maxAttempts).',
        },
      },
      required: ['message'],
    },
  },
];

async function loginCss() {
  if (!CSS_USERNAME || !CSS_PASSWORD) {
    throw new Error(
      'Portal auth required: set PORTAL_API_KEY or CSS_USERNAME + CSS_PASSWORD'
    );
  }
  const res = await fetch(`${CSS_AUTH_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: CSS_USERNAME,
      password: CSS_PASSWORD,
      clientId: CSS_CLIENT_ID,
    }),
  });
  const text = await res.text();
  if (!res.ok) {
    throw new Error(`CSS login failed (${res.status}): ${text}`);
  }
  const data = JSON.parse(text);
  const token = data.accessToken || data.access_token || data.token;
  if (!token) {
    throw new Error('CSS login succeeded but no accessToken in response');
  }
  cachedToken = token;
  // Refresh a bit early; CSS tokens typically ~1h — default 50m if unknown.
  const ttlMs = Number(process.env.CSS_TOKEN_TTL_MS || 50 * 60 * 1000);
  tokenExpiresAt = Date.now() + ttlMs;
  return token;
}

async function authHeaders() {
  const headers = { 'Content-Type': 'application/json' };
  if (PORTAL_API_KEY) {
    headers['X-API-Key'] = PORTAL_API_KEY;
    return headers;
  }
  if (!cachedToken || Date.now() >= tokenExpiresAt) {
    await loginCss();
  }
  headers.Authorization = `Bearer ${cachedToken}`;
  return headers;
}

async function portalFetch(path, options = {}, retry = true) {
  const url = `${PORTAL_URL}${path}`;
  const headers = {
    ...(await authHeaders()),
    ...options.headers,
  };
  const response = await fetch(url, { ...options, headers });
  if (response.status === 401 && retry && !PORTAL_API_KEY) {
    cachedToken = null;
    tokenExpiresAt = 0;
    return portalFetch(path, options, false);
  }
  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Portal API error (${response.status}): ${errorText}`);
  }
  if (response.status === 204) {
    return null;
  }
  const ct = response.headers.get('content-type') || '';
  if (!ct.includes('application/json')) {
    return { raw: await response.text() };
  }
  return response.json();
}

async function waitForIdle(sessionId, options = {}) {
  const intervalMs = options.intervalMs ?? DEFAULT_WAIT_INTERVAL_MS;
  const maxAttempts = options.maxAttempts ?? DEFAULT_WAIT_MAX_ATTEMPTS;
  const timeoutMs =
    options.timeoutMs !== undefined && options.timeoutMs !== null
      ? Number(options.timeoutMs)
      : DEFAULT_WAIT_TIMEOUT_MS;

  const started = Date.now();
  let status = 'STREAMING';
  let attempts = 0;

  console.log(
    `[MCP] waitForIdle start session=${sessionId} timeoutMs=${timeoutMs ?? 'none'} maxAttempts=${maxAttempts} intervalMs=${intervalMs}`
  );

  while (true) {
    const elapsed = Date.now() - started;
    const timedOut =
      timeoutMs != null && Number.isFinite(timeoutMs)
        ? elapsed >= timeoutMs
        : attempts >= maxAttempts;

    if (timedOut) {
      const waitedMs = Date.now() - started;
      console.log(
        `[MCP] waitForIdle timeout session=${sessionId} status=${status} waitedMs=${waitedMs} attempts=${attempts}`
      );
      throw new Error(
        `Timed out waiting for session ${sessionId} to leave STREAMING. ` +
          `Final status: ${status}. Waited ${waitedMs}ms` +
          (timeoutMs != null ? ` (timeoutMs=${timeoutMs})` : ` (maxAttempts=${maxAttempts})`) +
          `. Use get_session_transcript later or set waitForReply=false.`
      );
    }

    await new Promise((r) => setTimeout(r, intervalMs));
    attempts += 1;
    const session = await portalFetch(`/api/sessions/${sessionId}`);
    status = session.status;
    if (status !== 'STREAMING') {
      console.log(
        `[MCP] waitForIdle done session=${sessionId} status=${status} waitedMs=${Date.now() - started} attempts=${attempts}`
      );
      return status;
    }
  }
}

function latestAssistantText(messages) {
  const list = Array.isArray(messages) ? messages : [];
  const assistant = list.filter((m) => m.role === 'assistant' || m.role === 'ASSISTANT');
  if (!assistant.length) {
    return 'No assistant reply yet.';
  }
  const last = assistant[assistant.length - 1];
  return last.content || last.text || JSON.stringify(last);
}

async function handleTool(name, args = {}) {
  switch (name) {
    case 'portal_health': {
      const health = await portalFetch('/api/health');
      return textResult(JSON.stringify(health, null, 2));
    }
    case 'list_sessions': {
      const sessions = await portalFetch('/api/sessions');
      return textResult(JSON.stringify(sessions, null, 2));
    }
    case 'create_session': {
      const session = await portalFetch('/api/sessions', {
        method: 'POST',
        body: JSON.stringify({
          title: args.title,
          workspacePath: args.workspacePath || DEFAULT_WORKSPACE,
          provider: args.provider || DEFAULT_PROVIDER,
        }),
      });
      return textResult(`Session created:\n${JSON.stringify(session, null, 2)}`);
    }
    case 'send_prompt': {
      const { sessionId, prompt, timeoutMs } = args;
      await portalFetch(`/api/sessions/${sessionId}/prompt`, {
        method: 'POST',
        body: JSON.stringify({ prompt }),
      });
      const status = await waitForIdle(sessionId, {
        ...(timeoutMs != null ? { timeoutMs } : {}),
      });
      const messages = await portalFetch(`/api/sessions/${sessionId}/messages`);
      return textResult(
        `Run finished with status: ${status}\n\nResponse:\n${latestAssistantText(messages)}`
      );
    }
    case 'get_session_transcript': {
      const messages = await portalFetch(`/api/sessions/${args.sessionId}/messages`);
      return textResult(JSON.stringify(messages, null, 2));
    }
    case 'cancel_run': {
      const result = await portalFetch(`/api/sessions/${args.sessionId}/cancel`, {
        method: 'POST',
        body: '{}',
      });
      return textResult(JSON.stringify(result ?? { ok: true }, null, 2));
    }
    case 'machine_context': {
      const ctx = await portalFetch('/api/machine/context');
      return textResult(JSON.stringify(ctx, null, 2));
    }
    case 'machine_chat': {
      const wait = args.waitForReply !== false;
      const accepted = await portalFetch('/api/machine/chat', {
        method: 'POST',
        body: JSON.stringify({
          message: args.message,
          mode: args.mode || 'act',
          provider: args.provider || DEFAULT_PROVIDER,
          sessionId: args.sessionId || null,
        }),
      });
      const sessionId = accepted.sessionId || accepted.session_id;
      if (!wait) {
        return textResult(
          `Accepted (waitForReply=false).\nsessionId: ${sessionId || 'unknown'}\n\n${JSON.stringify(accepted, null, 2)}`
        );
      }
      if (!sessionId) {
        return textResult(JSON.stringify(accepted, null, 2));
      }
      const status = await waitForIdle(sessionId, {
        ...(args.timeoutMs != null ? { timeoutMs: args.timeoutMs } : {}),
      });
      const messages = await portalFetch(`/api/sessions/${sessionId}/messages`);
      return textResult(
        `Machine chat status: ${status}\nsessionId: ${sessionId}\n\nResponse:\n${latestAssistantText(messages)}`
      );
    }
    default:
      throw new Error(`Unknown tool: ${name}`);
  }
}

function textResult(text) {
  return { content: [{ type: 'text', text }] };
}

function createMcpServer() {
  const server = new Server(
    { name: 'agent-portal-mcp-bridge', version: '1.1.0' },
    { capabilities: { tools: {} } }
  );

  server.setRequestHandler(ListToolsRequestSchema, async () => ({ tools }));
  server.setRequestHandler(CallToolRequestSchema, async (request) => {
    const { name, arguments: args } = request.params;
    try {
      return await handleTool(name, args || {});
    } catch (error) {
      return {
        isError: true,
        content: [{ type: 'text', text: `Error calling tool: ${error.message}` }],
      };
    }
  });

  return server;
}

const MCP_REQUIRE_OAUTH = String(process.env.MCP_REQUIRE_OAUTH || 'false').toLowerCase() === 'true';

function hasValidInboundAuth(req) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : '';
  const alt = req.headers['x-mcp-token'] || req.query.token || '';
  return (
    oauth.isValidAccessToken(token) ||
    oauth.isValidAccessToken(alt) ||
    (MCP_BEARER_TOKEN && (token === MCP_BEARER_TOKEN || alt === MCP_BEARER_TOKEN))
  );
}

function requireInboundAuth(req, res, next) {
  if (!MCP_REQUIRE_OAUTH) {
    return next();
  }
  if (hasValidInboundAuth(req)) {
    return next();
  }

  const resourceUrl = `${HOST_ORIGIN}${
    req.path.startsWith('/sse')
      ? `${MCP_BASE_PATH}/sse`
      : req.path === '/' || req.path === ''
        ? MCP_BASE_PATH
        : `${MCP_BASE_PATH}${req.path}`
  }`;
  const metadataUrl = oauth.resourceMetadataUrlFor(resourceUrl);
  res.setHeader(
    'WWW-Authenticate',
    `Bearer realm="mcp", resource_metadata="${metadataUrl}", scope="mcp"`
  );
  return res.status(401).json({
    error: 'invalid_token',
    error_description:
      'OAuth required. Paste the MCP URL only in Gemini Spark, then complete Allow.',
  });
}

async function handleStreamableHttp(req, res) {
  const server = createMcpServer();
  const transport = new StreamableHTTPServerTransport({
    sessionIdGenerator: undefined,
  });
  res.on('close', () => {
    transport.close().catch(() => {});
    server.close().catch(() => {});
  });
  await server.connect(transport);
  await transport.handleRequest(req, res, req.body);
}

const app = express();
app.use(express.json({ limit: '4mb' }));

app.use((req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader(
    'Access-Control-Allow-Headers',
    'Content-Type, Authorization, X-MCP-Token, Mcp-Session-Id'
  );
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, DELETE, OPTIONS');
  if (req.method === 'OPTIONS') {
    return res.status(204).end();
  }
  next();
});

oauth.mount(app);

app.get('/health', (_req, res) => {
  res.json({
    status: 'ok',
    name: 'agent-portal-mcp-bridge',
    portalUrl: PORTAL_URL,
    sparkUrl: `${PUBLIC_BASE}/`,
    sparkHint:
      'Paste https://agent-portal.delena.buzz/mcp/ in Gemini Spark (Streamable HTTP). Leave Client ID/Secret empty.',
    requireOauth: MCP_REQUIRE_OAUTH,
    authMode: PORTAL_API_KEY ? 'portal-api-key' : CSS_USERNAME ? 'css-jwt' : 'unconfigured',
    oauth: {
      discovery: `${HOST_ORIGIN}/.well-known/oauth-protected-resource${MCP_BASE_PATH}`,
      authorizationServer: `${HOST_ORIGIN}/.well-known/oauth-authorization-server${MCP_BASE_PATH}`,
      authorize: `${PUBLIC_BASE}/authorize`,
      token: `${PUBLIC_BASE}/token`,
      register: `${PUBLIC_BASE}/register`,
    },
  });
});

const sseTransports = new Map();

app.get('/sse', requireInboundAuth, async (req, res) => {
  const clientSessionId = randomUUID();
  console.log(`[MCP] SSE connect ${clientSessionId}`);
  const server = createMcpServer();
  const transport = new SSEServerTransport(
    `${MCP_BASE_PATH}/messages/${clientSessionId}`,
    res
  );
  sseTransports.set(clientSessionId, { transport, server });
  req.on('close', () => {
    console.log(`[MCP] SSE close ${clientSessionId}`);
    sseTransports.delete(clientSessionId);
  });
  await server.connect(transport);
});

app.post('/messages/:clientSessionId', requireInboundAuth, async (req, res) => {
  const entry = sseTransports.get(req.params.clientSessionId);
  if (!entry) {
    return res.status(400).send('Unknown or closed SSE session');
  }
  await entry.transport.handlePostMessage(req, res, req.body);
});

/**
 * Streamable HTTP — Gemini Spark default transport.
 * Public URL: https://agent-portal.delena.buzz/mcp/  (nginx strips /mcp → POST /)
 */
app.post('/', requireInboundAuth, handleStreamableHttp);
app.get('/', requireInboundAuth, async (req, res) => {
  // Validators often GET the URL first; advertise MCP + allow SDK GET stream handling.
  const accept = req.headers.accept || '';
  if (accept.includes('text/event-stream') || req.headers['mcp-session-id']) {
    return handleStreamableHttp(req, res);
  }
  res.status(200).json({
    name: 'agent-portal-mcp-bridge',
    transport: 'streamable-http',
    mcp: true,
    protocol: 'MCP',
    hint: 'POST JSON-RPC initialize to this URL (Gemini Spark Streamable HTTP)',
  });
});

/** Legacy aliases */
app.post('/mcp', requireInboundAuth, handleStreamableHttp);
app.get('/mcp', requireInboundAuth, async (req, res) => {
  const accept = req.headers.accept || '';
  if (accept.includes('text/event-stream') || req.headers['mcp-session-id']) {
    return handleStreamableHttp(req, res);
  }
  res.status(200).json({
    name: 'agent-portal-mcp-bridge',
    transport: 'streamable-http',
    mcp: true,
  });
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`[MCP] listening on 0.0.0.0:${PORT}`);
  console.log(`[MCP] portal=${PORTAL_URL}`);
  console.log(`[MCP] Spark URL=${PUBLIC_BASE}/`);
  console.log(`[MCP] requireOauth=${MCP_REQUIRE_OAUTH}`);
  if (!PORTAL_API_KEY && !CSS_USERNAME) {
    console.warn('[MCP] WARNING: no PORTAL_API_KEY or CSS_USERNAME — tool calls will fail auth');
  }
});
