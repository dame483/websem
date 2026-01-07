import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {FilmSearch} from './features/film-search/film-search';

@Component({
  selector: 'app-root',
  imports: [FilmSearch],
  standalone: true,
  styleUrl: './app.css',
  template: `
    <app-film-search></app-film-search>  `,
})
export class App {
  protected readonly title = signal('frontend');
}
