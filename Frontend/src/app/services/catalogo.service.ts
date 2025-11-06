import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';
import { WishListService } from './wishlist.service';
import { JAVA_API } from '../config/api.config';
@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private apiUrl = `${JAVA_API}/catalogo`;

  constructor(private http: HttpClient, private authService: AuthService, private wishlistService: WishListService) {}

  // Ottieni prodotti più acquistati (per la home)
  getProdottiPopular(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/popular`);
  }

  // Ottieni tutte le categorie
  getCategorie(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/prodotti`);
  }

  // Ottieni prodotti per categoria
  getProdottiPerCategoria(nomeCategoria: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/prodotti/categoria/${nomeCategoria}`);
  }

  // Ottieni tutti i marchi
  getBrand(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/brand`);
  }

  // Ottieni suggerimenti di ricerca
  getSearchSuggestions(query: string, limit: number = 5): Observable<any[]> {
    if (!query || query.trim().length < 2) { //se la query è vuota o troppo corta
      return new Observable(observer => observer.next([])); //restituisce subito un array vuoto
    }
    return this.http.get<any[]>(`${this.apiUrl}/search/suggestions`, { //altrimenti fa una richiesta GET all'endpoint delle suggerimenti (catalogo.js)
      params: { q: query.trim(), limit: limit.toString() } //params: serve a passare i parametri alla richiesta HTTP GET. (inviati come stringa senza spazi come un link)
    });
  }

  // Ottieni prodotti in vetrina (selezionati dall'admin)
  getProdottiVetrina(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/vetrina`);
  }
  
private getIdUtente(): number | null {
    const user = this.authService.getUser();  //chiama getUser di authService
    return user ? user.id : null;
  }
  
  aggiungiAWishlist(prodotto: any): void {
  const idUtente = this.getIdUtente();
  if (!idUtente) {
    alert('Devi essere autenticato per aggiungere alla wishlist!');
    return;
  }
  this.http.post(`${JAVA_API}/wishlist`, {
    prodotto_id: prodotto.id_prodotto
  }).subscribe({
    next: () => {
      alert('Prodotto aggiunto alla wishlist!');
      this.wishlistService.refreshCount();
    },
    error: (err) => {
      console.error('Errore wishlist:', err);
      alert('Errore nell\'aggiungere il prodotto alla wishlist');
    }
  });
}
  


}