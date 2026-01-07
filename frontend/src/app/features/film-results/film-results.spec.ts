import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FilmResults } from './film-results';

describe('FilmResults', () => {
  let component: FilmResults;
  let fixture: ComponentFixture<FilmResults>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FilmResults]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FilmResults);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
