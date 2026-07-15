import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { apiBaseUrl } from './backend-url';

export type CssAuthMode = 'password' | 'hybrid' | 'oauth';

export interface AuthConfig {
  cssEnabled: boolean;
  authUrl: string;
  /** css-next issuer — OAuth authorize/token must use this (not same-origin /auth). */
  issuer?: string;
  clientId: string;
  loginPath: string;
  refreshPath: string;
  apiKeyFallbackEnabled: boolean;
  authMode?: CssAuthMode;
  /** Path only, e.g. /oauth/callback — not under nginx /auth/. */
  oauthRedirectPath?: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken?: string;
  username: string;
  roles?: string[];
}

const ACCESS_KEY = 'agentPortalAccessToken';
const REFRESH_KEY = 'agentPortalRefreshToken';
const USER_KEY = 'agentPortalUser';

const OAUTH_STATE_KEY = 'agentPortalOauthState';
const OAUTH_VERIFIER_KEY = 'agentPortalOauthVerifier';
const OAUTH_REDIRECT_KEY = 'agentPortalOauthRedirect';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  readonly config = signal<AuthConfig | null>(null);
  readonly username = signal<string | null>(null);
  readonly ready = signal(false);
  readonly requiresLogin = signal(false);
  /** Set when OAuth callback fails so the login overlay can show it. */
  readonly oauthError = signal<string | null>(null);

  ssoEnabled(cfg?: AuthConfig | null): boolean {
    const mode = (cfg ?? this.config())?.authMode ?? 'hybrid';
    return mode === 'hybrid' || mode === 'oauth';
  }

  passwordEnabled(cfg?: AuthConfig | null): boolean {
    const mode = (cfg ?? this.config())?.authMode ?? 'hybrid';
    return mode === 'hybrid' || mode === 'password';
  }

  async bootstrap(): Promise<void> {
    try {
      const cfg = await firstValueFrom(
        this.http.get<AuthConfig>(`${apiBaseUrl()}/auth/config`)
      );
      this.config.set(cfg);
      if (!cfg.cssEnabled) {
        this.requiresLogin.set(false);
        this.ready.set(true);
        return;
      }

      if (await this.tryCompleteOAuthCallback(cfg)) {
        this.requiresLogin.set(false);
        this.ready.set(true);
        return;
      }

      const token = this.getAccessToken();
      if (token && !this.isExpired(token)) {
        this.restoreUser();
        this.requiresLogin.set(false);
        this.ready.set(true);
        return;
      }
      if (await this.tryRefresh()) {
        this.requiresLogin.set(false);
        this.ready.set(true);
        return;
      }
      this.clearTokens();
      this.requiresLogin.set(true);
      this.ready.set(true);
    } catch {
      this.requiresLogin.set(false);
      this.ready.set(true);
    }
  }

  async login(username: string, password: string): Promise<void> {
    const cfg = this.config();
    if (!cfg) {
      throw new Error('Auth config not loaded');
    }
    const data = await firstValueFrom(
      this.http.post<LoginResponse>(`${this.authBase(cfg)}${cfg.loginPath}`, {
        username,
        password,
        clientId: cfg.clientId,
      })
    );
    this.storeTokens(data);
    this.oauthError.set(null);
    this.requiresLogin.set(false);
  }

  /**
   * Full-page navigate to css-next /oauth/authorize (PKCE S256).
   * Do not proxy authorize through /auth — needs real 302 + CSS_SSO cookie on issuer.
   */
  async beginOAuthLogin(): Promise<void> {
    const cfg = this.config();
    if (!cfg) {
      throw new Error('Auth config not loaded');
    }
    const issuer = this.oauthIssuer(cfg);
    if (!issuer) {
      throw new Error('CSS issuer missing for OAuth');
    }
    const redirectUri = this.resolveOAuthRedirectUri(cfg);
    const state = this.randomUrlSafe(32);
    const verifier = this.randomUrlSafe(64);
    const challenge = await this.pkceChallengeS256(verifier);
    sessionStorage.setItem(OAUTH_STATE_KEY, state);
    sessionStorage.setItem(OAUTH_VERIFIER_KEY, verifier);
    sessionStorage.setItem(OAUTH_REDIRECT_KEY, redirectUri);

    const url = new URL(`${issuer}/oauth/authorize`);
    url.searchParams.set('response_type', 'code');
    url.searchParams.set('client_id', cfg.clientId || 'agent-portal');
    url.searchParams.set('redirect_uri', redirectUri);
    url.searchParams.set('code_challenge', challenge);
    url.searchParams.set('code_challenge_method', 'S256');
    url.searchParams.set('state', state);
    window.location.assign(url.toString());
  }

  async logout(): Promise<void> {
    const cfg = this.config();
    const token = this.getAccessToken();
    if (cfg?.cssEnabled && token) {
      try {
        await firstValueFrom(
          this.http.post(
            `${this.authBase(cfg)}/auth/logout`,
            { clientId: cfg.clientId },
            { headers: { Authorization: `Bearer ${token}` } }
          )
        );
      } catch {
        // ignore logout errors
      }
    }
    this.clearTokens();
    if (cfg?.cssEnabled) {
      this.requiresLogin.set(true);
    }
  }

  getAccessToken(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  }

  async tryRefresh(): Promise<boolean> {
    const cfg = this.config();
    const refresh = localStorage.getItem(REFRESH_KEY);
    if (!cfg?.cssEnabled || !refresh) {
      return false;
    }
    try {
      const data = await firstValueFrom(
        this.http.post<LoginResponse>(`${this.authBase(cfg)}${cfg.refreshPath}`, {
          refreshToken: refresh,
          clientId: cfg.clientId,
        })
      );
      this.storeTokens(data);
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Prefer same-origin when CSS is reverse-proxied on this host so HTTPS pages
   * never call http://…/auth (mixed content blocked by the browser).
   * Keep absolute CSS URL when the port differs (ng serve :4200 vs CSS :9000).
   */
  private authBase(cfg: AuthConfig): string {
    const configured = (cfg.authUrl || '').trim();
    if (!configured || configured.startsWith('/')) {
      return configured.replace(/\/$/, '');
    }
    try {
      const parsed = new URL(configured);
      if (parsed.hostname === window.location.hostname) {
        const cfgPort = parsed.port || (parsed.protocol === 'https:' ? '443' : '80');
        const pagePort = window.location.port || (window.location.protocol === 'https:' ? '443' : '80');
        if (cfgPort === pagePort) {
          return window.location.origin;
        }
      }
    } catch {
      // fall through
    }
    if (typeof window !== 'undefined' && window.location.protocol === 'https:' && configured.startsWith('http:')) {
      return 'https:' + configured.slice('http:'.length);
    }
    return configured.replace(/\/$/, '');
  }

  /** Issuer origin for OAuth (css-next). Never use same-origin /auth for authorize. */
  private oauthIssuer(cfg: AuthConfig): string {
    const fromIssuer = (cfg.issuer || '').trim().replace(/\/$/, '');
    if (fromIssuer) {
      return fromIssuer;
    }
    // Fallback: if authUrl points at css-next host, reuse it.
    const auth = (cfg.authUrl || '').trim().replace(/\/$/, '');
    if (auth && /css-next/i.test(auth)) {
      return auth;
    }
    return '';
  }

  private resolveOAuthRedirectUri(cfg: AuthConfig): string {
    const path = (cfg.oauthRedirectPath || '/oauth/callback').trim() || '/oauth/callback';
    const normalized = path.startsWith('/') ? path : `/${path}`;
    return `${window.location.origin.replace(/\/$/, '')}${normalized}`;
  }

  private async tryCompleteOAuthCallback(cfg: AuthConfig): Promise<boolean> {
    if (typeof window === 'undefined') {
      return false;
    }
    const params = new URLSearchParams(window.location.search);
    const code = params.get('code');
    const state = params.get('state');
    const error = params.get('error');
    if (!code && !error) {
      return false;
    }
    // Only treat as OAuth when we started a flow (or path matches callback).
    const expectedPath = (cfg.oauthRedirectPath || '/oauth/callback').trim() || '/oauth/callback';
    const onCallbackPath =
      window.location.pathname.replace(/\/$/, '') === expectedPath.replace(/\/$/, '') ||
      !!sessionStorage.getItem(OAUTH_VERIFIER_KEY);
    if (!onCallbackPath) {
      return false;
    }
    try {
      await this.completeOAuthCallback(cfg, { code, state, error });
      this.stripOAuthQuery();
      return true;
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : 'OAuth callback failed';
      this.oauthError.set(msg);
      this.stripOAuthQuery();
      this.clearOAuthSession();
      return false;
    }
  }

  private async completeOAuthCallback(
    cfg: AuthConfig,
    params: { code?: string | null; state?: string | null; error?: string | null }
  ): Promise<void> {
    if (params.error) {
      throw new Error(`OAuth error: ${params.error}`);
    }
    const code = params.code;
    if (!code) {
      throw new Error('Missing authorization code');
    }
    const expectedState = sessionStorage.getItem(OAUTH_STATE_KEY);
    const verifier = sessionStorage.getItem(OAUTH_VERIFIER_KEY);
    const redirectUri =
      sessionStorage.getItem(OAUTH_REDIRECT_KEY) || this.resolveOAuthRedirectUri(cfg);
    this.clearOAuthSession();
    if (!expectedState || params.state !== expectedState) {
      throw new Error('OAuth state mismatch');
    }
    if (!verifier) {
      throw new Error('Missing PKCE verifier');
    }

    const data = await firstValueFrom(
      this.http.post<LoginResponse>(`${apiBaseUrl()}/auth/oauth/token`, {
        grant_type: 'authorization_code',
        code,
        redirect_uri: redirectUri,
        client_id: cfg.clientId || 'agent-portal',
        code_verifier: verifier,
      })
    );
    if (!data.accessToken) {
      throw new Error('No access token in OAuth token response');
    }
    this.storeTokens(data);
    this.oauthError.set(null);
  }

  private clearOAuthSession(): void {
    sessionStorage.removeItem(OAUTH_STATE_KEY);
    sessionStorage.removeItem(OAUTH_VERIFIER_KEY);
    sessionStorage.removeItem(OAUTH_REDIRECT_KEY);
  }

  private stripOAuthQuery(): void {
    const url = new URL(window.location.href);
    url.searchParams.delete('code');
    url.searchParams.delete('state');
    url.searchParams.delete('error');
    const expectedPath = (this.config()?.oauthRedirectPath || '/oauth/callback').trim();
    if (url.pathname.replace(/\/$/, '') === expectedPath.replace(/\/$/, '')) {
      url.pathname = '/';
    }
    window.history.replaceState({}, document.title, url.pathname + url.search + url.hash);
  }

  private base64Url(bytes: ArrayBuffer | Uint8Array): string {
    const u8 = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes);
    let bin = '';
    u8.forEach((b) => {
      bin += String.fromCharCode(b);
    });
    return btoa(bin).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  }

  private randomUrlSafe(length = 64): string {
    const bytes = new Uint8Array(length);
    crypto.getRandomValues(bytes);
    return this.base64Url(bytes).slice(0, length);
  }

  private async pkceChallengeS256(verifier: string): Promise<string> {
    const data = new TextEncoder().encode(verifier);
    const digest = await crypto.subtle.digest('SHA-256', data);
    return this.base64Url(digest);
  }

  private storeTokens(data: LoginResponse): void {
    localStorage.setItem(ACCESS_KEY, data.accessToken);
    if (data.refreshToken) {
      localStorage.setItem(REFRESH_KEY, data.refreshToken);
    }
    const user = { username: data.username, roles: data.roles || [] };
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this.username.set(data.username);
  }

  private restoreUser(): void {
    try {
      const raw = localStorage.getItem(USER_KEY);
      if (raw) {
        this.username.set(JSON.parse(raw).username ?? null);
      }
    } catch {
      this.username.set(null);
    }
  }

  private clearTokens(): void {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
    this.username.set(null);
  }

  private isExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      if (!payload.exp) {
        return true;
      }
      return Date.now() > payload.exp * 1000 - 15_000;
    } catch {
      return true;
    }
  }
}
