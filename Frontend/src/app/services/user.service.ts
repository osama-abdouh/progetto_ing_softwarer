import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class UserService {
  constructor(private http: HttpClient) {}
  private apiUrl = 'http://localhost:8080/api';
  private adminApiUrl = 'http://localhost:8080/api/admin';

  getUsers() {
    return this.http.get<any[]>(`${this.adminApiUrl}/users`);
  }

  blockUser(id: number) {
    return this.http.patch(`${this.adminApiUrl}/users/${id}/block`, {});
  }

  setAdmin(id: number, makeAdmin: boolean) {
    return this.http.patch(`${this.adminApiUrl}/users/${id}/admin`, { makeAdmin });
  }

   getUserProfile(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/profile`);
  }

  updateProfile(userData: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/profile`, userData);
  }

  changePassword(passwordData: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/profile/password`, passwordData);
  }

  // Indirizzi
  getAddresses(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/indirizzi`);
  }

  addAddress(addressData: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/indirizzi`, addressData);
  }

  updateAddress(id: number, addressData: any): Observable<any> {
  return this.http.put<any>(`${this.apiUrl}/indirizzi/${id}`, addressData);
}

  deleteAddress(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/indirizzi/${id}`);
  }

  // Wishlist
  getWishlist(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/wishlist`);
  }

  addToWishlist(prodottoId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/wishlist`, { prodotto_id: prodottoId });
  }

  removeFromWishlist(prodottoId: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/wishlist/${prodottoId}`);
  }
  setDefaultAddress(id: number): Observable<any> {
  return this.http.put<any>(`${this.apiUrl}/indirizzi/${id}/predefinito`, {});
}

changeEmail(emailData: any): Observable<any> {
  return this.http.put<any>(`${this.apiUrl}/profile/email`, emailData);
}

}