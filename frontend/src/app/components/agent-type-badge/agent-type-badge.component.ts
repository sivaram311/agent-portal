import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-agent-type-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="badge" [class.agy]="isAntigravity">
      {{ isAntigravity ? 'Antigravity' : 'Cursor' }}
    </span>
  `,
  styles: [
    `
      .badge {
        display: inline-flex;
        align-items: center;
        min-height: 1.5rem;
        padding: 0.15rem 0.55rem;
        border-radius: 999px;
        font-size: 0.68rem;
        font-weight: 650;
        letter-spacing: 0.04em;
        text-transform: uppercase;
        background: rgba(14, 165, 233, 0.14);
        color: #7dd3fc;
        white-space: nowrap;
      }
      .badge.agy {
        background: var(--ap-accent-soft);
        color: #5eead4;
      }
    `,
  ],
})
export class AgentTypeBadgeComponent {
  @Input() provider: string = 'cursor';

  get isAntigravity(): boolean {
    return this.provider === 'antigravity' || this.provider === 'agy';
  }
}
