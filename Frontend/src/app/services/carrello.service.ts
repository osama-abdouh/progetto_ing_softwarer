import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap, map } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { JAVA_API } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class CarrelloService {
  private baseUrl = `${JAVA_API}/carrello`;
  
  private carrelloSubject = new BehaviorSubject<any[]>(this.getCarrelloGuest());
  public carrello$ = this.carrelloSubject.asObservable();

  constructor(private http: HttpClient, private authService: AuthService) {

    if (this.authService.isLoggedIn()) {
      this.caricaCarrello();
    }
  }

  //qui prende id utente dal token, Il token è firmato dal backend con una chiave segreta
  private getIdUtente(): number | null {
    const user = this.authService.getUser();  //chiama getUser di authService
    return user ? user.id : null;
  }

  // Gestisce carrello per utenti autenticati e guest
  aggiungiAlCarrello(idProdotto: number, quantita: number): Observable<any> { //prepara la chiamata HTTP con i dati necessari
    const idUtente = this.getIdUtente(); //prende id utente da sopra 
    
    if (!idUtente) {
      // Utente guest: salva nel localStorage
      return new Observable(observer => {
        const carrelloGuest = this.getCarrelloGuest();
        const prodottoEsistente = carrelloGuest.find((p: any) => p.id_prodotto === idProdotto);
        
        if (prodottoEsistente) {
          prodottoEsistente.quantita += quantita;
        } else {
          carrelloGuest.push({ id_prodotto: idProdotto, quantita: quantita });
        }
        
        this.setCarrelloGuest(carrelloGuest);
        this.carrelloSubject.next(carrelloGuest); // Aggiorna il BehaviorSubject
        observer.next({ success: true });
        observer.complete();
      });
    }
    
    return this.http.post(`${this.baseUrl}/aggiungi`, { //fa la chiamata al backend post con i dati necessari
      id_utente: idUtente,
      id_prodotto: idProdotto,
      quantita: quantita
    }).pipe(
      tap(() => this.caricaCarrello()) //appena tarmina la post aggiorna il carrello nel frontend
    ); // chiama private caricaCarrello() per aggiornare il carrello locale
  }

  // Aggiungi pacchetto al carrello (gestisce utenti autenticati e guest)
  aggiungiPacchettoAlCarrello(idPacchetto: number, quantita: number): Observable<any> {
    const idUtente = this.getIdUtente();
    
    if (!idUtente) {
      // Utente guest: salva pacchetto nel localStorage
      return new Observable(observer => {
        const carrelloGuest = this.getCarrelloGuest();
        const pacchettoEsistente = carrelloGuest.find((p: any) => p.id_pacchetto === idPacchetto);
        
        if (pacchettoEsistente) {
          pacchettoEsistente.quantita += quantita;
        } else {
          carrelloGuest.push({ id_pacchetto: idPacchetto, quantita: quantita });
        }
        
        this.setCarrelloGuest(carrelloGuest);
        this.carrelloSubject.next(carrelloGuest); // Aggiorna il BehaviorSubject
        observer.next({ success: true });
        observer.complete();
      });
    }

    return this.http.post(`${this.baseUrl}/aggiungi-pacchetto`, {
      id_utente: idUtente,
      id_pacchetto: idPacchetto,
      quantita: quantita
    }).pipe(
      tap(() => this.caricaCarrello())
    );
  }

  rimuoviDalCarrello(idProdotto: number): Observable<any> {
    const idUtente = this.getIdUtente();
    if (!idUtente) {
      throw new Error('Utente non autenticato. Effettua il login per gestire il carrello.');
    }
    
    
    return this.http.delete(`${this.baseUrl}/rimuovi/${idUtente}/${idProdotto}`).pipe(
      tap(() => this.caricaCarrello())
    );
  }

  // Rimuovi un pacchetto dal carrello
  rimuoviPacchetto(idPacchetto: number): Observable<any> {
    const idUtente = this.getIdUtente();
    if (!idUtente) {
      throw new Error('Utente non autenticato. Effettua il login per gestire il carrello.');
    }

    return this.http.delete(`${this.baseUrl}/rimuoviPacchetto/${idUtente}/${idPacchetto}`).pipe(
      tap(() => this.caricaCarrello())
    );
  }

  // Aggiorna quantità di un pacchetto
  aggiornaPacchetto(idPacchetto: number, quantita: number): Observable<any> {
    const idUtente = this.getIdUtente();
    if (!idUtente) {
      throw new Error('Utente non autenticato. Effettua il login per gestire il carrello.');
    }

    return this.http.put(`${this.baseUrl}/aggiornaPacchetto`, {
      id_utente: idUtente,
      id_pacchetto: idPacchetto,
      quantita: quantita
    }).pipe(
      tap(() => this.caricaCarrello())
    );
  }

  aggiornaQuantita(idProdotto: number, quantita: number): Observable<any> {
    const idUtente = this.getIdUtente();
    if (!idUtente) {
      throw new Error('Utente non autenticato. Effettua il login per gestire il carrello.');
    }
    
    return this.http.put(`${this.baseUrl}/aggiorna`, {
      id_utente: idUtente,
      id_prodotto: idProdotto,
      quantita: quantita
    }).pipe(
      tap(() => this.caricaCarrello())
    );
  }

  //Carica carrello guest dal localStorage o carrello utente dal backend
  private caricaCarrello(): void {
    const idUtente = this.getIdUtente();
    if (!idUtente) {
      // Se l'utente non è loggato, carica carrello guest dal localStorage
      const carrelloGuest = this.getCarrelloGuest();
      this.carrelloSubject.next(carrelloGuest);
      return;
    }
    
    this.http.get<any[]>(`${this.baseUrl}/${idUtente}`).subscribe(
      carrello => this.carrelloSubject.next(carrello),
      err => console.error('Errore caricamento carrello:', err)
    );
  }

  ottieniCarrello(): Observable<any[]> {
    return this.carrello$;
  }

  calcolaTotale(): Observable<number> {
    return this.carrello$.pipe(
      // per prezzo in caso promo fosse true
      map(carrello => carrello.reduce((total, item) => {
        const unit = item.prezzo_scontato != null ? item.prezzo_scontato : item.prezzo;
        return total + (unit * item.quantita);
      }, 0))
    );
  }

  // Metodo per ricaricare il carrello dopo il login
  ricaricaCarrello(): void {
    this.caricaCarrello();
  }

  // Metodo per svuotare il carrello al logout
  svuotaCarrello(): void {
    this.carrelloSubject.next([]);
  }

  // Metodo per aggiornare il carrello dopo un acquisto (ricarica dal backend)
  aggiornaDopoAcquisto(): void {
    this.caricaCarrello();
  }

  getCarrelloGuest(): any[] {
  const dati = localStorage.getItem('carrelloGuest');
  return dati ? JSON.parse(dati) : [];
}

setCarrelloGuest(carrello: any[]): void {
  localStorage.setItem('carrelloGuest', JSON.stringify(carrello));
  this.carrelloSubject.next(carrello); 
}

// Sincronizza il carrello guest con quello utente dopo il login
sincronizzaCarrelloGuestConUtente(): void {
  const carrelloGuest = this.getCarrelloGuest();
  if (carrelloGuest.length > 0 && this.isLoggedIn()) {
    carrelloGuest.forEach(item => {
      this.aggiungiAlCarrello(item.id_prodotto, item.quantita).subscribe(); 
    });
    
    localStorage.removeItem('carrelloGuest');
  }
}

isLoggedIn(): boolean {
  return !!this.getIdUtente();
}


}