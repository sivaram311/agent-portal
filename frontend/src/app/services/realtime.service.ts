import { Injectable, OnDestroy } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Observable, Subject } from 'rxjs';
import { AgentEvent } from '../models/session.models';
import { wsUrl } from './backend-url';

@Injectable({ providedIn: 'root' })
export class RealtimeService implements OnDestroy {
  private client?: Client;
  private readonly events$ = new Subject<AgentEvent>();
  private activeSessionId?: string;
  private subscription?: { unsubscribe: () => void };

  connect(): void {
    if (this.client?.active) {
      return;
    }
    this.client = new Client({
      webSocketFactory: () => new SockJS(wsUrl()),
      reconnectDelay: 3000,
      onConnect: () => {
        if (this.activeSessionId) {
          this.subscribeSession(this.activeSessionId);
        }
      },
    });
    this.client.activate();
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
        this.events$.next(event);
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
