import { Component, Input, OnChanges, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { friendlyHttpError } from '../../core/friendly-error';
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
        this.error = friendlyHttpError(err, 'Failed to load changes');
      },
    });
  }

  open(change: FileChange): void {
    if (!this.sessionId) {
      return;
    }
    this.api.diffChange(this.sessionId, change.path).subscribe({
      next: (diff) => (this.selected = diff),
      error: (err) => (this.error = friendlyHttpError(err, 'Failed to load diff')),
    });
  }

  accept(): void {
    if (!this.sessionId || !this.selected) {
      return;
    }
    this.api.acceptChange(this.sessionId, this.selected.path).subscribe({
      next: () => {
        this.selected = undefined;
        this.reload();
      },
      error: (err) => (this.error = friendlyHttpError(err, 'Accept failed')),
    });
  }

  reject(): void {
    if (!this.sessionId || !this.selected) {
      return;
    }
    this.api.rejectChange(this.sessionId, this.selected.path).subscribe({
      next: () => {
        this.selected = undefined;
        this.reload();
      },
      error: (err) => (this.error = friendlyHttpError(err, 'Reject failed')),
    });
  }
}
