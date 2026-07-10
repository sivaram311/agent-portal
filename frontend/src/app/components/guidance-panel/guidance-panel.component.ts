import { Component, Input, OnChanges, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import { GuidancePack, SessionGuidance, SessionGuidanceItem } from '../../models/session.models';

@Component({
  selector: 'app-guidance-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './guidance-panel.component.html',
  styleUrl: './guidance-panel.component.scss',
})
export class GuidancePanelComponent implements OnChanges {
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);

  @Input({ required: true }) sessionId!: string;

  packs: GuidancePack[] = [];
  guidance?: SessionGuidance;
  loading = false;
  saving = false;
  sessionNote = '';
  /** packId -> enabled */
  enabledMap: Record<string, boolean> = {};

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['sessionId'] && this.sessionId) {
      this.reload();
    }
  }

  reload(): void {
    this.loading = true;
    this.api.listGuidancePacks().subscribe({
      next: (packs) => {
        this.packs = packs;
        this.api.getSessionGuidance(this.sessionId).subscribe({
          next: (g) => {
            this.guidance = g;
            this.enabledMap = {};
            const hasRows = g.items.some((i) => i.packId);
            for (const p of packs) {
              const row = g.items.find((i) => i.packId === p.id);
              if (row) {
                this.enabledMap[p.id] = row.enabled;
              } else if (!hasRows) {
                // No session rows yet — suggest user defaults
                this.enabledMap[p.id] = p.enabledByDefault;
              } else {
                this.enabledMap[p.id] = false;
              }
            }
            const note = g.items.find((i) => i.sessionOnly);
            this.sessionNote = note?.bodyMarkdown || '';
            this.loading = false;
          },
          error: (err) => {
            this.loading = false;
            this.toast.error(err?.error?.error || 'Failed to load session guidance');
          },
        });
      },
      error: (err) => {
        this.loading = false;
        this.toast.error(err?.error?.error || 'Failed to load packs');
      },
    });
  }

  get effective(): SessionGuidanceItem[] {
    return this.guidance?.effective ?? [];
  }

  save(): void {
    if (this.saving) {
      return;
    }
    this.saving = true;
    const items: {
      packId?: string | null;
      enabled?: boolean;
      sortOrder?: number;
      title?: string;
      sessionBody?: string;
      kind?: 'RULE' | 'SKILL';
    }[] = [];
    let order = 0;
    for (const p of this.packs) {
      items.push({
        packId: p.id,
        enabled: !!this.enabledMap[p.id],
        sortOrder: order++,
      });
    }
    const note = this.sessionNote.trim();
    if (note) {
      items.push({
        packId: null,
        enabled: true,
        sortOrder: order++,
        title: 'Session note',
        sessionBody: note,
        kind: 'RULE',
      });
    }
    this.api.putSessionGuidance(this.sessionId, items).subscribe({
      next: (g) => {
        this.guidance = g;
        this.saving = false;
        this.toast.success('Guidance updated');
      },
      error: (err) => {
        this.saving = false;
        this.toast.error(err?.error?.error || 'Save failed');
      },
    });
  }
}
