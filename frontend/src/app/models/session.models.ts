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
  portalApiKeyRequired?: boolean;
  cssEnabled?: boolean;
  cssClientId?: string;
}
