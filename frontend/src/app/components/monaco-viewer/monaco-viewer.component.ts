import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';

declare global {
  interface Window {
    require?: {
      config: (cfg: { paths: Record<string, string> }) => void;
      (deps: string[], cb: (monaco: MonacoApi) => void): void;
    };
  }
}

interface MonacoApi {
  editor: {
    create: (el: HTMLElement, options: Record<string, unknown>) => MonacoEditor;
    setModelLanguage: (model: { uri: unknown }, language: string) => void;
  };
}

interface MonacoEditor {
  getModel: () => { uri: unknown } | null;
  setValue: (v: string) => void;
  layout: () => void;
  dispose: () => void;
}

let monacoLoadPromise: Promise<MonacoApi> | null = null;

function monacoBaseUrl(): string {
  return `${document.baseURI.replace(/\/$/, '')}/assets/monaco/vs`;
}

function loadMonaco(): Promise<MonacoApi> {
  if (monacoLoadPromise) {
    return monacoLoadPromise;
  }
  monacoLoadPromise = new Promise((resolve, reject) => {
    const boot = () => {
      window.require!.config({ paths: { vs: monacoBaseUrl() } });
      window.require!(['vs/editor/editor.main'], (monaco: MonacoApi) => resolve(monaco));
    };
    if (window.require) {
      boot();
      return;
    }
    const existing = document.querySelector('script[data-monaco-loader]');
    if (existing) {
      existing.addEventListener('load', boot);
      existing.addEventListener('error', () => reject(new Error('Monaco loader failed')));
      return;
    }
    const script = document.createElement('script');
    script.src = `${monacoBaseUrl()}/loader.js`;
    script.dataset['monacoLoader'] = '1';
    script.onload = boot;
    script.onerror = () => reject(new Error('Monaco loader failed'));
    document.head.appendChild(script);
  });
  return monacoLoadPromise;
}

function languageForPath(path: string): string {
  const lower = path.toLowerCase();
  if (lower.endsWith('.ts') || lower.endsWith('.tsx')) return 'typescript';
  if (lower.endsWith('.js') || lower.endsWith('.jsx') || lower.endsWith('.mjs')) return 'javascript';
  if (lower.endsWith('.json')) return 'json';
  if (lower.endsWith('.html') || lower.endsWith('.htm')) return 'html';
  if (lower.endsWith('.css') || lower.endsWith('.scss')) return 'css';
  if (lower.endsWith('.md')) return 'markdown';
  if (lower.endsWith('.py')) return 'python';
  if (lower.endsWith('.java')) return 'java';
  if (lower.endsWith('.xml') || lower.endsWith('.svg')) return 'xml';
  if (lower.endsWith('.yml') || lower.endsWith('.yaml')) return 'yaml';
  if (lower.endsWith('.sh') || lower.endsWith('.ps1')) return 'shell';
  return 'plaintext';
}

@Component({
  selector: 'app-monaco-viewer',
  standalone: true,
  imports: [CommonModule],
  template: `<div #host class="monaco-host" data-testid="monaco-viewer"></div>`,
  styles: [
    `
      :host {
        display: block;
        height: 100%;
        min-height: 16rem;
      }
      .monaco-host {
        height: 100%;
        min-height: 16rem;
        border-radius: 0.5rem;
        overflow: hidden;
      }
    `,
  ],
})
export class MonacoViewerComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('host', { static: true }) host!: ElementRef<HTMLDivElement>;

  @Input() content = '';
  @Input() path = '';

  private editor?: MonacoEditor;
  private monaco?: MonacoApi;
  private viewReady = false;

  ngAfterViewInit(): void {
    this.viewReady = true;
    void this.mount();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.viewReady) {
      return;
    }
    if (changes['content'] || changes['path']) {
      void this.mount();
    }
  }

  ngOnDestroy(): void {
    this.editor?.dispose();
  }

  private async mount(): Promise<void> {
    try {
      this.monaco = await loadMonaco();
      if (!this.editor) {
        this.editor = this.monaco.editor.create(this.host.nativeElement, {
          value: this.content,
          language: languageForPath(this.path),
          theme: 'vs-dark',
          readOnly: true,
          minimap: { enabled: false },
          scrollBeyondLastLine: false,
          automaticLayout: true,
          fontSize: 13,
          wordWrap: 'on',
        });
      } else {
        this.editor.setValue(this.content);
        const model = this.editor.getModel();
        if (model) {
          this.monaco.editor.setModelLanguage(model, languageForPath(this.path));
        }
        this.editor.layout();
      }
    } catch {
      // Parent still shows content via empty host; ignore
    }
  }
}
