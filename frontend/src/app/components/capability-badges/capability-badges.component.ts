import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HealthInfo } from '../../models/session.models';

@Component({
  selector: 'app-capability-badges',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './capability-badges.component.html',
  styleUrl: './capability-badges.component.scss',
})
export class CapabilityBadgesComponent {
  @Input() health?: HealthInfo;
}
