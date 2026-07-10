import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Session } from '../../models/session.models';
import { StatusPillComponent } from '../status-pill/status-pill.component';
import { AgentTypeBadgeComponent } from '../agent-type-badge/agent-type-badge.component';

@Component({
  selector: 'app-session-card',
  standalone: true,
  imports: [CommonModule, StatusPillComponent, AgentTypeBadgeComponent],
  templateUrl: './session-card.component.html',
  styleUrl: './session-card.component.scss',
})
export class SessionCardComponent {
  @Input({ required: true }) session!: Session;
  @Input() active = false;
  @Output() select = new EventEmitter<string>();
  @Output() archive = new EventEmitter<string>();

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
}
