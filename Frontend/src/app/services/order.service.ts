import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class OrderService {
  constructor(private http: HttpClient) {}

  getOrdiniUtente(userId: number) {
    return this.http.get<any[]>(`http://localhost:8080/api/orders/user/${userId}`);
  }

  getDettaglioOrdine(orderId: number) {
    return this.http.get<any>(`http://localhost:8080/api/orders/${orderId}`);
  }

  getTrackingOrdine(idOrdine: string) {
  return this.http.get<any>(`http://localhost:8080/api/orders/tracking/${idOrdine}`);
}
}

