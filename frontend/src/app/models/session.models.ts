export type SessionStatus =
  | 'IDLE'
  | 'STREAMING'
  | 'WAITING_PERMISSION'
  | 'WAITING_PLAN'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED'
  | 'ARCHIVED';

export type AgentProviderId = 'cursor' | 'antigravity';

export interface Session {
  id: string;
  title: string;
  workspacePath: string;
  cursorSessionId?: string;
  status: SessionStatus;
  provider: AgentProviderId | string;
  ownerUsername?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ChatMessage {
  id: string;
  sessionId: string;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  sequenceNo: number;
  createdAt: string;
}

export interface ToolRun {
  id: string;
  sessionId: string;
  toolCallId?: string;
  toolName: string;
  argsJson?: string;
  status: string;
  output?: string;
  exitCode?: number;
  startedAt: string;
  finishedAt?: string;
  kind?: string;
  parentToolCallId?: string;
  subagentId?: string;
}

export interface FileEntry {
  name: string;
  path: string;
  directory: boolean;
  size: number;
}

export interface FileContent {
  path: string;
  content: string;
  mediaType: string;
  truncated: boolean;
}

export interface PermissionRequest {
  id: string;
  sessionId: string;
  toolCallId?: string;
  detailsJson?: string;
  status: string;
  kind: string;
  planMarkdown?: string;
  createdAt: string;
}

export interface AgentEvent {
  sessionId: string;
  type: string;
  payload: Record<string, unknown>;
  timestamp: string;
}

export interface HealthInfo {
  status: string;
  workspaceRoot: string;
  cursorCommand: string;
  cursorCommandExists: boolean;
  apiKeyConfigured: boolean;
  antigravityCommand?: string;
  antigravityCommandExists?: boolean;
  antigravityBrainRoot?: string;
  antigravityBrainReadable?: boolean;
  antigravitySkipPermissions?: boolean;
  antigravityInteractiveMode?: boolean;
  antigravityInteractiveProtocol?: string;
  antigravityCapabilities?: Record<string, unknown>;
  portalApiKeyRequired?: boolean;
  cssEnabled?: boolean;
  cssClientId?: string;
  capabilities?: Record<string, boolean>;
}

export interface AuditEvent {
  id: string;
  username: string;
  action: string;
  sessionId: string;
  details: string;
  createdAt: string;
}

export interface FileChange {
  path: string;
  status: string;
  size: number;
  unifiedDiff?: string | null;
  source?: string | null;
}

export interface SessionPreset {
  id: string;
  title: string;
  provider: string;
  workspacePath: string;
  starterPrompt: string;
}

export interface HistoryItem {
  kind: 'message' | 'tool' | 'event';
  at: string;
  title: string;
  detail: string;
}

export interface SessionEventRow {
  id: string;
  type: string;
  createdAt: string;
  payload: Record<string, unknown>;
}
