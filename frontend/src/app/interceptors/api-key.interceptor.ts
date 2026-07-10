import { HttpInterceptorFn } from '@angular/common/http';

export const apiKeyInterceptor: HttpInterceptorFn = (req, next) => {
  const key = localStorage.getItem('agentPortalApiKey');
  if (key) {
    return next(req.clone({ setHeaders: { 'X-API-Key': key } }));
  }
  return next(req);
};
