import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PermissionRequest } from '../../models/session.models';

@Component({
  selector: 'app-permission-dialog',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './permission-dialog.component.html',
  styleUrl: './permission-dialog.component.scss',
})
export class PermissionDialogComponent {
  @Input() permission?: PermissionRequest | null;
  @Output() decide = new EventEmitter<{ decision: string; reason?: string }>();

  get isPlan(): boolean {
    return this.permission?.kind === 'plan';
  }
}
