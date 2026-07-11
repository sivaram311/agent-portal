import { Component, EventEmitter, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import {
  PlatformApp,
  PlatformMemory,
  PlatformOrg,
  PlatformPipeline,
  PlatformRole,
  PlatformTask,
  PlatformAgentMessage,
  E2eLoopProgress,
} from '../../models/session.models';

@Component({
  selector: 'app-app-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './app-home.component.html',
  styleUrl: './app-home.component.scss',
})
export class AppHomeComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);

  @Output() closed = new EventEmitter<void>();
  @Output() openSessions = new EventEmitter<void>();

  apps: PlatformApp[] = [];
  roles: PlatformRole[] = [];
  tasks: PlatformTask[] = [];
  memory: PlatformMemory[] = [];
  messages: PlatformAgentMessage[] = [];
  pipelines: PlatformPipeline[] = [];
  org: PlatformOrg | null = null;
  e2eProgress: E2eLoopProgress | null = null;
  loading = false;
  swarmBusy = false;
  invokeBusyId: string | null = null;
  tab: 'org' | 'apps' | 'roles' | 'tasks' | 'memory' | 'messages' | 'pipelines' = 'org';

  ngOnInit(): void {
    this.reload();
  }

  statusEntries(): { key: string; value: number }[] {
    if (!this.org?.tasksByStatus) {
      return [];
    }
    return Object.entries(this.org.tasksByStatus).map(([key, value]) => ({ key, value }));
  }

  roleEntries(): { key: string; value: number }[] {
    if (!this.org?.tasksByRole) {
      return [];
    }
    return Object.entries(this.org.tasksByRole)
      .filter(([, value]) => value > 0)
      .map(([key, value]) => ({ key, value }));
  }

  reload(): void {
    this.loading = true;
    this.api.platformHome().subscribe({
      next: (home) => {
        this.apps = home.apps ?? [];
        this.pipelines = home.pipelines ?? [];
        this.org = home.org ?? null;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Could not load App Home');
      },
    });
    this.api.platformRoles().subscribe({
      next: (roles) => (this.roles = roles),
      error: () => undefined,
    });
    this.api.platformTasks().subscribe({
      next: (tasks) => {
        this.tasks = tasks;
        this.refreshE2eProgress(tasks);
      },
      error: () => undefined,
    });
    this.api.platformMemory().subscribe({
      next: (memory) => (this.memory = memory),
      error: () => undefined,
    });
    this.api.platformMessages().subscribe({
      next: (messages) => (this.messages = messages),
      error: () => undefined,
    });
    this.api.platformPipelines().subscribe({
      next: (pipelines) => {
        if (pipelines?.length) {
          this.pipelines = pipelines;
        }
      },
      error: () => undefined,
    });
    this.api.platformOrg().subscribe({
      next: (org) => (this.org = org),
      error: () => undefined,
    });
  }

  private refreshE2eProgress(tasks: PlatformTask[]): void {
    const run = tasks.find(
      (t) => t.pipelineId === 'SYSTEM_E2E_LOOP' && !t.parentTaskId && t.stepKey === 'RUN'
    );
    if (!run) {
      this.e2eProgress = null;
      return;
    }
    this.api.e2eLoopProgress(run.id).subscribe({
      next: (progress) => (this.e2eProgress = progress),
      error: () => (this.e2eProgress = null),
    });
  }

  openApp(app: PlatformApp): void {
    if (!app.baseUrl) {
      return;
    }
    window.open(app.baseUrl, '_blank', 'noopener');
  }

  runPipeline(pipeline: PlatformPipeline): void {
    const slug = `run-${Date.now().toString(36)}`;
    const body: { title: string; projectSlug: string; description?: string; maxIterations?: number } = {
      title: `${pipeline.name} run`,
      projectSlug: slug,
      description: pipeline.description,
    };
    if (pipeline.looping) {
      body.maxIterations = pipeline.maxIterations ?? 20;
    }
    this.api.runPlatformPipeline(pipeline.id, body).subscribe({
      next: (tasks) => {
        this.toast.success(`Started ${pipeline.id} (${tasks.length} tasks)`);
        this.tab = pipeline.looping ? 'pipelines' : 'tasks';
        this.reload();
      },
      error: () => this.toast.error('Could not start pipeline'),
    });
  }

  tickSwarm(): void {
    this.swarmBusy = true;
    this.api.platformSwarmTick().subscribe({
      next: (result) => {
        this.swarmBusy = false;
        this.toast.success(
          `Swarm: advanced ${result.advanced}, messages ${result.messagesSent}, parents done ${result.parentsCompleted}`
        );
        this.reload();
      },
      error: () => {
        this.swarmBusy = false;
        this.toast.error('Swarm tick failed');
      },
    });
  }

  invokeTask(task: PlatformTask): void {
    if (task.sessionId) {
      this.toast.success('Session already linked — use the main session list');
      this.openSessions.emit();
      return;
    }
    this.invokeBusyId = task.id;
    this.api.invokePlatformTask(task.id).subscribe({
      next: (session) => {
        this.invokeBusyId = null;
        this.toast.success(`Opened ${task.role} session ${session.id}`);
        this.openSessions.emit();
        this.reload();
      },
      error: (err) => {
        this.invokeBusyId = null;
        const msg = err?.error?.message || err?.message || 'Could not open agent session';
        this.toast.error(msg);
      },
    });
  }
}
