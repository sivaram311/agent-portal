import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

export type SessionTabId = 'code' | 'transcript' | 'logs' | 'preview';

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
    { id: 'code', label: 'Code' },
    { id: 'transcript', label: 'Transcript' },
    { id: 'logs', label: 'Logs' },
    { id: 'preview', label: 'Preview' },
  ];

  select(id: SessionTabId): void {
    this.active = id;
    this.activeChange.emit(id);
  }
}
