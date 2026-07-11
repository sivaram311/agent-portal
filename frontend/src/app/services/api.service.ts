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
  FileChange,
  SessionPreset,
  SessionEventRow,
  GuidancePack,
  GuidanceKind,
  SessionGuidance,
  GuidanceTemplate,
  PlatformHome,
  PlatformApp,
  PlatformRole,
  PlatformTask,
  PlatformMemory,
  PlatformAgentMessage,
  PlatformPipeline,
  PlatformOrg,
  SwarmTickResult,
  E2eLoopProgress,
} from '../models/session.models';
import { apiBaseUrl } from './backend-url';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = apiBaseUrl();

  health() {
    return this.http.get<HealthInfo>(`${this.base}/health`);
  }

  presets() {
    return this.http.get<SessionPreset[]>(`${this.base}/presets`);
  }

  listSessions() {
    return this.http.get<Session[]>(`${this.base}/sessions`);
  }

  createSession(
    title: string,
    workspacePath: string,
    provider: string,
    useGuidanceDefaults = true,
    platformRole?: string,
    platformTaskId?: string
  ) {
    return this.http.post<Session>(`${this.base}/sessions`, {
      title,
      workspacePath,
      provider,
      useGuidanceDefaults,
      platformRole: platformRole || undefined,
      platformTaskId: platformTaskId || undefined,
    });
  }

  updateSessionRole(id: string, body: { platformRole?: string | null; platformTaskId?: string }) {
    return this.http.patch<Session>(`${this.base}/sessions/${id}/platform-role`, body);
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

  unarchive(id: string) {
    return this.http.post<Session>(`${this.base}/sessions/${id}/unarchive`, {});
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

  listChanges(sessionId: string) {
    return this.http.get<FileChange[]>(`${this.base}/sessions/${sessionId}/changes`);
  }

  diffChange(sessionId: string, path: string) {
    return this.http.get<FileChange>(`${this.base}/sessions/${sessionId}/changes/diff`, {
      params: { path },
    });
  }

  acceptChange(sessionId: string, path: string) {
    return this.http.post<{ status: string; path: string }>(
      `${this.base}/sessions/${sessionId}/changes/accept`,
      { path }
    );
  }

  rejectChange(sessionId: string, path: string) {
    return this.http.post<{ status: string; path: string }>(
      `${this.base}/sessions/${sessionId}/changes/reject`,
      { path }
    );
  }

  events(sessionId: string) {
    return this.http.get<SessionEventRow[]>(`${this.base}/sessions/${sessionId}/events`);
  }

  listCollaborators(sessionId: string) {
    return this.http.get<{ username: string; role: string; createdAt: string }[]>(
      `${this.base}/sessions/${sessionId}/collaborators`
    );
  }

  addCollaborator(sessionId: string, username: string) {
    return this.http.post<{ status: string; username: string }>(
      `${this.base}/sessions/${sessionId}/collaborators`,
      { username }
    );
  }

  removeCollaborator(sessionId: string, username: string) {
    return this.http.delete(`${this.base}/sessions/${sessionId}/collaborators/${encodeURIComponent(username)}`);
  }

  listGuidancePacks(kind?: GuidanceKind) {
    const q = kind ? `?kind=${kind}` : '';
    return this.http.get<GuidancePack[]>(`${this.base}/guidance/packs${q}`);
  }

  createGuidancePack(body: {
    kind: GuidanceKind;
    title: string;
    description?: string;
    bodyMarkdown: string;
    globs?: string;
    alwaysApply?: boolean;
    enabledByDefault?: boolean;
    slug?: string;
  }) {
    return this.http.post<GuidancePack>(`${this.base}/guidance/packs`, body);
  }

  updateGuidancePack(id: string, body: Partial<GuidancePack>) {
    return this.http.patch<GuidancePack>(`${this.base}/guidance/packs/${id}`, body);
  }

  deleteGuidancePack(id: string) {
    return this.http.delete(`${this.base}/guidance/packs/${id}`);
  }

  getGuidanceDefaults() {
    return this.http.get<GuidancePack[]>(`${this.base}/guidance/defaults`);
  }

  putGuidanceDefaults(enabledPackIds: string[]) {
    return this.http.put<GuidancePack[]>(`${this.base}/guidance/defaults`, { enabledPackIds });
  }

  guidanceTemplates() {
    return this.http.get<GuidanceTemplate[]>(`${this.base}/guidance/templates`);
  }

  installGuidanceTemplates() {
    return this.http.post<GuidancePack[]>(`${this.base}/guidance/templates/install`, {});
  }

  getSessionGuidance(sessionId: string) {
    return this.http.get<SessionGuidance>(`${this.base}/sessions/${sessionId}/guidance`);
  }

  putSessionGuidance(
    sessionId: string,
    items: {
      packId?: string | null;
      enabled?: boolean;
      sortOrder?: number;
      title?: string;
      sessionBody?: string;
      kind?: GuidanceKind;
    }[]
  ) {
    return this.http.put<SessionGuidance>(`${this.base}/sessions/${sessionId}/guidance`, { items });
  }

  platformHome() {
    return this.http.get<PlatformHome>(`${this.base}/platform/home`);
  }

  platformApps(enabledOnly = true) {
    return this.http.get<PlatformApp[]>(`${this.base}/platform/apps`, {
      params: { enabledOnly: String(enabledOnly) },
    });
  }

  platformRoles() {
    return this.http.get<PlatformRole[]>(`${this.base}/platform/roles`);
  }

  platformTasks(status?: string, role?: string) {
    const params: Record<string, string> = {};
    if (status) params['status'] = status;
    if (role) params['role'] = role;
    return this.http.get<PlatformTask[]>(`${this.base}/platform/tasks`, { params });
  }

  createPlatformTask(body: {
    title: string;
    role: string;
    description?: string;
    projectSlug?: string;
    workspacePath?: string;
  }) {
    return this.http.post<PlatformTask>(`${this.base}/platform/tasks`, body);
  }

  platformMemory(projectSlug?: string, kind?: string) {
    const params: Record<string, string> = {};
    if (projectSlug) params['projectSlug'] = projectSlug;
    if (kind) params['kind'] = kind;
    return this.http.get<PlatformMemory[]>(`${this.base}/platform/memory`, { params });
  }

  platformMessages(projectSlug?: string, toRole?: string) {
    const params: Record<string, string> = {};
    if (projectSlug) params['projectSlug'] = projectSlug;
    if (toRole) params['toRole'] = toRole;
    return this.http.get<PlatformAgentMessage[]>(`${this.base}/platform/messages`, { params });
  }

  platformPipelines() {
    return this.http.get<PlatformPipeline[]>(`${this.base}/platform/pipelines`);
  }

  runPlatformPipeline(
    id: string,
    body: { title: string; projectSlug: string; description?: string; maxIterations?: number }
  ) {
    return this.http.post<PlatformTask[]>(`${this.base}/platform/pipelines/${id}/run`, body);
  }

  e2eLoopProgress(runId: string) {
    return this.http.get<E2eLoopProgress>(`${this.base}/platform/pipelines/runs/${runId}`);
  }

  platformOrg() {
    return this.http.get<PlatformOrg>(`${this.base}/platform/org`);
  }

  platformSwarmTick(projectSlug?: string) {
    return this.http.post<SwarmTickResult>(`${this.base}/platform/swarm/tick`, {
      projectSlug: projectSlug ?? null,
    });
  }

  updatePlatformTask(
    id: string,
    body: Partial<{
      status: string;
      sessionId: string;
      title: string;
      outcome: string;
      stepKey: string;
      description: string;
    }>
  ) {
    return this.http.patch<PlatformTask>(`${this.base}/platform/tasks/${id}`, body);
  }

  invokePlatformTask(id: string) {
    return this.http.post<Session>(`${this.base}/platform/tasks/${id}/invoke`, {});
  }
}
