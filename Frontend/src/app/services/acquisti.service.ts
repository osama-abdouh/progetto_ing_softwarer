import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';
import { JAVA_API } from '../config/api.config';

export interface DatiCheckout {
  metodo_pagamento: string;
  nome_intestatario: string;
  numero_carta: string;
  scadenza: string;
  cvv: string;
}

export interface RisultatoAcquisto {
  success: boolean;
  message: string;
  acquisti: any[];
  totale: number;
}

@Injectable({
  providedIn: 'root'
})
export class AcquistiService {
  private baseUrl = 'http://localhost:8080/api/acquisti';

  constructor(private http: HttpClient, private authService: AuthService) {}

  private getIdUtente(): number | null {
    const user = this.authService.getUser();
    return user ? user.id : null;
  }

  // Processa il checkout
  processaCheckout(datiCheckout: any): Observable<any> {
    const idUtente = this.getIdUtente();
    if (!idUtente) {
      throw new Error('Utente non autenticato');
    }

    // Aggiungi automaticamente l'ID utente ai dati
    const datiCompleti = {
      ...datiCheckout,
      id_utente: idUtente
    };

    console.log('Dati inviati al backend:', datiCompleti); // Per debug
    
  // Checkout now goes to Java backend
  return this.http.post<any>(`${JAVA_API}/acquisti/checkout`, datiCompleti);
  }

  // Ottieni storico acquisti dell'utente
  getStoricoAcquisti(): Observable<any[]> {
    const idUtente = this.getIdUtente();
    if (!idUtente) {
      throw new Error('Utente non autenticato');
    }

    return this.http.get<any[]>(`${this.baseUrl}/storico/${idUtente}`);
  }

  // Ottieni dettagli di un singolo acquisto
  getDettaglioAcquisto(idAcquisto: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/dettaglio/${idAcquisto}`);
  }
}