import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getAccessToken();
  const skipAuth =
    req.url.includes('/auth/login') ||
    req.url.includes('/auth/refresh') ||
    req.url.includes('/oauth/token') ||
    req.url.includes('/oauth/authorize');

  const cloned =
    token && !skipAuth && !req.headers.has('Authorization')
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(cloned).pipe(
    catchError((err: unknown) => {
      if (!(err instanceof HttpErrorResponse) || err.status !== 401) {
        return throwError(() => err);
      }
      const cfg = auth.config();
      if (!cfg?.cssEnabled || skipAuth || req.url.includes('/auth/')) {
        return throwError(() => err);
      }
      return from(auth.tryRefresh()).pipe(
        switchMap((ok) => {
          if (!ok) {
            void auth.logout();
            return throwError(() => err);
          }
          const refreshed = auth.getAccessToken();
          return next(
            req.clone({
              setHeaders: refreshed ? { Authorization: `Bearer ${refreshed}` } : {},
            })
          );
        })
      );
    })
  );
};
