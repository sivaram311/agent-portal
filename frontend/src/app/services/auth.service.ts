import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { apiBaseUrl } from './backend-url';

export interface AuthConfig {
  cssEnabled: boolean;
  authUrl: string;
  clientId: string;
  loginPath: string;
  refreshPath: string;
  apiKeyFallbackEnabled: boolean;
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

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  readonly config = signal<AuthConfig | null>(null);
  readonly username = signal<string | null>(null);
  readonly ready = signal(false);
  readonly requiresLogin = signal(false);

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
    this.requiresLogin.set(false);
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
