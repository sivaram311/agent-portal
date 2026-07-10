import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Session } from '../../models/session.models';
import { StatusPillComponent } from '../status-pill/status-pill.component';
import { AgentTypeBadgeComponent } from '../agent-type-badge/agent-type-badge.component';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-session-detail-header',
  standalone: true,
  imports: [CommonModule, StatusPillComponent, AgentTypeBadgeComponent],
  templateUrl: './session-detail-header.component.html',
  styleUrl: './session-detail-header.component.scss',
})
export class SessionDetailHeaderComponent {
  private readonly toast = inject(ToastService);

  @Input() session?: Session;
  @Input() busy = false;
  @Input() showBack = false;
  @Output() back = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();
  @Output() archive = new EventEmitter<string>();

  private longPressTimer?: number;

  onPathPointerDown(event: PointerEvent): void {
    this.clearLongPress();
    this.longPressTimer = window.setTimeout(() => {
      void this.copyPath();
    }, 480);
  }

  onPathPointerUp(): void {
    this.clearLongPress();
  }

  clearLongPress(): void {
    if (this.longPressTimer != null) {
      window.clearTimeout(this.longPressTimer);
      this.longPressTimer = undefined;
    }
  }

  copyPathClick(): void {
    this.clearLongPress();
    void this.copyPath();
  }

  private async copyPath(): Promise<void> {
    const path = this.session?.workspacePath;
    if (!path) {
      return;
    }
    try {
      await navigator.clipboard.writeText(path);
      this.toast.success('Path copied');
    } catch {
      this.toast.error('Could not copy path');
    }
  }
}
