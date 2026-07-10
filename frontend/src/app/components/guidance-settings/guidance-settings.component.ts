import { Component, EventEmitter, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import { GuidanceKind, GuidancePack } from '../../models/session.models';

@Component({
  selector: 'app-guidance-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './guidance-settings.component.html',
  styleUrl: './guidance-settings.component.scss',
})
export class GuidanceSettingsComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);

  @Output() closed = new EventEmitter<void>();

  tab: GuidanceKind = 'RULE';
  packs: GuidancePack[] = [];
  loading = false;
  editing?: GuidancePack | null;
  draftTitle = '';
  draftDescription = '';
  draftBody = '';
  draftGlobs = '';
  draftAlwaysApply = true;
  draftEnabledByDefault = true;
  saving = false;

  ngOnInit(): void {
    this.reload();
  }

  setTab(kind: GuidanceKind): void {
    this.tab = kind;
    this.cancelEdit();
  }

  reload(): void {
    this.loading = true;
    this.api.listGuidancePacks().subscribe({
      next: (packs) => {
        this.packs = packs;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.toast.error(err?.error?.error || 'Failed to load guidance');
      },
    });
  }

  get filtered(): GuidancePack[] {
    return this.packs.filter((p) => p.kind === this.tab);
  }

  startCreate(): void {
    this.editing = null;
    this.draftTitle = '';
    this.draftDescription = '';
    this.draftBody = '';
    this.draftGlobs = '';
    this.draftAlwaysApply = true;
    this.draftEnabledByDefault = true;
  }

  startEdit(pack: GuidancePack): void {
    this.editing = pack;
    this.draftTitle = pack.title;
    this.draftDescription = pack.description || '';
    this.draftBody = pack.bodyMarkdown;
    this.draftGlobs = pack.globs || '';
    this.draftAlwaysApply = pack.alwaysApply;
    this.draftEnabledByDefault = pack.enabledByDefault;
  }

  cancelEdit(): void {
    this.editing = undefined;
  }

  get isEditorOpen(): boolean {
    return this.editing !== undefined;
  }

  save(): void {
    const title = this.draftTitle.trim();
    const body = this.draftBody.trim();
    if (!title || !body || this.saving) {
      return;
    }
    this.saving = true;
    if (this.editing) {
      this.api
        .updateGuidancePack(this.editing.id, {
          title,
          description: this.draftDescription.trim(),
          bodyMarkdown: body,
          globs: this.draftGlobs.trim() || undefined,
          alwaysApply: this.draftAlwaysApply,
          enabledByDefault: this.draftEnabledByDefault,
        })
        .subscribe({
          next: () => {
            this.saving = false;
            this.cancelEdit();
            this.reload();
            this.toast.success('Saved');
          },
          error: (err) => {
            this.saving = false;
            this.toast.error(err?.error?.error || 'Save failed');
          },
        });
    } else {
      this.api
        .createGuidancePack({
          kind: this.tab,
          title,
          description: this.draftDescription.trim(),
          bodyMarkdown: body,
          globs: this.draftGlobs.trim() || undefined,
          alwaysApply: this.draftAlwaysApply,
          enabledByDefault: this.draftEnabledByDefault,
        })
        .subscribe({
          next: () => {
            this.saving = false;
            this.cancelEdit();
            this.reload();
            this.toast.success(this.tab === 'RULE' ? 'Rule created' : 'Skill created');
          },
          error: (err) => {
            this.saving = false;
            this.toast.error(err?.error?.error || 'Create failed');
          },
        });
    }
  }

  toggleDefault(pack: GuidancePack): void {
    this.api.updateGuidancePack(pack.id, { enabledByDefault: !pack.enabledByDefault }).subscribe({
      next: () => this.reload(),
      error: (err) => this.toast.error(err?.error?.error || 'Update failed'),
    });
  }

  remove(pack: GuidancePack): void {
    if (!confirm(`Delete “${pack.title}”?`)) {
      return;
    }
    this.api.deleteGuidancePack(pack.id).subscribe({
      next: () => {
        this.toast.success('Deleted');
        this.reload();
      },
      error: (err) => this.toast.error(err?.error?.error || 'Delete failed'),
    });
  }

  installTemplates(): void {
    this.api.installGuidanceTemplates().subscribe({
      next: (created) => {
        this.toast.success(created.length ? `Installed ${created.length} starter pack(s)` : 'Starters already installed');
        this.reload();
      },
      error: (err) => this.toast.error(err?.error?.error || 'Install failed'),
    });
  }
}
