import { Component, Input, OnChanges, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { AuditEvent } from '../../models/session.models';

@Component({
  selector: 'app-audit-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './audit-panel.component.html',
  styleUrl: './audit-panel.component.scss',
})
export class AuditPanelComponent implements OnChanges {
  private readonly api = inject(ApiService);

  @Input() sessionId?: string;

  events: AuditEvent[] = [];
  error = '';
  loading = false;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['sessionId']) {
      this.reload();
    }
  }

  reload(): void {
    this.loading = true;
    this.error = '';
    this.api.audit(this.sessionId, 40).subscribe({
      next: (events) => {
        this.events = events;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.error || 'Failed to load audit';
        this.events = [];
      },
    });
  }
}
