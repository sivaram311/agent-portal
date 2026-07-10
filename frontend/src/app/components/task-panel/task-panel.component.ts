import {
  AfterViewChecked,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToolRun } from '../../models/session.models';
import { SubagentPanelComponent } from '../subagent-panel/subagent-panel.component';

@Component({
  selector: 'app-task-panel',
  standalone: true,
  imports: [CommonModule, SubagentPanelComponent],
  templateUrl: './task-panel.component.html',
  styleUrl: './task-panel.component.scss',
})
export class TaskPanelComponent implements OnChanges, AfterViewChecked {
  @Input() tools: ToolRun[] = [];
  @Input() terminalLines: string[] = [];
  @Input() busy = false;
  @Output() abandon = new EventEmitter<string>();

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
