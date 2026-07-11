import {
  AfterViewChecked,
  Component,
  ElementRef,
  Input,
  OnChanges,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-console-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './console-panel.component.html',
  styleUrl: './console-panel.component.scss',
})
export class ConsolePanelComponent implements OnChanges, AfterViewChecked {
  @Input() lines: string[] = [];
  @Input() busy = false;

  @ViewChild('body') body?: ElementRef<HTMLElement>;

  private pendingScroll = false;
  copyHint = '';

  get text(): string {
    return this.lines.join('');
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['lines']) {
      this.pendingScroll = true;
    }
  }

  ngAfterViewChecked(): void {
    if (!this.pendingScroll) {
      return;
    }
    this.pendingScroll = false;
    const el = this.body?.nativeElement;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  }

  async copyAll(): Promise<void> {
    const value = this.text;
    if (!value) {
      return;
    }
    try {
      await navigator.clipboard.writeText(value);
      this.copyHint = 'Copied';
      setTimeout(() => (this.copyHint = ''), 1500);
    } catch {
      this.copyHint = 'Copy failed';
      setTimeout(() => (this.copyHint = ''), 1500);
    }
  }
}
