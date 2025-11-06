import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) { }

  // METODI ESISTENTI...
  getUtenti(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/admin/users`);
  }

  toggleBlockUser(userId: number): Observable<any> {
    return this.http.patch(`${this.baseUrl}/admin/users/${userId}/block`, {});
  }

  toggleAdminUser(userId: number): Observable<any> {
    return this.http.patch(`${this.baseUrl}/admin/users/${userId}/admin`, { makeAdmin: true });
  }

  getProdotti(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/products/load`);
  }

  // AGGIUNGI QUESTI METODI MANCANTI
  getStatisticheUtenti(): Observable<any[]> {
    console.log('Chiamata API per statistiche utenti');
    return this.http.get<any[]>(`${this.baseUrl}/admin/statistiche-utenti`);
  }

  getOrdiniUtente(userId: number): Observable<any[]> {
    console.log('Chiamata API per ordini utente ID:', userId);
    return this.http.get<any[]>(`${this.baseUrl}/admin/users/${userId}/ordini`);
  }

  getDettaglioOrdineAdmin(ordineId: number): Observable<any> {
    console.log('Chiamata API per dettaglio ordine admin ID:', ordineId);
    return this.http.get<any>(`${this.baseUrl}/admin/ordini/${ordineId}/dettaglio`);
  }

  aggiornaStatoOrdine(ordineId: number, body: any): Observable<any> {
    console.log('Chiamata API per aggiornare stato ordine ID:', ordineId, 'nuovo stato:', body.stato);
    return this.http.patch(`${this.baseUrl}/admin/ordini/${ordineId}/stato`, body);
  }


  rimuoviProdotto(prodottoId: number): Observable<any> {
    console.log('Chiamata API per rimuovere prodotto ID:', prodottoId);
    return this.http.delete(`${this.baseUrl}/admin/prodotti/${prodottoId}`);
  }


  getAllCoupon(): Observable<any[]> {
  return this.http.get<any[]>(`${this.baseUrl}/coupon`);
}

createCoupon(coupon: any): Observable<any> {
  return this.http.post(`${this.baseUrl}/coupon`, coupon);
}

updateCoupon(coupon: any): Observable<any> {
  return this.http.put(`${this.baseUrl}/coupon/${coupon.id}`, coupon);
}

deleteCoupon(id: number): Observable<any> {
  return this.http.delete(`${this.baseUrl}/coupon/${id}`);
}
}