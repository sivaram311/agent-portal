import { Component, Input, OnChanges, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { ChatMessage, HistoryItem, ToolRun } from '../../models/session.models';

@Component({
  selector: 'app-history-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './history-panel.component.html',
  styleUrl: './history-panel.component.scss',
})
export class HistoryPanelComponent implements OnChanges {
  private readonly api = inject(ApiService);

  @Input() sessionId?: string;
  @Input() messages: ChatMessage[] = [];
  @Input() tools: ToolRun[] = [];

  items: HistoryItem[] = [];
  loading = false;
  error = '';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['sessionId'] || changes['messages'] || changes['tools']) {
      this.reload();
    }
  }

  reload(): void {
    if (!this.sessionId) {
      this.items = [];
      return;
    }
    this.loading = true;
    this.error = '';
    this.api.events(this.sessionId).subscribe({
      next: (events) => {
        const items: HistoryItem[] = [];
        for (const m of this.messages) {
          items.push({
            kind: 'message',
            at: m.createdAt,
            title: m.role,
            detail: m.content.slice(0, 240),
          });
        }
        for (const t of this.tools) {
          items.push({
            kind: 'tool',
            at: t.startedAt,
            title: t.toolName,
            detail: `${t.status}${t.kind ? ' · ' + t.kind : ''}`,
          });
        }
        for (const e of events) {
          items.push({
            kind: 'event',
            at: e.createdAt,
            title: e.type,
            detail: JSON.stringify(e.payload ?? {}).slice(0, 180),
          });
        }
        items.sort((a, b) => a.at.localeCompare(b.at));
        this.items = items;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.error || 'Failed to load history';
      },
    });
  }
}
