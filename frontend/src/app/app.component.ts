import { Component, HostListener, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { SessionListComponent } from './components/session-list/session-list.component';
import { ChatComponent } from './components/chat/chat.component';
import { TaskPanelComponent } from './components/task-panel/task-panel.component';
import { PermissionDialogComponent } from './components/permission-dialog/permission-dialog.component';
import { SessionDetailHeaderComponent } from './components/session-detail-header/session-detail-header.component';
import { SessionTabsComponent, SessionTabId } from './components/session-tabs/session-tabs.component';
import { AgentInputBarComponent } from './components/agent-input-bar/agent-input-bar.component';
import { CodeViewerComponent } from './components/code-viewer/code-viewer.component';
import { ToastComponent } from './components/toast/toast.component';
import { LoginOverlayComponent } from './components/login-overlay/login-overlay.component';
import { CapabilityBadgesComponent } from './components/capability-badges/capability-badges.component';
import { AuditPanelComponent } from './components/audit-panel/audit-panel.component';
import { ApiService } from './services/api.service';
import { AuthService } from './services/auth.service';
import { RealtimeService } from './services/realtime.service';
import { ToastService } from './services/toast.service';
import {
  AgentEvent,
  ChatMessage,
  HealthInfo,
  PermissionRequest,
  Session,
  ToolRun,
} from './models/session.models';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    SessionListComponent,
    ChatComponent,
    TaskPanelComponent,
    PermissionDialogComponent,
    SessionDetailHeaderComponent,
    SessionTabsComponent,
    AgentInputBarComponent,
    CodeViewerComponent,
    ToastComponent,
    LoginOverlayComponent,
    CapabilityBadgesComponent,
    AuditPanelComponent,
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly realtime = inject(RealtimeService);
  private readonly toast = inject(ToastService);

  sessions: Session[] = [];
  active?: Session;
  messages: ChatMessage[] = [];
  tools: ToolRun[] = [];
  terminalLines: string[] = [];
  streamingText = '';
  busy = false;
  health?: HealthInfo;
  pendingPermission?: PermissionRequest | null;
  awaitingInputPrompt = '';
  showCreate = false;
  createTitle = '';
  createWorkspace = 'demo';
  createProvider: 'cursor' | 'antigravity' = 'cursor';
  error = '';
  sessionSearch = '';
  activeTab: SessionTabId = 'transcript';
  drawerOpen = false;
  isMobile = false;

  private eventSub?: Subscription;

  get authReady(): boolean {
    return this.auth.ready();
  }

  get showLogin(): boolean {
    return this.auth.requiresLogin();
  }

  get signedInAs(): string | null {
    return this.auth.username();
  }

  ngOnInit(): void {
    this.updateViewport();
    void this.boot();
  }

  private async boot(): Promise<void> {
    await this.auth.bootstrap();
    if (this.auth.requiresLogin()) {
      return;
    }
    this.afterAuth();
  }

  onSignedIn(): void {
    this.afterAuth();
  }

  async logout(): Promise<void> {
    await this.auth.logout();
    this.sessions = [];
    this.active = undefined;
    this.messages = [];
    this.tools = [];
  }

  private afterAuth(): void {
    this.realtime.reconnect();
    this.api.health().subscribe({
      next: (h) => (this.health = h),
      error: () => this.toast.error('Backend unreachable on :8080'),
    });
    this.refreshSessions();
  }

  ngOnDestroy(): void {
    this.eventSub?.unsubscribe();
  }

  @HostListener('window:resize')
  updateViewport(): void {
    this.isMobile = window.innerWidth < 640;
    if (!this.isMobile) {
      this.drawerOpen = false;
    }
  }

  get showMobileDetail(): boolean {
    return this.isMobile && !!this.active;
  }

  get showMobileList(): boolean {
    return this.isMobile && !this.active;
  }

  refreshSessions(): void {
    this.api.listSessions().subscribe({
      next: (list) => {
        this.sessions = list;
        if (this.active) {
          this.active = list.find((s) => s.id === this.active!.id) ?? this.active;
        }
      },
      error: (err) => this.toast.error(err?.error?.error || 'Failed to load sessions'),
    });
  }

  openCreate(): void {
    this.showCreate = true;
    this.drawerOpen = false;
    this.createTitle = '';
    this.createWorkspace = 'demo';
    this.createProvider = 'cursor';
  }

  createSession(): void {
    const title = this.createTitle.trim() || 'New session';
    const workspace = this.createWorkspace.trim() || 'demo';
    this.api.createSession(title, workspace, this.createProvider).subscribe({
      next: (session) => {
        this.showCreate = false;
        this.refreshSessions();
        this.selectSession(session.id);
        this.toast.success('Session created');
      },
      error: (err) => this.toast.error(err?.error?.error || 'Failed to create session'),
    });
  }

  selectSession(id: string): void {
    this.drawerOpen = false;
    this.activeTab = 'transcript';
    this.eventSub?.unsubscribe();
    this.streamingText = '';
    this.terminalLines = [];
    this.pendingPermission = null;
    this.awaitingInputPrompt = '';
    this.busy = false;

    this.api.getSession(id).subscribe({
      next: (session) => {
        this.active = session;
        this.busy =
          session.status === 'STREAMING' ||
          session.status === 'WAITING_PERMISSION' ||
          session.status === 'WAITING_PLAN';
      },
    });

    this.api.messages(id).subscribe({ next: (m) => (this.messages = m) });
    this.api.tools(id).subscribe({
      next: (t) => {
        this.tools = t;
        this.terminalLines = t
          .filter((x) => !!x.output)
          .flatMap((x) => [`$ ${x.toolName}`, x.output || '', '']);
      },
    });
    this.api.permissions(id).subscribe({
      next: (p) => {
        if (this.active?.provider === 'antigravity') {
          this.pendingPermission = null;
        } else {
          this.pendingPermission = p[0] ?? null;
        }
      },
    });

    this.eventSub = this.realtime.watchSession(id).subscribe((event) => this.onEvent(event));
  }

  clearActive(): void {
    this.active = undefined;
    this.messages = [];
    this.tools = [];
    this.terminalLines = [];
    this.streamingText = '';
    this.busy = false;
    this.pendingPermission = null;
    this.awaitingInputPrompt = '';
    this.eventSub?.unsubscribe();
  }

  sendPrompt(prompt: string): void {
    if (!this.active) {
      return;
    }
    this.error = '';
    this.awaitingInputPrompt = '';
    this.busy = true;
    this.streamingText = '';
    this.activeTab = 'transcript';
    const optimistic: ChatMessage = {
      id: 'tmp-' + Date.now(),
      sessionId: this.active.id,
      role: 'USER',
      content: prompt,
      sequenceNo: (this.messages.at(-1)?.sequenceNo ?? 0) + 1,
      createdAt: new Date().toISOString(),
    };
    this.messages = [...this.messages, optimistic];

    this.api.prompt(this.active.id, prompt).subscribe({
      next: (msg) => {
        this.messages = this.messages.map((m) => (m.id === optimistic.id ? msg : m));
        this.refreshSessions();
      },
      error: (err) => {
        this.busy = false;
        this.toast.error(err?.error?.error || 'Prompt failed');
      },
    });
  }

  cancelRun(): void {
    if (!this.active) {
      return;
    }
    this.api.cancel(this.active.id).subscribe({
      next: () => {
        this.busy = false;
        this.refreshSessions();
        this.toast.success('Run cancelled');
      },
      error: (err) => this.toast.error(err?.error?.error || 'Cancel failed'),
    });
  }

  archiveSession(id: string): void {
    this.api.archive(id).subscribe({
      next: () => {
        if (this.active?.id === id) {
          this.clearActive();
        }
        this.refreshSessions();
        this.toast.success('Session archived');
      },
      error: (err) => this.toast.error(err?.error?.error || 'Archive failed'),
    });
  }

  onPermissionDecide(decision: { decision: string; reason?: string }): void {
    if (!this.active || !this.pendingPermission) {
      return;
    }
    const permissionId = this.pendingPermission.id;
    this.api
      .resolvePermission(this.active.id, permissionId, decision.decision, decision.reason)
      .subscribe({
        next: () => {
          this.pendingPermission = null;
          this.busy = true;
          this.refreshSessions();
        },
        error: (err) => this.toast.error(err?.error?.error || 'Permission resolve failed'),
      });
  }

  abandonSubagent(subId: string): void {
    if (!this.active) {
      return;
    }
    this.api.abandonSubagent(this.active.id, subId).subscribe({
      next: (result) => {
        const idx = this.tools.findIndex(
          (t) => t.subagentId === subId || t.toolCallId === subId || t.id === subId
        );
        if (idx >= 0) {
          const copy = [...this.tools];
          copy[idx] = { ...copy[idx], status: 'abandoned', finishedAt: new Date().toISOString() };
          this.tools = copy;
        }
        if (result.sessionCancelled) {
          this.busy = false;
          this.toast.success(result.message || 'Session run was cancelled');
        } else {
          this.toast.success(result.message || 'Sub-agent abandoned');
        }
        this.refreshSessions();
      },
      error: (err) => this.toast.error(err?.error?.error || 'Abandon failed'),
    });
  }

  private upsertToolFromEvent(event: AgentEvent, defaults?: Partial<ToolRun>): void {
    if (!this.active) {
      return;
    }
    const payload = event.payload;
    const toolCallId = String(payload['toolCallId'] ?? payload['subagentId'] ?? '');
    const idx = this.tools.findIndex(
      (t) =>
        (toolCallId && t.toolCallId === toolCallId) ||
        (payload['subagentId'] && t.subagentId === String(payload['subagentId'])) ||
        (payload['toolRunId'] && t.id === String(payload['toolRunId']))
    );
    const existing = idx >= 0 ? this.tools[idx] : undefined;
    const tool: ToolRun = {
      id: String(payload['toolRunId'] ?? existing?.id ?? Date.now()),
      sessionId: this.active.id,
      toolCallId: toolCallId || existing?.toolCallId,
      toolName: String(payload['toolName'] ?? existing?.toolName ?? defaults?.toolName ?? 'tool'),
      argsJson:
        payload['args'] != null ? String(payload['args']) : existing?.argsJson ?? defaults?.argsJson,
      status: String(payload['status'] ?? existing?.status ?? defaults?.status ?? 'running'),
      kind:
        payload['kind'] != null && String(payload['kind'])
          ? String(payload['kind'])
          : defaults?.kind ?? existing?.kind,
      subagentId:
        payload['subagentId'] != null && String(payload['subagentId'])
          ? String(payload['subagentId'])
          : defaults?.subagentId ?? existing?.subagentId,
      parentToolCallId:
        payload['parentToolCallId'] != null && String(payload['parentToolCallId'])
          ? String(payload['parentToolCallId'])
          : defaults?.parentToolCallId ?? existing?.parentToolCallId,
      output: existing?.output,
      exitCode: existing?.exitCode,
      startedAt: existing?.startedAt ?? event.timestamp,
      finishedAt: existing?.finishedAt,
    };

    if (idx >= 0) {
      const copy = [...this.tools];
      copy[idx] = { ...copy[idx], ...tool };
      this.tools = copy;
    } else {
      this.tools = [...this.tools, tool];
    }
  }

  private onEvent(event: AgentEvent): void {
    if (!this.active || event.sessionId !== this.active.id) {
      return;
    }

    switch (event.type) {
      case 'assistant_delta': {
        const text = String(event.payload['text'] ?? '');
        this.streamingText += text;
        break;
      }
      case 'assistant_message': {
        const content = String(event.payload['content'] ?? this.streamingText);
        if (content) {
          this.messages = [
            ...this.messages,
            {
              id: 'a-' + Date.now(),
              sessionId: this.active.id,
              role: 'ASSISTANT',
              content,
              sequenceNo: (this.messages.at(-1)?.sequenceNo ?? 0) + 1,
              createdAt: event.timestamp,
            },
          ];
        }
        this.streamingText = '';
        break;
      }
      case 'terminal_chunk': {
        const text = String(event.payload['text'] ?? '');
        if (text) {
          this.terminalLines = [...this.terminalLines, text];
        }
        break;
      }
      case 'tool_call': {
        this.upsertToolFromEvent(event);
        break;
      }
      case 'subagent_started':
      case 'subagent_progress':
      case 'subagent_finished': {
        this.upsertToolFromEvent(event, { kind: 'subagent' });
        break;
      }
      case 'input_required': {
        const prompt = String(event.payload['prompt'] ?? event.payload['message'] ?? '');
        if (prompt) {
          this.awaitingInputPrompt = prompt;
          this.busy = true;
        }
        break;
      }
      case 'permission_required':
      case 'plan_required': {
        if (this.active.provider === 'antigravity') {
          break;
        }
        this.pendingPermission = {
          id: String(event.payload['permissionId']),
          sessionId: this.active.id,
          toolCallId: String(event.payload['toolCallId'] ?? ''),
          detailsJson: String(event.payload['details'] ?? event.payload['plan'] ?? ''),
          status: 'PENDING',
          kind: String(event.payload['kind'] ?? (event.type === 'plan_required' ? 'plan' : 'permission')),
          planMarkdown: event.payload['plan'] ? String(event.payload['plan']) : undefined,
          createdAt: event.timestamp,
        };
        this.busy = true;
        break;
      }
      case 'permission_resolved':
        this.pendingPermission = null;
        break;
      case 'status': {
        const status = String(event.payload['status'] ?? this.active.status) as Session['status'];
        this.active = { ...this.active, status };
        this.busy =
          status === 'STREAMING' || status === 'WAITING_PERMISSION' || status === 'WAITING_PLAN';
        this.refreshSessions();
        break;
      }
      case 'run_completed':
      case 'run_cancelled':
      case 'run_failed':
        this.busy = false;
        if (this.streamingText) {
          this.onEvent({
            ...event,
            type: 'assistant_message',
            payload: { content: this.streamingText },
          });
        }
        this.api.messages(this.active.id).subscribe({ next: (m) => (this.messages = m) });
        this.api.tools(this.active.id).subscribe({ next: (t) => (this.tools = t) });
        this.refreshSessions();
        break;
      default:
        break;
    }
  }
}
