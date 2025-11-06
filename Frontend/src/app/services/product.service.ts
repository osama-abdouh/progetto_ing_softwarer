import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private apiUrl = 'http://localhost:8080/api/products';

  constructor(private http: HttpClient) {}

  getProdotti(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  inserisciProdotto(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/insert`, data);
  }

  modificaProdotto(id: number, data: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}`, data);
  }

  bloccaProdotto(id: number, bloccato: boolean): Observable<any> {
    return this.http.patch(`${this.apiUrl}/${id}/blocco`, { bloccato });
  }
}