import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface CouponResponse {
  valido: boolean;
  messaggio: string;
  coupon?: any;
  sconto?: number;
  totale_originale?: number;
  totale_scontato?: number;
}
const privateurl = 'http://localhost:8080/api';
@Injectable({ providedIn: 'root' })
export class CouponService {
  constructor(private http: HttpClient) {}

  verificaCoupon(codice: string, totaleCarrello: number) {
    return this.http.post<CouponResponse>(`${privateurl}/coupon/verifica`, { 
      codice, 
      totale_carrello: totaleCarrello 
    });
  }

  usaCoupon(couponId: number) {
    return this.http.post(`${privateurl}/coupon/usa`, { coupon_id: couponId });
  }
}