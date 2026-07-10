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
import { ApiService } from './services/api.service';
import { RealtimeService } from './services/realtime.service';
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
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly realtime = inject(RealtimeService);

  sessions: Session[] = [];
  active?: Session;
  messages: ChatMessage[] = [];
  tools: ToolRun[] = [];
  terminalLines: string[] = [];
  streamingText = '';
  busy = false;
  health?: HealthInfo;
  pendingPermission?: PermissionRequest | null;
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

  ngOnInit(): void {
    this.updateViewport();
    this.realtime.connect();
    this.api.health().subscribe({
      next: (h) => (this.health = h),
      error: () => (this.error = 'Backend unreachable on :8080'),
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
      error: (err) => (this.error = err?.error?.error || 'Failed to load sessions'),
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
      },
      error: (err) => (this.error = err?.error?.error || 'Failed to create session'),
    });
  }

  selectSession(id: string): void {
    this.drawerOpen = false;
    this.activeTab = 'transcript';
    this.eventSub?.unsubscribe();
    this.streamingText = '';
    this.terminalLines = [];
    this.pendingPermission = null;
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
    this.eventSub?.unsubscribe();
  }

  sendPrompt(prompt: string): void {
    if (!this.active) {
      return;
    }
    this.error = '';
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
        this.error = err?.error?.error || 'Prompt failed';
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
      },
    });
  }

  archiveSession(id: string): void {
    this.api.archive(id).subscribe({
      next: () => {
        if (this.active?.id === id) {
          this.clearActive();
        }
        this.refreshSessions();
      },
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
        error: (err) => (this.error = err?.error?.error || 'Permission resolve failed'),
      });
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
        const tool: ToolRun = {
          id: String(event.payload['toolRunId'] ?? Date.now()),
          sessionId: this.active.id,
          toolCallId: String(event.payload['toolCallId'] ?? ''),
          toolName: String(event.payload['toolName'] ?? 'tool'),
          argsJson: String(event.payload['args'] ?? ''),
          status: String(event.payload['status'] ?? 'running'),
          startedAt: event.timestamp,
        };
        const idx = this.tools.findIndex((t) => t.toolCallId && t.toolCallId === tool.toolCallId);
        if (idx >= 0) {
          const copy = [...this.tools];
          copy[idx] = { ...copy[idx], ...tool };
          this.tools = copy;
        } else {
          this.tools = [...this.tools, tool];
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
