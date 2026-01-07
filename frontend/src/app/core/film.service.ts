import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Film } from './film.model';

export interface FilmSearchQuery {
  title?: string;
  country?: string;
  language?: string;
  director?: string;
  producer?: string;

  studio?: string;
  musicComposer?: string;
  distributor?: string;
  editor?: string;

  budgetMin?: number;
  budgetMax?: number;
  grossMin?: number;
  grossMax?: number;
  runtimeMin?: number;
  runtimeMax?: number;
}

@Injectable({ providedIn: 'root' })
export class FilmService {
  private readonly baseUrl = 'http://localhost:8080/api/films';

  constructor(private http: HttpClient) {}

  searchFilms(query: FilmSearchQuery): Observable<Film[]> {
    let params = new HttpParams();

    for (const [key, value] of Object.entries(query)) {
      if (value === null || value === undefined) continue;

      if (typeof value === 'string') {
        const v = value.trim();
        if (!v) continue;
        params = params.set(key, v);
      } else if (typeof value === 'number' && Number.isFinite(value)) {
        params = params.set(key, String(value));
      }
    }

    return this.http.get<Film[]>(this.baseUrl, { params });
  }

}
