import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Session } from '../../models/session.models';
import { StatusPillComponent } from '../status-pill/status-pill.component';
import { AgentTypeBadgeComponent } from '../agent-type-badge/agent-type-badge.component';

@Component({
  selector: 'app-session-detail-header',
  standalone: true,
  imports: [CommonModule, StatusPillComponent, AgentTypeBadgeComponent],
  templateUrl: './session-detail-header.component.html',
  styleUrl: './session-detail-header.component.scss',
})
export class SessionDetailHeaderComponent {
  @Input() session?: Session;
  @Input() busy = false;
  @Input() showBack = false;
  @Output() back = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();
  @Output() archive = new EventEmitter<string>();
}
