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
  loading = false;
  swarmBusy = false;
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
      next: (tasks) => (this.tasks = tasks),
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

  openApp(app: PlatformApp): void {
    if (!app.baseUrl) {
      return;
    }
    window.open(app.baseUrl, '_blank', 'noopener');
  }

  runPipeline(pipeline: PlatformPipeline): void {
    const slug = `run-${Date.now().toString(36)}`;
    this.api
      .runPlatformPipeline(pipeline.id, {
        title: `${pipeline.name} run`,
        projectSlug: slug,
        description: pipeline.description,
      })
      .subscribe({
        next: (tasks) => {
          this.toast.success(`Started ${pipeline.id} (${tasks.length} tasks)`);
          this.tab = 'tasks';
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
}
