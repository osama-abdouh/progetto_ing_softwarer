import { Component } from '@angular/core';
import { CommonModule, NgIf, NgFor } from '@angular/common'; 
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { CarrelloService } from '../../services/carrello.service';
import { CatalogoService } from '../../services/catalogo.service';
import { SuggestedService } from '../../services/suggested.service';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [CommonModule, NgIf, NgFor, FormsModule],
  templateUrl: './catalogo.html',
  styleUrls: ['./catalogo.css']
})
export class Catalogo {
  arrivoDaHome: boolean = false;
  categorie: any[] = [];
  prodotti: any[] = [];
  prodottoSelezionato: any = null;
  private handledByPathId: boolean = false;
  
  // Gestione stati, variabili booleane per gestire quale sezione mostrare nella pagina.
  mostraCategorie: boolean = true;
  mostraProdotti: boolean = false;
  mostraDettaglio: boolean = false;
  categoriaSelezionata: string = '';
  marcaSelezionata: string = '';
  marcheDisponibili: string[] = [];
  caricamento: boolean = true;
  
  // Nuove proprietà per la ricerca
  searchQuery: string = '';
  isSearchMode: boolean = false;
  
  constructor(private http: HttpClient, private carrelloService: CarrelloService, private route: ActivatedRoute, private router: Router, private catalogoService: CatalogoService, private suggestedService: SuggestedService) {
    // Gestione path param: /catalogo/prodotto/:id
    this.route.params.subscribe(params => {
      const id = params['id'];
      if (id) {
        this.handledByPathId = true;
        this.arrivoDaHome = true;
        const numId = isNaN(Number(id)) ? id : Number(id);
        this.caricaProdottoDettaglio(numId as any);
      }
    });
    // Gestione query param (fallback legacy): /catalogo?prodottoId=ID
    this.route.queryParams.subscribe(params => { //params è un oggetto che contiene i parametri della query string (dopo il "?" es. ?prodottoId=42 diventa params['prodottoId'] = 42)
      if (this.handledByPathId) {
        return; // già gestito da path param
      }
      if (params['search']) { //se dentro params cè un parametro chiamato "search" (compare quando usi barra di ricerca)
        this.searchQuery = params['search'];
        this.isSearchMode = true;
        this.eseguiRicerca(params['search']); // fa una richiesta al backend per trovare i prodotti che corrispondono alla ricerca e li mostra nella pagina.
      } else if (params['prodottoId']) {
        this.arrivoDaHome = true;
        const id = params['prodottoId'];
        const numId = isNaN(Number(id)) ? id : Number(id);
        this.caricaProdottoDettaglio(numId as any);
      } else {
        // Reset completo dello stato quando non ci sono parametri
        this.resetStato();
        this.arrivoDaHome = false;
        this.caricaCategorie(); //carica tutte le categorie di prodotti dal backend
      }
    });
  }
  caricaProdottoDettaglio(id: number | string) { //da barra di ricerca entra dentro i prodotti
    console.debug('Carico dettaglio prodotto ID:', id);
    const url = `http://localhost:8080/api/catalogo/prodotto/${id}`;
    this.http.get<any>(url).subscribe({
      next: (prodotto) => this.visualizzaDettaglio(prodotto, id),
      error: (err) => {
        console.error('Errore caricamento prodotto:', err);
        alert('Impossibile caricare il dettaglio del prodotto selezionato.');
        this.caricaCategorie();
      }
    });
  }

  private visualizzaDettaglio(prodotto: any, id: number | string) {
    if (prodotto) {
      this.prodottoSelezionato = prodotto;
      this.mostraCategorie = false;
      this.mostraProdotti = false;
      this.mostraDettaglio = true;
      
      // Salva visualizzazione (il service controlla se l'utente è loggato)
      this.suggestedService.salvaVisualizzazione(Number(id)).subscribe({
        next: () => {},
        error: (err) => console.error('Errore salvataggio visualizzazione:', err)
      });
    } else {
      console.warn('Prodotto non trovato per ID:', id);
      this.caricaCategorie();
    }
  }
  
  aggiungiAWishlist(prodotto: any): void {

  this.catalogoService.aggiungiAWishlist(prodotto);

}

//carica le categorie dal backend
  caricaCategorie() {
    this.caricamento = true;  //footer flash
  this.http.get<any[]>('http://localhost:8080/api/catalogo/prodotti').subscribe(
      dati => { 
        this.categorie = dati; //quando arrivano i dati li salva in this.categorie
        this.caricamento = false;
      },
      err => {
        console.error('Errore caricamento categorie:', err);
        this.caricamento = false;
      }
    );
  }
  
  selezionaCategoria(nomeCategoria: string) {
    this.categoriaSelezionata = nomeCategoria;
    this.mostraCategorie = false;
    this.mostraProdotti = true;
    this.mostraDettaglio = false;
    this.caricaProdottiCategoria(nomeCategoria);
  }

  //carica tutti i prodotti appartenenti ad una categoria specifica (usa una get al backend)
  caricaProdottiCategoria(categoria: string) {  
    this.caricamento = true; //bug fix per caricamento footer flash
  this.http.get<any[]>(`http://localhost:8080/api/catalogo/prodotti/categoria/${categoria}`).subscribe(
      dati => {
        this.prodotti = dati; //Salva nell’array prodotti tutti i prodotti restituiti dal backend per quella categoria.
        // Estrai marche disponibili dai prodotti
        this.marcheDisponibili = Array.from(new Set(dati.map(p => p.marchio).filter(m => !!m))); //prende da ogni prodotto solo il marchio rimuovendo eventuali valori nulli o undefined
        this.marcaSelezionata = ''; // Reset filtro marca
        this.caricamento = false;
      },
      err => {
        console.error('Errore caricamento prodotti:', err);
        this.caricamento = false;
      }
    );
  }

  eseguiRicerca(query: string) {
    this.caricamento = true;
    this.isSearchMode = true;
    this.mostraCategorie = false;
    this.mostraProdotti = true;
    this.mostraDettaglio = false;
    this.categoriaSelezionata = `Risultati per: "${query}"`;

    // Usa il backend Java per la ricerca prodotti
  this.http.get<any[]>(`http://localhost:8080/api/catalogo/prodotti/ricerca?q=${encodeURIComponent(query)}`).subscribe( //encodeURIComponent(query): serve per avere url sicuri (es. non avere spazi)
      //Salva nell'array prodotti tutti i prodotti restituiti dalla ricerca (quelli che corrispondono alla query).
      prodotti => {  
        this.prodotti = prodotti;
        // Estrai marche disponibili dai risultati
        this.marcheDisponibili = Array.from(new Set(this.prodotti.map(p => p.marchio).filter(m => !!m)));
        this.marcaSelezionata = '';
        this.caricamento = false;
      },
      err => {
        console.error('Errore ricerca prodotti:', err);
        this.caricamento = false;
      }
    );
  }

  //non serve rifare una get in questo caso,basta prendere l’oggetto prodotto già presente e mostrarlo.
  selezionaProdotto(prodotto: any) {
    //in caricaProdottiDettaglio ho messo : this.prodottoSelezionato = prodotto;
    this.prodottoSelezionato = prodotto; //mostra solo il dettaglio del prodotto selezionato
    this.mostraCategorie = false;
    this.mostraProdotti = false;
    this.mostraDettaglio = true;
    this.suggestedService.salvaVisualizzazione(this.prodottoSelezionato.id_prodotto).subscribe({
          next: () => {},
          error: (err) => console.error('Errore salvataggio visualizzazione:', err)
        });
    
  }
  
  tornaProdotti() {
    this.mostraCategorie = false;
    this.mostraProdotti = true;
    this.mostraDettaglio = false;
    this.prodottoSelezionato = null;
  }
  
  tornaAllaHome() {
    this.router.navigate(['/']);
  }
  
  tornaAlleCategorie() {
    // Se siamo in modalità ricerca, pulisci i parametri URL e torna alle categorie
    if (this.isSearchMode) {
      this.router.navigate(['/catalogo']).then(() => {
        this.resetStato();
        this.caricaCategorie();
      });
      return;
    }
    this.resetStato();
    this.caricaCategorie();
  }

  resetStato() {
    this.mostraCategorie = true;
    this.mostraProdotti = false;
    this.mostraDettaglio = false;
    this.prodotti = [];
    this.prodottoSelezionato = null;
    this.categoriaSelezionata = '';
    this.marcaSelezionata = '';
    this.marcheDisponibili = [];
    this.arrivoDaHome = false;
    this.searchQuery = '';
    this.isSearchMode = false;
  }
  
  get prodottiFiltrati() {
    if (!this.marcaSelezionata) {
      return this.prodotti;
    }
    return this.prodotti.filter(p => p.marchio === this.marcaSelezionata);
  }

  aggiungiAlCarrello(prodotto: any): void { //prodotto contiene tutte le info del prodotto selezionato (vedi caricaProdottiCategoria)
    // Controlla disponibilità prima di aggiungere
    if (prodotto.quantita_disponibile <= 0) {
      alert('Prodotto non disponibile!');
      return;
    }
    if (!this.carrelloService.isLoggedIn()) {
    // Guest: salva nel localStorage
    let carrello = this.carrelloService.getCarrelloGuest();
    const esiste = carrello.find(item => item.id_prodotto === prodotto.id_prodotto);
    if (esiste) {
      esiste.quantita += 1;
    } else {
      carrello.push({ id_prodotto: prodotto.id_prodotto, quantita: 1, ...prodotto });
    }
    this.carrelloService.setCarrelloGuest(carrello);
    alert('Prodotto aggiunto al carrello!');
    return;
  }
    
    this.carrelloService.aggiungiAlCarrello(prodotto.id_prodotto, 1).subscribe({ //aggungi 1 unità del prodotto al carrello
      next: () => {
        alert('Prodotto aggiunto al carrello!');
      },
      error: (err) => {
        console.error('Errore:', err);
        alert('Errore nell\'aggiungere il prodotto al carrello');
      }
    });
  }
}
