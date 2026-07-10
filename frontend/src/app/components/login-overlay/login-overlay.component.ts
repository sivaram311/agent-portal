import { Component, inject, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login-overlay',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login-overlay.component.html',
  styleUrl: './login-overlay.component.scss',
})
export class LoginOverlayComponent {
  private readonly auth = inject(AuthService);

  readonly signedIn = output<void>();

  username = 'admin';
  password = '';
  error = '';
  busy = signal(false);

  async submit(): Promise<void> {
    this.error = '';
    this.busy.set(true);
    try {
      await this.auth.login(this.username.trim(), this.password);
      this.signedIn.emit();
    } catch (e: unknown) {
      if (e instanceof HttpErrorResponse) {
        this.error = e.error?.message || e.message || 'Login failed';
      } else if (e instanceof Error) {
        this.error = e.message;
      } else {
        this.error = 'Login failed';
      }
    } finally {
      this.busy.set(false);
    }
  }
}
