import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Observable, catchError, shareReplay, switchMap, throwError } from 'rxjs';
import {
  Catalog,
  Draw,
  DrawRequest,
  DrawSummary,
  ModelInfo,
  TournamentFormatInfo,
} from './contracts';

interface CsrfSession {
  headerName: string;
  token: string;
}

@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);
  private csrfSession?: Observable<CsrfSession>;

  players(asOf?: string) {
    return this.http.get<Catalog>('/api/tournaments/players', { params: asOf ? { asOf } : {} });
  }
  formats() {
    return this.http.get<TournamentFormatInfo[]>('/api/tournaments/formats');
  }
  models() {
    return this.http.get<ModelInfo[]>('/api/tournaments/models');
  }
  draws() {
    return this.http.get<DrawSummary[]>('/api/tournaments');
  }
  draw(id: string) {
    return this.http.get<Draw>(`/api/tournaments/${encodeURIComponent(id)}`);
  }
  preview(body: DrawRequest) {
    return this.mutate<Draw>('POST', '/api/tournaments/preview', body);
  }
  create(body: DrawRequest) {
    return this.mutate<Draw>('POST', '/api/tournaments', body);
  }
  update(id: string, body: DrawRequest) {
    return this.mutate<Draw>('PUT', `/api/tournaments/${encodeURIComponent(id)}`, body);
  }

  private session(refresh = false) {
    if (refresh || !this.csrfSession) {
      this.csrfSession = this.http
        .get<CsrfSession>('/api/session')
        .pipe(shareReplay({ bufferSize: 1, refCount: false }));
    }
    return this.csrfSession;
  }

  private mutate<T>(method: 'POST' | 'PUT', url: string, body: unknown) {
    const send = (session: CsrfSession) =>
      this.http.request<T>(method, url, {
        body,
        headers: new HttpHeaders({ [session.headerName]: session.token }),
      });
    return this.session().pipe(
      switchMap(send),
      catchError((error: unknown) => {
        // A rejected CSRF request has not been applied. Refresh only for this status;
        // network failures are never blindly retried because a write may have succeeded.
        if (error instanceof HttpErrorResponse && error.status === 403)
          return this.session(true).pipe(switchMap(send));
        return throwError(() => error);
      }),
    );
  }
}

export function apiError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const body: unknown = error.error;
    if (body && typeof body === 'object' && 'detail' in body && typeof body.detail === 'string')
      return body.detail;
    if (error.status === 0)
      return 'Nie można połączyć się z API Spring Boot. Sprawdź, czy backend działa.';
    return `Operacja nie powiodła się (${error.status}). Spróbuj ponownie.`;
  }
  return error instanceof Error ? error.message : 'Nie udało się wykonać operacji.';
}
