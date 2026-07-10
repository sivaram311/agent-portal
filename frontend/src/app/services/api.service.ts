import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  ChatMessage,
  FileContent,
  FileEntry,
  HealthInfo,
  PermissionRequest,
  Session,
  ToolRun,
  AuditEvent,
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

  abandonSubagent(sessionId: string, subId: string) {
    return this.http.post<{
      status: string;
      subagentId: string;
      toolRunId: string;
      sessionCancelled: boolean;
      message: string;
    }>(`${this.base}/sessions/${sessionId}/subagents/${encodeURIComponent(subId)}/abandon`, {});
  }

  listFiles(sessionId: string, path?: string) {
    const url = `${this.base}/sessions/${sessionId}/files`;
    if (path) {
      return this.http.get<FileEntry[]>(url, { params: { path } });
    }
    return this.http.get<FileEntry[]>(url);
  }

  readFile(sessionId: string, path: string) {
    return this.http.get<FileContent>(`${this.base}/sessions/${sessionId}/files/content`, {
      params: { path },
    });
  }

  audit(sessionId?: string, limit = 50) {
    const params: Record<string, string> = { limit: String(limit) };
    if (sessionId) {
      params['sessionId'] = sessionId;
    }
    return this.http.get<AuditEvent[]>(`${this.base}/audit`, { params });
  }
}
