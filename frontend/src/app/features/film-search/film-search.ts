import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { FilmService, FilmSearchQuery } from '../../core/film.service';
import { Film } from '../../core/film.model';
import { CommonModule } from '@angular/common';
import { FilmResults } from '../film-results/film-results';

@Component({
  selector: 'app-film-search',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, FilmResults],
  templateUrl: './film-search.html',
  styleUrl: './film-search.css',
})
export class FilmSearch {
  form: FormGroup;
  results: Film[] = [];
  loading = false;
  error: string | null = null;

  constructor(private fb: FormBuilder, private filmService: FilmService) {
    this.form = this.fb.group({
      title: [''],
      country: [''],
      language: [''],
      director: [''],
      producer: [''],

      studio: [''],
      musicComposer: [''],
      distributor: [''],
      editor: [''],

      budgetMin: [0],
      budgetMax: [500000000],

      grossMin: [0],
      grossMax: [500000000],

      runtimeMin: [0],
      runtimeMax: [400],
    });
  }

  private readonly BUDGET_MIN_DEFAULT = 0;
  private readonly BUDGET_MAX_DEFAULT = 500_000_000;

  private readonly GROSS_MIN_DEFAULT = 0;
  private readonly GROSS_MAX_DEFAULT = 500_000_000;

  private readonly RUNTIME_MIN_DEFAULT = 0;
  private readonly RUNTIME_MAX_DEFAULT = 400;


  onSearch(): void {
    this.error = null;
    this.loading = true;

    const query: FilmSearchQuery = this.normalizeQuery(this.form.value);

    this.filmService.searchFilms(query).subscribe({
      next: (films) => {
        this.results = films;
        this.loading = false;
      },
      error: () => {
        this.error = 'Search failed. Check backend URL/CORS and endpoint.';
        this.loading = false;
      },
    });
  }

  onReset(): void {
    // reset() takes raw values, not arrays like ['']
    this.form.reset({
      title: '',
      country: '',
      language: '',
      director: '',
      producer: '',

      studio: '',
      musicComposer: '',
      distributor: '',
      editor: '',

      budgetMin: 0,
      budgetMax: 500000000,

      grossMin: 0,
      grossMax: 500000000,

      runtimeMin: 0,
      runtimeMax: 400,
    });

    this.results = [];
    this.error = null;
  }

  toNumber(v: string): number {
    const n = Number(v);
    return Number.isFinite(n) ? n : 0;
  }

  private normalizeQuery(raw: any): FilmSearchQuery {
    const trim = (v: any) => (typeof v === 'string' ? v.trim() : v);

    const q: FilmSearchQuery = {
      title: trim(raw.title),
      country: trim(raw.country),
      language: trim(raw.language),
      director: trim(raw.director),
      producer: trim(raw.producer),

      studio: trim(raw.studio),
      musicComposer: trim(raw.musicComposer),
      distributor: trim(raw.distributor),
      editor: trim(raw.editor),

      budgetMin: raw.budgetMin,
      budgetMax: raw.budgetMax,
      grossMin: raw.grossMin,
      grossMax: raw.grossMax,
      runtimeMin: raw.runtimeMin,
      runtimeMax: raw.runtimeMax,
    };

    // ---------- BUDGET ----------
    if (
      q.budgetMin === this.BUDGET_MIN_DEFAULT &&
      q.budgetMax === this.BUDGET_MAX_DEFAULT
    ) {
      q.budgetMin = undefined;
      q.budgetMax = undefined;
    }

    // ---------- GROSS ----------
    if (
      q.grossMin === this.GROSS_MIN_DEFAULT &&
      q.grossMax === this.GROSS_MAX_DEFAULT
    ) {
      q.grossMin = undefined;
      q.grossMax = undefined;
    }

    // ---------- RUNTIME ----------
    if (
      q.runtimeMin === this.RUNTIME_MIN_DEFAULT &&
      q.runtimeMax === this.RUNTIME_MAX_DEFAULT
    ) {
      q.runtimeMin = undefined;
      q.runtimeMax = undefined;
    }

    // ---------- SAFETY: normalize reversed ranges ----------
    const normalizeRange = (min?: number, max?: number) => {
      if (min == null || max == null) return { min, max };
      return min <= max ? { min, max } : { min: max, max: min };
    };

    let r;

    r = normalizeRange(q.budgetMin, q.budgetMax);
    q.budgetMin = r.min;
    q.budgetMax = r.max;

    r = normalizeRange(q.grossMin, q.grossMax);
    q.grossMin = r.min;
    q.grossMax = r.max;

    r = normalizeRange(q.runtimeMin, q.runtimeMax);
    q.runtimeMin = r.min;
    q.runtimeMax = r.max;

    return q;
  }

}
