import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { NgIf, CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
@Component({
  selector: 'app-registrazione',
  standalone: true,
  imports: [ CommonModule, ReactiveFormsModule, RouterModule, NgIf ],
  templateUrl: './registrazione.html',
  styleUrls: ['./registrazione.css']
})
export class Registrazione {

  registerForm: FormGroup;
  showPassword = false;
  showConfirmPassword = false;
  dataMax: string = ''; // Data massima (18 anni fa)
  dataMin: string = ''; 



  constructor(private fb: FormBuilder, private http: HttpClient, private router: Router) {
     this.registerForm = this.fb.group({
      nome: ['', Validators.required],
      cognome: ['', Validators.required],
      sesso: ['', Validators.required],
      dataNascita: ['', [Validators.required, this.minAgeValidator(18)]],
      telefono: ['', [Validators.required, Validators.pattern(/^[0-9]{10,15}$/)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordsMatchValidator });
  }
  passwordsMatchValidator(form: FormGroup) {
  const password = form.get('password')?.value;
  const confirmPassword = form.get('confirmPassword')?.value;
  return password === confirmPassword ? null : { passwordMismatch: true };
}
togglePassword() {
  this.showPassword = !this.showPassword;
}
toggleConfirmPassword() {
  this.showConfirmPassword = !this.showConfirmPassword;
}

ngOnInit() {
  // Calcola data massima (18 anni fa per maggiorenni)
  const oggi = new Date();
  const annoMax = oggi.getFullYear() - 18;
  const meseMax = String(oggi.getMonth() + 1).padStart(2, '0');
  const giornoMax = String(oggi.getDate()).padStart(2, '0');
  this.dataMax = `${annoMax}-${meseMax}-${giornoMax}`;

  // Calcola data minima (es: 100 anni fa)
  const annoMin = oggi.getFullYear() - 100;
  this.dataMin = `${annoMin}-${meseMax}-${giornoMax}`;
}

   minAgeValidator(minAge: number) {
    return (control: AbstractControl) => {
      if (!control.value) return null;
      const today = new Date();
      const birthDate = new Date(control.value);
      let age = today.getFullYear() - birthDate.getFullYear();
      const m = today.getMonth() - birthDate.getMonth();
      if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) {
        age--;
      }
      return age >= minAge ? null : { minAge: true };
    };
  }

  onSubmit() {
    if (this.registerForm.valid) {
  this.http.post('http://localhost:8080/api/auth/register', this.registerForm.value)
      .subscribe({
        next: res => {
          alert('Registrazione avvenuta con successo!');
          this.registerForm.reset(); // Svuota il form
          this.router.navigate(['/login']); // Vai al login
        },
        error: err => alert('Errore nella registrazione: ' + err.error.message)
      });
  }

}
}
