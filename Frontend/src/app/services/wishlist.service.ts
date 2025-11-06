import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class WishListService {
  private apiUrl = 'http://localhost:8080/api/wishlist';
  private wishlistCountSubject = new BehaviorSubject<number>(0);
  wishlistCount$ = this.wishlistCountSubject.asObservable();

  constructor(private http: HttpClient, private authService: AuthService) {}

  private getIdUtente(): number | null {
    const user = this.authService.getUser();
    return user ? user.id : null;
  }

  rimuovi(prodottoId: number): Observable<any> {
    const userId = this.getIdUtente();
    if (!userId) throw new Error('Utente non autenticato');
    // Java backend derives user from JWT, only prodotto_id path param is needed
    return this.http.delete(`${this.apiUrl}/${prodottoId}`).pipe(
      tap(() => this.refreshCount())
    );
  }

  getWishlist(userId: number): Observable<any[]> {
    // Java backend derives user from JWT, no userId path needed
    return this.http.get<any[]>(`${this.apiUrl}`);
  }
  refreshCount(): void {
    const userId = this.getIdUtente();
    if (!userId) { this.wishlistCountSubject.next(0); return; }
    this.http.get<any[]>(`${this.apiUrl}`).subscribe({
      next: (arr) => this.wishlistCountSubject.next(Array.isArray(arr) ? arr.length : 0),
      error: () => this.wishlistCountSubject.next(0)
    });
  }
  
}