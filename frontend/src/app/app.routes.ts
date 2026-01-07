import { Routes } from '@angular/router';
import {FilmSearch} from './features/film-search/film-search';

export const routes: Routes = [
  {
    path: '',
    component: FilmSearch,
  },

  {
    path: '**',
    redirectTo: '',
  },
];
