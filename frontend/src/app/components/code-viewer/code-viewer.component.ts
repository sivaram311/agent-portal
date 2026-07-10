import { Component, Input, OnChanges, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ApiService } from '../../services/api.service';
import { FileContent, FileEntry } from '../../models/session.models';
import { MarkdownPipe } from '../../pipes/markdown.pipe';
import { MonacoViewerComponent } from '../monaco-viewer/monaco-viewer.component';

@Component({
  selector: 'app-code-viewer',
  standalone: true,
  imports: [CommonModule, MarkdownPipe, MonacoViewerComponent],
  templateUrl: './code-viewer.component.html',
  styleUrl: './code-viewer.component.scss',
})
export class CodeViewerComponent implements OnChanges {
  private readonly api = inject(ApiService);
  private readonly sanitizer = inject(DomSanitizer);

  @Input() sessionId?: string;
  @Input() previewMode = false;

  entries: FileEntry[] = [];
  currentPath = '';
  selectedPath?: string;
  fileContent?: FileContent;
  loading = false;
  loadError = '';
  htmlPreviewUrl?: SafeResourceUrl;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['sessionId']) {
      this.currentPath = '';
      this.selectedPath = undefined;
      this.fileContent = undefined;
      this.htmlPreviewUrl = undefined;
      this.loadError = '';
      if (this.sessionId) {
        this.loadDirectory('');
      } else {
        this.entries = [];
      }
    }
    if (changes['previewMode'] && this.fileContent) {
      this.refreshHtmlPreview();
    }
  }

  get breadcrumbs(): { label: string; path: string }[] {
    if (!this.currentPath) {
      return [{ label: 'workspace', path: '' }];
    }
    const parts = this.currentPath.split('/').filter(Boolean);
    const crumbs: { label: string; path: string }[] = [{ label: 'workspace', path: '' }];
    let acc = '';
    for (const part of parts) {
      acc = acc ? `${acc}/${part}` : part;
      crumbs.push({ label: part, path: acc });
    }
    return crumbs;
  }

  get isMarkdownPreview(): boolean {
    return (
      this.previewMode &&
      !!this.selectedPath &&
      this.selectedPath.toLowerCase().endsWith('.md') &&
      !!this.fileContent &&
      !this.isImage
    );
  }

  get isHtmlPreview(): boolean {
    if (!this.previewMode || !this.selectedPath || !this.fileContent || this.isImage) {
      return false;
    }
    const p = this.selectedPath.toLowerCase();
    return p.endsWith('.html') || p.endsWith('.htm');
  }

  get isImage(): boolean {
    return !!this.fileContent?.mediaType.startsWith('image/');
  }

  loadDirectory(path: string): void {
    if (!this.sessionId) {
      return;
    }
    this.loading = true;
    this.loadError = '';
    this.api.listFiles(this.sessionId, path || undefined).subscribe({
      next: (entries) => {
        this.currentPath = path;
        this.entries = entries;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.loadError = err?.error?.error || 'Failed to list files';
        this.entries = [];
      },
    });
  }

  openEntry(entry: FileEntry): void {
    if (entry.directory) {
      this.loadDirectory(entry.path);
      return;
    }
    this.selectFile(entry.path);
  }

  selectFile(path: string): void {
    if (!this.sessionId) {
      return;
    }
    this.selectedPath = path;
    this.loading = true;
    this.loadError = '';
    this.htmlPreviewUrl = undefined;
    this.api.readFile(this.sessionId, path).subscribe({
      next: (content) => {
        this.fileContent = content;
        this.loading = false;
        this.refreshHtmlPreview();
      },
      error: (err) => {
        this.loading = false;
        this.loadError = err?.error?.error || 'Failed to read file';
        this.fileContent = undefined;
      },
    });
  }

  navigateTo(path: string): void {
    this.selectedPath = undefined;
    this.fileContent = undefined;
    this.htmlPreviewUrl = undefined;
    this.loadDirectory(path);
  }

  private refreshHtmlPreview(): void {
    if (!this.isHtmlPreview || !this.fileContent) {
      this.htmlPreviewUrl = undefined;
      return;
    }
    const blob = new Blob([this.fileContent.content], { type: 'text/html' });
    const url = URL.createObjectURL(blob);
    this.htmlPreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }
}
