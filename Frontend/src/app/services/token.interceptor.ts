import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const TokenInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const token = localStorage.getItem('token');
  
  // Rotte pubbliche accessibili senza token
  const publicRoutes = [
    '/api/auth/login',
    '/api/auth/register',
    '/api/catalogo',
    '/api/products',
    '/api/pacchetti',
    '/api/images',
    '/api/immagine',
    '/assets',
    '/uploads',
    '/public'
  ];
  
  const isPublicRoute = publicRoutes.some(route => req.url.includes(route));

  // Se non c'è token e NON è una rotta pubblica e NON è una richiesta GET
  // allora richiedi il login (solo per POST/PUT/DELETE che modificano dati)
  if (!token && !isPublicRoute && req.method !== 'GET') {
    localStorage.clear();
    sessionStorage.clear();
    router.navigate(['/login']);
    return throwError(() => new Error('Autenticazione richiesta'));
  }
  
  // Clona la richiesta aggiungendo il token se presente
  let clonedReq = req;
  if (token) {
    clonedReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(clonedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Gestisce errori 401 (non autorizzato) solo per rotte non pubbliche
      const isPublicRoute = publicRoutes.some(route => req.url.includes(route));
      if (error.status === 401 && !isPublicRoute) {
        console.log('401 Unauthorized - reindirizzamento al login');
        localStorage.clear();
        sessionStorage.clear();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};