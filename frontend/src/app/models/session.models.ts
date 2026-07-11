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
  platformRole?: string;
  platformTaskId?: string;
  allowedTools?: string[];
  allowedActions?: string[];
  rolePromptHint?: string;
  humanApprovalRequired?: boolean;
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

export type GuidanceKind = 'RULE' | 'SKILL';

export interface GuidancePack {
  id: string;
  kind: GuidanceKind;
  slug: string;
  title: string;
  description?: string;
  bodyMarkdown: string;
  globs?: string;
  alwaysApply: boolean;
  enabledByDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SessionGuidanceItem {
  id: string;
  packId?: string | null;
  kind: GuidanceKind;
  title: string;
  bodyMarkdown: string;
  enabled: boolean;
  sortOrder: number;
  sessionOnly: boolean;
}

export interface SessionGuidance {
  items: SessionGuidanceItem[];
  effective: SessionGuidanceItem[];
}

export interface GuidanceTemplate {
  kind: GuidanceKind | string;
  slug: string;
  title: string;
  description: string;
  bodyMarkdown: string;
  alwaysApply: boolean;
}

export interface PlatformApp {
  id: string;
  slug: string;
  name: string;
  clientId: string;
  env: string;
  baseUrl: string;
  healthUrl?: string;
  subdomain?: string;
  upstreamPort?: number;
  enabled: boolean;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PlatformHome {
  title: string;
  auth?: string;
  apps: PlatformApp[];
  pipelines?: PlatformPipeline[];
  org?: PlatformOrg;
  docs?: string;
}

export interface PlatformRole {
  id: string;
  name: string;
  department: string;
  defaultWorkspaceHint: string;
  skillHint: string;
  allowedTools?: string[];
  allowedActions?: string[];
  promptHint?: string;
  humanApprovalRequired?: boolean;
}

export interface PlatformTask {
  id: string;
  title: string;
  description?: string;
  role: string;
  status: string;
  projectSlug?: string;
  workspacePath?: string;
  createdBy?: string;
  assigneeUsername?: string;
  sessionId?: string;
  parentTaskId?: string;
  pipelineId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PlatformMemory {
  id: string;
  projectSlug: string;
  key: string;
  kind: string;
  value: string;
  createdBy?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PlatformAgentMessage {
  id: string;
  projectSlug: string;
  taskId?: string;
  fromRole: string;
  toRole: string;
  subject: string;
  body: string;
  status: string;
  createdBy?: string;
  createdAt: string;
}

export interface PlatformPipeline {
  id: string;
  name: string;
  description: string;
  steps: string[];
}

export interface PlatformProjectSummary {
  projectSlug: string;
  taskCount: number;
  openCount: number;
  doneCount: number;
  blockedCount: number;
  linkedSessions: number;
  pipelineId?: string;
}

export interface PlatformOrg {
  title: string;
  tasksByStatus: Record<string, number>;
  tasksByRole: Record<string, number>;
  unreadMessages: number;
  memoryEntries: number;
  linkedSessions: number;
  activeProjects: number;
  projects: PlatformProjectSummary[];
  blockedTasks: PlatformTask[];
  recentOpenTasks: PlatformTask[];
  roles: PlatformRole[];
}

export interface SwarmTickResult {
  projectSlug?: string;
  advanced: number;
  parentsCompleted: number;
  messagesSent: number;
  actions: { type: string; taskId?: string; role?: string; detail?: string }[];
}

