import { HttpInterceptorFn } from '@angular/common/http';

/** Legacy local API-key fallback when CSS JWT is not used. */
export const apiKeyInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.headers.has('Authorization') || req.headers.has('X-API-Key')) {
    return next(req);
  }
  const key = localStorage.getItem('agentPortalApiKey');
  if (key) {
    return next(req.clone({ setHeaders: { 'X-API-Key': key } }));
  }
  return next(req);
};
