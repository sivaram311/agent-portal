import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-agent-input-bar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './agent-input-bar.component.html',
  styleUrl: './agent-input-bar.component.scss',
})
export class AgentInputBarComponent {
  @Input() busy = false;
  @Input() disabled = false;
  @Input() placeholder = 'Ask the agent to inspect, edit, or run tasks…';
  @Output() send = new EventEmitter<string>();

  draft = '';

  submit(): void {
    const text = this.draft.trim();
    if (!text || this.busy || this.disabled) {
      return;
    }
    this.send.emit(text);
    this.draft = '';
  }
}
