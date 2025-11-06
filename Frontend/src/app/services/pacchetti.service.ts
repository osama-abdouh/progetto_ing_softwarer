import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { JAVA_API } from '../config/api.config';

export interface Pacchetto { //indica quali "proprietà" ha in questo caso un pacchetto
  nome: string;
  id_pacchetto: number;
  descrizione: string;
  prezzo_originale: number;
  prezzo_effettivo: number;
  prezzo_scontato: number;
}

export interface PacchettoDettaglio { //ha sia pacchetto come quello sopra più un array di prodotti inclusi
  pacchetto: Pacchetto;
  prodotti: any[];
}

@Injectable({ 
  providedIn: 'root' 
})
export class PacchettiService {
  private apiUrl = `${JAVA_API}/pacchetti`;

  constructor(private http: HttpClient) {}

  // Ottieni tutti i pacchetti tematici
  getPacchetti(): Observable<Pacchetto[]> {
    return this.http.get<Pacchetto[]>(this.apiUrl);
  }

  // Ottieni dettagli di un pacchetto specifico con prodotti
  getPacchettoDettaglio(id: number): Observable<PacchettoDettaglio> {
    return this.http.get<PacchettoDettaglio>(`${this.apiUrl}/${id}`);
  }
}