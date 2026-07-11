import { Component, EventEmitter, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import { PlatformApp, PlatformRole, PlatformTask } from '../../models/session.models';

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
  loading = false;
  tab: 'apps' | 'roles' | 'tasks' = 'apps';

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading = true;
    this.api.platformHome().subscribe({
      next: (home) => {
        this.apps = home.apps ?? [];
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
  }

  openApp(app: PlatformApp): void {
    if (!app.baseUrl) {
      return;
    }
    window.open(app.baseUrl, '_blank', 'noopener');
  }
}
