import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Session } from '../../models/session.models';
import { StatusPillComponent } from '../status-pill/status-pill.component';
import { AgentTypeBadgeComponent } from '../agent-type-badge/agent-type-badge.component';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-session-card',
  standalone: true,
  imports: [CommonModule, StatusPillComponent, AgentTypeBadgeComponent],
  templateUrl: './session-card.component.html',
  styleUrl: './session-card.component.scss',
})
export class SessionCardComponent {
  private readonly toast = inject(ToastService);

  @Input({ required: true }) session!: Session;
  @Input() active = false;
  @Output() select = new EventEmitter<string>();
  @Output() archive = new EventEmitter<string>();

  private longPressTimer?: number;
  private longPressFired = false;

  relativeTime(iso: string): string {
    const then = new Date(iso).getTime();
    if (Number.isNaN(then)) {
      return '';
    }
    const diff = Date.now() - then;
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'just now';
    if (mins < 60) return `${mins}m ago`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    return `${days}d ago`;
  }

  onCardClick(): void {
    if (this.longPressFired) {
      this.longPressFired = false;
      return;
    }
    this.select.emit(this.session.id);
  }

  onPathPointerDown(event: PointerEvent): void {
    event.stopPropagation();
    this.longPressFired = false;
    this.clearLongPress();
    this.longPressTimer = window.setTimeout(() => {
      this.longPressFired = true;
      void this.copyPath();
    }, 480);
  }

  onPathPointerUp(event: PointerEvent): void {
    event.stopPropagation();
    this.clearLongPress();
    if (this.longPressFired) {
      event.preventDefault();
    }
  }

  clearLongPress(): void {
    if (this.longPressTimer != null) {
      window.clearTimeout(this.longPressTimer);
      this.longPressTimer = undefined;
    }
  }

  private async copyPath(): Promise<void> {
    const path = this.session.workspacePath;
    try {
      await navigator.clipboard.writeText(path);
      this.toast.success('Path copied');
    } catch {
      this.toast.error('Could not copy path');
    }
  }
}
