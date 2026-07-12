import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToolRun } from '../../models/session.models';
import { StatusPillComponent } from '../status-pill/status-pill.component';

@Component({
  selector: 'app-subagent-panel',
  standalone: true,
  imports: [CommonModule, StatusPillComponent],
  templateUrl: './subagent-panel.component.html',
  styleUrl: './subagent-panel.component.scss',
})
export class SubagentPanelComponent {
  @Input() tools: ToolRun[] = [];
  @Input() busy = false;
  @Output() abandon = new EventEmitter<string>();

  /** When false, completed/cancelled/abandoned rows stay collapsed. */
  showFinished = false;

  get subagentTools(): ToolRun[] {
    return this.tools.filter(
      (t) => t.kind === 'subagent' || /agent|task/i.test(t.toolName || '')
    );
  }

  get activeSubagents(): ToolRun[] {
    return this.subagentTools.filter((t) => this.isActive(t));
  }

  get finishedSubagents(): ToolRun[] {
    return this.subagentTools.filter((t) => !this.isActive(t));
  }

  get visibleSubagents(): ToolRun[] {
    return this.showFinished
      ? [...this.activeSubagents, ...this.finishedSubagents]
      : this.activeSubagents;
  }

  subagentKey(tool: ToolRun): string {
    return tool.subagentId || tool.toolCallId || tool.id;
  }

  isActive(tool: ToolRun): boolean {
    const s = (tool.status || '').toLowerCase();
    return (
      s === 'running' ||
      s === 'pending' ||
      s === 'in_progress' ||
      s === 'in-progress'
    );
  }

  isRunning(tool: ToolRun): boolean {
    return this.isActive(tool);
  }

  onAbandon(tool: ToolRun): void {
    this.abandon.emit(this.subagentKey(tool));
  }
}
