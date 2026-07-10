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

  get subagentTools(): ToolRun[] {
    return this.tools.filter(
      (t) => t.kind === 'subagent' || /agent|task/i.test(t.toolName)
    );
  }

  subagentKey(tool: ToolRun): string {
    return tool.subagentId || tool.toolCallId || tool.id;
  }

  isRunning(tool: ToolRun): boolean {
    return tool.status.toLowerCase() === 'running';
  }

  onAbandon(tool: ToolRun): void {
    this.abandon.emit(this.subagentKey(tool));
  }
}
