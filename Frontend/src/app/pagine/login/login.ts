import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, RouterModule } from '@angular/router';
import { CarrelloService } from '../../services/carrello.service';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ CommonModule,ReactiveFormsModule, RouterModule ],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class Login 
{
  loginForm: FormGroup;
  showPassword = false;
  
 // Dopo il login, ricarica il carrello con i dati dell'utente loggato
  constructor(
    private fb: FormBuilder, 
    private http: HttpClient, 
    private router: Router,
    private carrelloService: CarrelloService,
    private auth: AuthService
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  
  togglePassword() {
  this.showPassword = !this.showPassword;
}


  onSubmit() {
    if (this.loginForm.valid) {
  this.http.post<any>('http://localhost:8080/api/auth/login', this.loginForm.value)
        .subscribe({
          next: (res: any) => {
           this.auth.login(res.token);
           this.carrelloService.sincronizzaCarrelloGuestConUtente();
            
            // Ricarica il carrello con i dati dell'utente loggato
            this.carrelloService.ricaricaCarrello();
            
            this.router.navigate(['/home']);
          },
          error: err => alert('Errore nel login: ' + (err.error?.message || err.error?.error || 'Errore sconosciuto'))
        });
    }
  }
}