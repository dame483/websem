import { Component, Input } from '@angular/core';
import { Film } from '../../core/film.model';
import {CommonModule} from '@angular/common';

@Component({
  selector: 'app-film-results',
  imports: [CommonModule],
  templateUrl: './film-results.html',
  styleUrl: './film-results.css',
  standalone: true
})
export class FilmResults {
  @Input() films: Film[] = [];
}

