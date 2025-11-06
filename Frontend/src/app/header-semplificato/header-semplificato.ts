import { Component, HostListener } from '@angular/core';
import { RouterModule, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { CatalogoService } from '../services/catalogo.service';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { CarrelloService } from '../services/carrello.service';
import { WishListService } from '../services/wishlist.service';


@Component({
  selector: 'app-header-semplificato',
  standalone: true,
  imports: [RouterModule, CommonModule, FormsModule],
  templateUrl: './header-semplificato.html',
  styleUrls: ['./header-semplificato.css']
})
export class HeaderSemplificato {
  user: any;
  searchQuery: string = '';
  searchSuggestions: any[] = [];
  showSuggestions = false;
  showSearchOverlay = false;
  showSignInMenu = false;
  carrelloCount = 0;  
  wishlistCount = 0;


  private searchSubject = new Subject<string>();

  constructor(
  private carrelloService: CarrelloService,
  private wishlistService: WishListService,
    public auth: AuthService,
    private router: Router,
    private catalogoService: CatalogoService
  ) {
     
     this.carrelloService.carrello$.subscribe(carrello => {
    this.carrelloCount = carrello.reduce((totale, prodotto) => totale + (prodotto.quantita || 1), 0);
  });
     this.wishlistService.wishlistCount$.subscribe(count => this.wishlistCount = count);

    this.user = this.auth.getUser();

    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      switchMap((query: string) => {
        if (query.length >= 2) return this.catalogoService.getSearchSuggestions(query);
        return [];
      })
    ).subscribe(suggestions => {
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
    this.auth.user$.subscribe(u => {
      this.user = u; 
      if (u) this.wishlistService.refreshCount(); else this.wishlistService.refreshCount();
    });
  }

  logout() { this.auth.logout(); }

  toggleSignInMenu(event: MouseEvent) { event.stopPropagation(); this.showSignInMenu = !this.showSignInMenu; }

  @HostListener('document:click') onDocumentClick() { this.showSignInMenu = false; this.showSuggestions = false; }
  

  pulisciParametriCatalogo() { this.router.navigate(['/catalogo']); }

  toggleSearchOverlay() {
    this.showSearchOverlay = !this.showSearchOverlay;
    if (this.showSearchOverlay) setTimeout(() => {
      const el = document.querySelector('.search-input-expanded') as HTMLInputElement; if (el) el.focus();
    }, 100);
    else { this.searchQuery = ''; this.showSuggestions = false; this.searchSuggestions = []; }
  }

  closeSearchOverlay() { this.showSearchOverlay = false; this.searchQuery = ''; this.showSuggestions = false; this.searchSuggestions = []; }


    onSearchInput(event: any) {
      const query = event.target.value;
      this.searchQuery = query;
      this.searchSubject.next(query);
      if (!query || query.trim().length === 0) {
        this.searchSuggestions = [];
        this.showSuggestions = false;
      }
    }

  selectSuggestion(product: any) { this.searchQuery = ''; this.showSuggestions = false; this.showSearchOverlay = false; this.router.navigate(['/catalogo/prodotto', product.id_prodotto]); }

  searchProducts() { if (this.searchQuery.trim()) { this.router.navigate(['/catalogo'], { queryParams: { search: this.searchQuery.trim() } }); this.searchQuery = ''; this.showSuggestions = false; this.showSearchOverlay = false; } }

  onSearchKeyPress(event: KeyboardEvent) { if (event.key === 'Enter') { event.preventDefault(); this.searchProducts(); } else if (event.key === 'Escape') { this.showSuggestions = false; } }
}
