import { Injectable, NgZone, OnDestroy, inject } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Observable, Subject } from 'rxjs';
import { AgentEvent } from '../models/session.models';
import { wsUrl } from './backend-url';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class RealtimeService implements OnDestroy {
  private readonly auth = inject(AuthService);
  private readonly zone = inject(NgZone);
  private client?: Client;
  private readonly events$ = new Subject<AgentEvent>();
  private activeSessionId?: string;
  private subscription?: { unsubscribe: () => void };

  connect(): void {
    if (this.client?.active) {
      return;
    }
    this.client = new Client({
      webSocketFactory: () => {
        const token = this.auth.getAccessToken();
        const base = wsUrl();
        const url = token ? `${base}?access_token=${encodeURIComponent(token)}` : base;
        return new SockJS(url);
      },
      reconnectDelay: 3000,
      onConnect: () => {
        this.zone.run(() => {
          if (this.activeSessionId) {
            this.subscribeSession(this.activeSessionId);
          }
        });
      },
    });
    this.client.activate();
  }

  reconnect(): void {
    void this.client?.deactivate();
    this.client = undefined;
    this.connect();
  }

  watchSession(sessionId: string): Observable<AgentEvent> {
    this.activeSessionId = sessionId;
    this.connect();
    if (this.client?.connected) {
      this.subscribeSession(sessionId);
    }
    return this.events$.asObservable();
  }

  private subscribeSession(sessionId: string): void {
    this.subscription?.unsubscribe();
    this.subscription = this.client?.subscribe(`/topic/sessions/${sessionId}`, (msg: IMessage) => {
      try {
        const event = JSON.parse(msg.body) as AgentEvent;
        // SockJS/STOMP callbacks are outside Angular; run so streamingText updates paint live.
        this.zone.run(() => this.events$.next(event));
      } catch {
        // ignore malformed
      }
    });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
    void this.client?.deactivate();
    this.events$.complete();
  }
}
