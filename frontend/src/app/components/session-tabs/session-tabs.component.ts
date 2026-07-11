import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

export type SessionTabId =
  | 'code'
  | 'transcript'
  | 'logs'
  | 'console'
  | 'preview'
  | 'activity'
  | 'changes'
  | 'history'
  | 'guidance';

@Component({
  selector: 'app-session-tabs',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './session-tabs.component.html',
  styleUrl: './session-tabs.component.scss',
})
export class SessionTabsComponent {
  @Input() active: SessionTabId = 'transcript';
  @Output() activeChange = new EventEmitter<SessionTabId>();

  readonly tabs: { id: SessionTabId; label: string }[] = [
    { id: 'transcript', label: 'Transcript' },
    { id: 'logs', label: 'Logs' },
    { id: 'console', label: 'Console' },
    { id: 'code', label: 'Code' },
    { id: 'preview', label: 'Preview' },
    { id: 'changes', label: 'Changes' },
    { id: 'history', label: 'History' },
    { id: 'guidance', label: 'Guidance' },
    { id: 'activity', label: 'Activity' },
  ];

  select(id: SessionTabId): void {
    this.active = id;
    this.activeChange.emit(id);
  }
}
