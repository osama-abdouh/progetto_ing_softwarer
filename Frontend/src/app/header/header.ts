import { Component, HostListener } from '@angular/core';
import { RouterModule, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { ProductService } from '../services/product.service';
import { CatalogoService } from '../services/catalogo.service';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterModule, CommonModule, FormsModule],
  templateUrl: './header.html',
  styleUrls: ['./header.css']
})
export class Header {
  user: any;
  searchQuery: string = '';
  searchSuggestions: any[] = [];
  showSuggestions: boolean = false;
  showSearchOverlay: boolean = false;
  private searchSubject = new Subject<string>();
  
  constructor(
    public auth: AuthService, 
    private router: Router,
    private productService: ProductService,
    private catalogoService: CatalogoService
  ) {
    this.user = this.auth.getUser();
    
    // Setup search suggestions with debounce
    this.searchSubject.pipe(
      debounceTime(400), // Aumentato il debounce per ridurre lo sfarfallio
      distinctUntilChanged(),
      switchMap((query: string) => {
        if (query.length >= 2) {
          return this.catalogoService.getSearchSuggestions(query);
        } else {
          return [];
        }
      })
    ).subscribe(suggestions => {
      // Aggiorna solo se la query è ancora attuale
      if (this.searchQuery.length >= 2) {
        this.searchSuggestions = suggestions;
        this.showSuggestions = suggestions.length > 0;
      } else {
        this.searchSuggestions = [];
        this.showSuggestions = false;
      }
    });
  }
  ngOnInit() {
    this.auth.user$.subscribe(user => this.user = user);
  }
   logout() {
    this.auth.logout();
  }
 showSignInMenu = false;

  toggleSignInMenu(event: MouseEvent) {
    event.stopPropagation();
    this.showSignInMenu = !this.showSignInMenu;
  }

  @HostListener('document:click')
  onDocumentClick() {
    this.showSignInMenu = false;
    this.showSuggestions = false;
  }

  @HostListener('document:keydown.escape')
  onEsc() {
    this.showSignInMenu = false;
    this.showSuggestions = false;
    this.showSearchOverlay = false;
  }

  pulisciParametriCatalogo() {
    // Naviga al catalogo senza parametri per resettare lo stato
    this.router.navigate(['/catalogo']);
  }

  toggleSearchOverlay() {
    this.showSearchOverlay = !this.showSearchOverlay;
    if (this.showSearchOverlay) {
      // Focus sull'input quando si apre l'overlay
      setTimeout(() => {
        const searchInput = document.querySelector('.search-input-expanded') as HTMLInputElement;
        if (searchInput) {
          searchInput.focus();
        }
      }, 100);
    } else {
      // Reset della ricerca quando si chiude
      this.searchQuery = '';
      this.showSuggestions = false;
      this.searchSuggestions = [];
    }
  }

  closeSearchOverlay() {
    this.showSearchOverlay = false;
    this.searchQuery = '';
    this.showSuggestions = false;
    this.searchSuggestions = [];
  }

  onSearchInput(event: any) {
    const query = event.target.value;
    this.searchQuery = query;
    this.searchSubject.next(query);
  }

  selectSuggestion(product: any) {
    this.searchQuery = '';
    this.showSuggestions = false;
    this.showSearchOverlay = false;
    // Naviga alla pagina del prodotto specifico (route dedicata)
    this.router.navigate(['/catalogo/prodotto', product.id_prodotto]);
  }

  searchProducts() {
    if (this.searchQuery.trim()) {
      // Naviga al catalogo con il parametro di ricerca
      this.router.navigate(['/catalogo'], { 
        queryParams: { search: this.searchQuery.trim() } 
      });
      this.searchQuery = ''; // Pulisce la barra di ricerca
      this.showSuggestions = false;
      this.showSearchOverlay = false;
    }
  }

  onSearchKeyPress(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.searchProducts();
    } else if (event.key === 'Escape') {
      this.showSuggestions = false;
    }
  }

}
