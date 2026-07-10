import { Component, Input, OnChanges, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { FileChange } from '../../models/session.models';

@Component({
  selector: 'app-changes-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './changes-panel.component.html',
  styleUrl: './changes-panel.component.scss',
})
export class ChangesPanelComponent implements OnChanges {
  private readonly api = inject(ApiService);

  @Input() sessionId?: string;

  changes: FileChange[] = [];
  selected?: FileChange;
  loading = false;
  error = '';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['sessionId']) {
      this.selected = undefined;
      this.reload();
    }
  }

  reload(): void {
    if (!this.sessionId) {
      this.changes = [];
      return;
    }
    this.loading = true;
    this.error = '';
    this.api.listChanges(this.sessionId).subscribe({
      next: (list) => {
        this.changes = list;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.error || 'Failed to load changes';
      },
    });
  }

  open(change: FileChange): void {
    if (!this.sessionId) {
      return;
    }
    this.api.diffChange(this.sessionId, change.path).subscribe({
      next: (diff) => (this.selected = diff),
      error: (err) => (this.error = err?.error?.error || 'Failed to load diff'),
    });
  }
}
