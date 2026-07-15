import { Component, OnInit, inject, output, signal } from '@angular/core';
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
export class LoginOverlayComponent implements OnInit {
  private readonly auth = inject(AuthService);

  readonly signedIn = output<void>();

  username = 'admin';
  password = '';
  error = '';
  busy = signal(false);
  showPassword = signal(true);
  showSso = signal(false);

  ngOnInit(): void {
    const cfg = this.auth.config();
    this.showPassword.set(this.auth.passwordEnabled(cfg));
    this.showSso.set(this.auth.ssoEnabled(cfg));
    const oauthErr = this.auth.oauthError();
    if (oauthErr) {
      this.error = oauthErr;
    }
  }

  async submit(): Promise<void> {
    this.error = '';
    this.busy.set(true);
    try {
      await this.auth.login(this.username.trim(), this.password);
      this.signedIn.emit();
    } catch (e: unknown) {
      this.error = this.describeError(e);
    } finally {
      this.busy.set(false);
    }
  }

  async startSso(): Promise<void> {
    this.error = '';
    this.busy.set(true);
    try {
      await this.auth.beginOAuthLogin();
      // navigation away — busy stays true
    } catch (e: unknown) {
      this.error = this.describeError(e);
      this.busy.set(false);
    }
  }

  private describeError(e: unknown): string {
    if (e instanceof HttpErrorResponse) {
      return e.error?.message || e.error?.error || e.message || 'Login failed';
    }
    if (e instanceof Error) {
      return e.message;
    }
    return 'Login failed';
  }
}
