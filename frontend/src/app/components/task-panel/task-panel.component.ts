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
import { ToolRun } from '../../models/session.models';

@Component({
  selector: 'app-task-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './task-panel.component.html',
  styleUrl: './task-panel.component.scss',
})
export class TaskPanelComponent implements OnChanges, AfterViewChecked {
  @Input() tools: ToolRun[] = [];
  @Input() terminalLines: string[] = [];

  @ViewChild('terminalBody') terminalBody?: ElementRef<HTMLElement>;

  private pendingScroll = false;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['terminalLines'] || changes['tools']) {
      this.pendingScroll = true;
    }
  }

  ngAfterViewChecked(): void {
    if (!this.pendingScroll) {
      return;
    }
    this.pendingScroll = false;
    const el = this.terminalBody?.nativeElement;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  }
}
