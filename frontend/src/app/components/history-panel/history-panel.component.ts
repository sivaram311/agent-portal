import { Component, Input, OnChanges, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { friendlyHttpError } from '../../core/friendly-error';
import { ChatMessage, HistoryItem, ToolRun } from '../../models/session.models';

/** High-churn ACP stream events — collapsed by default in History. */
const NOISY_EVENT_TYPES = new Set([
  'assistant_delta',
  'thinking_delta',
  'session_update',
  'cursor_update_todos',
]);

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
  showProtocolNoise = false;
  collapsedNoiseCount = 0;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['sessionId'] || changes['messages'] || changes['tools']) {
      this.reload();
    }
  }

  toggleNoise(): void {
    this.showProtocolNoise = !this.showProtocolNoise;
    this.reload();
  }

  reload(): void {
    if (!this.sessionId) {
      this.items = [];
      this.collapsedNoiseCount = 0;
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

        let noise = 0;
        const deltaBuckets = new Map<string, { count: number; at: string; sample: string }>();

        for (const e of events) {
          if (!this.showProtocolNoise && NOISY_EVENT_TYPES.has(e.type)) {
            noise++;
            if (e.type === 'assistant_delta' || e.type === 'thinking_delta') {
              const key = e.type;
              const text = String(e.payload['text'] ?? '');
              const prev = deltaBuckets.get(key);
              if (prev) {
                prev.count += 1;
                prev.at = e.createdAt;
                if (prev.sample.length < 120) {
                  prev.sample = (prev.sample + text).slice(0, 120);
                }
              } else {
                deltaBuckets.set(key, { count: 1, at: e.createdAt, sample: text.slice(0, 120) });
              }
            }
            continue;
          }
          items.push({
            kind: 'event',
            at: e.createdAt,
            title: e.type,
            detail: JSON.stringify(e.payload ?? {}).slice(0, 180),
          });
        }

        if (!this.showProtocolNoise) {
          for (const [type, bucket] of deltaBuckets) {
            items.push({
              kind: 'event',
              at: bucket.at,
              title: `${type} (collapsed ×${bucket.count})`,
              detail: bucket.sample ? `…${bucket.sample}` : `${bucket.count} stream chunks omitted`,
            });
          }
          if (noise > 0) {
            this.collapsedNoiseCount = noise;
          } else {
            this.collapsedNoiseCount = 0;
          }
        } else {
          this.collapsedNoiseCount = 0;
        }

        items.sort((a, b) => a.at.localeCompare(b.at));
        this.items = items;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = friendlyHttpError(err, 'Failed to load history');
      },
    });
  }
}
