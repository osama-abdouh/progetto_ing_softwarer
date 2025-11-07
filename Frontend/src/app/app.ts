import { Component, signal } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs';
import { HeaderSemplificato } from './header-semplificato/header-semplificato';
import { Footer } from './footer/footer';


@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, Footer, HeaderSemplificato],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App {
  protected readonly title = signal('project');
  constructor(router: Router) {
    router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      window.scrollTo(0, 0);
    });
  }
}