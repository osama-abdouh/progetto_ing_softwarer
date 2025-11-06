import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { RouterModule, Router } from '@angular/router'; 
import { AuthService } from '../../services/auth.service';
import { CatalogoService } from '../../services/catalogo.service';
import { PacchettiService, Pacchetto } from '../../services/pacchetti.service';
import { CarrelloService } from '../../services/carrello.service';
import { SuggestedService } from '../../services/suggested.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [ CommonModule, RouterModule],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class Home implements OnInit {

    user: any;
    showWelcome = false;
    prodottiInEvidenza: any[] = [];
    prodottiInVetrina: any[] = [];
    pacchetti: Pacchetto[] = [];
    loading = true;
    loadingVetrina = true;
    loadingPacchetti = true;
    error = '';
    errorVetrina = '';
    errorPacchetti = '';
    prodottiSuggeriti: any[] = [];
    isLoggedIn: boolean = false;

  constructor(
    public auth: AuthService,
    private catalogoService: CatalogoService,
    private pacchettiService: PacchettiService,
    private carrelloService: CarrelloService,
    private router: Router,
    private suggestedService: SuggestedService
  ) {
    this.user = this.auth.getUser();
  }
  vaiAlDettaglioProdotto(prodotto: any) {
    // Support multiple possible id field names from different endpoints
    const rawId = prodotto?.id_prodotto ?? prodotto?.id ?? prodotto?.prodotto_id;
    const idNum = Number(rawId);
    if (!rawId || !Number.isFinite(idNum)) {
      console.warn('Impossibile determinare l\'ID del prodotto per la navigazione:', prodotto);
      // Fallback: apri il catalogo senza dettaglio specifico
      this.router.navigate(['/catalogo']);
      return;
    }
    // Prefer the explicit detail route; Catalogo component supports both path and query param
    this.router.navigate(['/catalogo/prodotto', idNum]);
  }

  vaiANovita(articolo: string) {
    this.router.navigate(['/novita', articolo]);
  }

  vaiADettaglioPacchetto(pacchetto: Pacchetto) { //Effetto funzionale: dal frontend ora si aggiunge un singolo item “pacchetto” al carrello (con quantità 1), invece di inserire i prodotti singoli nel carrello.
    // Ora aggiungiamo il pacchetto come singolo item nel carrello
    if (!this.auth.isLoggedIn()) {
    this.router.navigate(['/login']);
    alert('Devi essere loggato per aggiungere un pacchetto al carrello.');
    return;
  }
    const quantitaPacchetto = 1;
    this.carrelloService.aggiungiPacchettoAlCarrello(pacchetto.id_pacchetto, quantitaPacchetto).subscribe({
      next: () => {
        alert(`Pacchetto "${pacchetto.nome}" aggiunto al carrello.`);
      },
      error: (err) => {
        console.error('Errore aggiunta pacchetto al carrello:', err);
        alert('Errore nell\'aggiunta del pacchetto al carrello.');
      }
    });
  }

  // Naviga alla pagina di dettaglio del pacchetto (offerte)
  apriDettaglioPacchetto(pacchetto: Pacchetto) {
    this.router.navigate(['/offerte', pacchetto.id_pacchetto]);
  }
  
  ngOnInit() {

    this.isLoggedIn = this.auth.isLoggedIn();

if (this.isLoggedIn) {
    this.loading = true;
    this.suggestedService.getProdottiSuggeriti().subscribe({
      next: prodotti => {
        this.prodottiSuggeriti = prodotti;
        this.loading = false;
      },
      error: err => {
        console.error('Errore suggeriti:', err);
        this.loading = false;
      }
    });
}
  
    // Carica i prodotti più visualizzati dal database
    this.loadProdottiPopular();
    // Carica i prodotti selezionati per la vetrina dall'admin
    this.loadProdottiVetrina();
    
    // Carica i pacchetti tematici dal database
    this.loadPacchetti();
  }

  loadProdottiVetrina() {
    this.loadingVetrina = true;
    this.catalogoService.getProdottiVetrina().subscribe({
      next: (prodotti) => {
        this.prodottiInVetrina = prodotti;
        this.loadingVetrina = false;
      },
      error: (err) => {
        console.error('Errore nel caricamento prodotti in vetrina:', err);
        this.errorVetrina = 'Errore nel caricamento dei prodotti in vetrina';
        this.loadingVetrina = false;
        this.prodottiInVetrina = [];
      }
    });
  }

  loadProdottiPopular() {
    this.loading = true;
  this.catalogoService.getProdottiPopular().subscribe({ // Prende 3 prodotti più aquistati da backend
      next: (prodotti) => {
        this.prodottiInEvidenza = prodotti;
        this.loading = false;
      },
      error: (err) => {
        console.error('Errore nel caricamento prodotti:', err);
        this.error = 'Errore nel caricamento dei prodotti';
        this.loading = false;   
      }
    });
  }

  loadPacchetti() {
    this.loadingPacchetti = true;
    this.pacchettiService.getPacchetti().subscribe({
      next: (pacchetti) => {
        this.pacchetti = pacchetti;
        this.loadingPacchetti = false;
      },
      error: (err) => {
        console.error('Errore nel caricamento pacchetti:', err);
        this.errorPacchetti = 'Errore nel caricamento delle offerte speciali';
        this.loadingPacchetti = false;
        this.pacchetti = [];
      }
    });
  }

  
}

