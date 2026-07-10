import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  ChatMessage,
  HealthInfo,
  PermissionRequest,
  Session,
  ToolRun,
} from '../models/session.models';
import { apiBaseUrl } from './backend-url';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = apiBaseUrl();

  health() {
    return this.http.get<HealthInfo>(`${this.base}/health`);
  }

  listSessions() {
    return this.http.get<Session[]>(`${this.base}/sessions`);
  }

  createSession(title: string, workspacePath: string, provider: string) {
    return this.http.post<Session>(`${this.base}/sessions`, { title, workspacePath, provider });
  }

  getSession(id: string) {
    return this.http.get<Session>(`${this.base}/sessions/${id}`);
  }

  messages(id: string) {
    return this.http.get<ChatMessage[]>(`${this.base}/sessions/${id}/messages`);
  }

  tools(id: string) {
    return this.http.get<ToolRun[]>(`${this.base}/sessions/${id}/tools`);
  }

  permissions(id: string) {
    return this.http.get<PermissionRequest[]>(`${this.base}/sessions/${id}/permissions`);
  }

  prompt(id: string, prompt: string) {
    return this.http.post<ChatMessage>(`${this.base}/sessions/${id}/prompt`, { prompt });
  }

  cancel(id: string) {
    return this.http.post<{ status: string }>(`${this.base}/sessions/${id}/cancel`, {});
  }

  resolvePermission(sessionId: string, permissionId: string, decision: string, reason?: string) {
    return this.http.post<{ status: string }>(
      `${this.base}/sessions/${sessionId}/permissions/${permissionId}`,
      { decision, reason }
    );
  }

  archive(id: string) {
    return this.http.post<Session>(`${this.base}/sessions/${id}/archive`, {});
  }
}
