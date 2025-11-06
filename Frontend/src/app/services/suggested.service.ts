import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { JAVA_API } from '../config/api.config';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class SuggestedService {
  private apiUrl = `${JAVA_API}/suggested`;

  constructor(private http: HttpClient, private auth: AuthService) {}

  // Ottieni prodotti suggeriti (già salva visualizzazione automaticamente)
  getProdottiSuggeriti(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/suggested`);
  }

  salvaVisualizzazione(prodottoId: number): Observable<any> {
    // Se l'utente non è loggato, non salvare la visualizzazione
    if (!this.auth.isLoggedIn()) {
      console.log('Utente guest - visualizzazione non salvata');
      return of(null); // Ritorna un Observable vuoto
    }
    
    console.log('Salvataggio visualizzazione per prodotto ID:', prodottoId);
    return this.http.post<any>(`${this.apiUrl}/visualizza`, { prodotto_id: prodottoId });
  }
}