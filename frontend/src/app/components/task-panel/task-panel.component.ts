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

  /** Collapsed by default — full scrollback lives in Console (Cursor + Antigravity). */
  terminalOpen = false;
  private userToggledTerminal = false;
  private pendingScroll = false;

  /** Tool runs list excludes subagents — those live only in the Sub-agents panel. */
  get nonSubagentTools(): ToolRun[] {
    return this.tools.filter(
      (t) => t.kind !== 'subagent' && !/agent|task/i.test(t.toolName || '')
    );
  }

  get terminalLineCount(): number {
    return this.terminalLines.length;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['terminalLines'] || changes['tools']) {
      this.pendingScroll = true;
    }
    if (changes['terminalLines']) {
      if (!this.terminalLines.length) {
        // Session switch / clear — reset so the next run can auto-open again.
        this.userToggledTerminal = false;
        this.terminalOpen = false;
      } else if (!this.userToggledTerminal) {
        // Auto-open when chunks arrive (Cursor + Antigravity share terminalLines).
        this.terminalOpen = true;
      }
    }
  }

  toggleTerminal(): void {
    this.userToggledTerminal = true;
    this.terminalOpen = !this.terminalOpen;
    if (this.terminalOpen) {
      this.pendingScroll = true;
    }
  }

  ngAfterViewChecked(): void {
    if (!this.pendingScroll || !this.terminalOpen) {
      return;
    }
    this.pendingScroll = false;
    const el = this.terminalBody?.nativeElement;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  }
}
