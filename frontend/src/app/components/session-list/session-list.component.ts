import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Session } from '../../models/session.models';
import { SessionCardComponent } from '../session-card/session-card.component';

export type SessionFilter = 'all' | 'active' | 'failed' | 'archived';

@Component({
  selector: 'app-session-list',
  standalone: true,
  imports: [CommonModule, FormsModule, SessionCardComponent],
  templateUrl: './session-list.component.html',
  styleUrl: './session-list.component.scss',
})
export class SessionListComponent {
  @Input() sessions: Session[] = [];
  @Input() activeId?: string;
  @Input() search = '';
  @Input() compact = false;
  @Output() searchChange = new EventEmitter<string>();
  @Output() select = new EventEmitter<string>();
  @Output() create = new EventEmitter<void>();
  @Output() archive = new EventEmitter<string>();

  filter: SessionFilter = 'all';

  get filtered(): Session[] {
    const q = this.search.trim().toLowerCase();
    return this.sessions.filter((s) => {
      if (this.filter === 'archived') {
        return s.status === 'ARCHIVED' && this.matchesSearch(s, q);
      }
      if (this.filter === 'failed') {
        if (s.status !== 'FAILED' && s.status !== 'CANCELLED') return false;
        return this.matchesSearch(s, q);
      }
      if (this.filter === 'active') {
        const active =
          s.status === 'IDLE' ||
          s.status === 'STREAMING' ||
          s.status === 'WAITING_PERMISSION' ||
          s.status === 'WAITING_PLAN' ||
          s.status === 'COMPLETED';
        if (!active || s.status === 'ARCHIVED') return false;
        return this.matchesSearch(s, q);
      }
      // "all" includes archived so restores are discoverable without hunting the chip
      return this.matchesSearch(s, q);
    });
  }

  private matchesSearch(s: Session, q: string): boolean {
    if (!q) return true;
    return (
      s.title.toLowerCase().includes(q) ||
      s.workspacePath.toLowerCase().includes(q) ||
      String(s.provider).toLowerCase().includes(q)
    );
  }

  setFilter(f: SessionFilter): void {
    this.filter = f;
  }

  onSearch(value: string): void {
    this.searchChange.emit(value);
  }
}
