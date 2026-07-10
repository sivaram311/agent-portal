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
import { ChatMessage } from '../../models/session.models';
import { MarkdownPipe } from '../../pipes/markdown.pipe';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, MarkdownPipe],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss',
})
export class ChatComponent implements OnChanges, AfterViewChecked {
  @Input() messages: ChatMessage[] = [];
  @Input() streamingText = '';

  @ViewChild('scrollBox') scrollBox?: ElementRef<HTMLDivElement>;

  private pendingScroll = false;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['messages'] || changes['streamingText']) {
      this.pendingScroll = true;
    }
  }

  ngAfterViewChecked(): void {
    if (!this.pendingScroll) {
      return;
    }
    this.pendingScroll = false;
    const el = this.scrollBox?.nativeElement;
    if (!el) {
      this.pendingScroll = true;
      return;
    }
    el.scrollTop = el.scrollHeight;
  }
}
