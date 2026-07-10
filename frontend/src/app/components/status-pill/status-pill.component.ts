import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-status-pill',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="pill" [attr.data-status]="normalized">
      <span class="dot" aria-hidden="true"></span>
      {{ label }}
    </span>
  `,
  styles: [
    `
      .pill {
        display: inline-flex;
        align-items: center;
        gap: 0.35rem;
        min-height: 1.5rem;
        padding: 0.15rem 0.55rem;
        border-radius: 999px;
        font-size: 0.68rem;
        font-weight: 600;
        letter-spacing: 0.04em;
        text-transform: uppercase;
        background: rgba(148, 163, 184, 0.12);
        color: var(--ap-text-muted);
        white-space: nowrap;
      }
      .dot {
        width: 0.45rem;
        height: 0.45rem;
        border-radius: 50%;
        background: currentColor;
      }
      .pill[data-status='idle'],
      .pill[data-status='completed'] {
        color: #86efac;
        background: rgba(34, 197, 94, 0.14);
      }
      .pill[data-status='streaming'],
      .pill[data-status='waiting_permission'],
      .pill[data-status='waiting_plan'] {
        color: #fcd34d;
        background: rgba(245, 158, 11, 0.14);
      }
      .pill[data-status='failed'],
      .pill[data-status='cancelled'] {
        color: #fca5a5;
        background: rgba(239, 68, 68, 0.14);
      }
      .pill[data-status='archived'] {
        color: #cbd5e1;
        background: rgba(148, 163, 184, 0.12);
      }
    `,
  ],
})
export class StatusPillComponent {
  @Input() status = 'IDLE';

  get normalized(): string {
    return (this.status || 'IDLE').toLowerCase().replace(/_/g, '_');
  }

  get label(): string {
    return (this.status || 'IDLE').replace(/_/g, ' ');
  }
}
