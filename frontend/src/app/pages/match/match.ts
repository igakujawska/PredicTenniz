import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { catchError, finalize, forkJoin, of, Subject, switchMap, debounceTime } from 'rxjs';
import { Api, apiError } from '../../core/api';
import type { Match, ModelInfo, Player, Surface } from '../../core/contracts';
interface Comparison {
  model: ModelInfo;
  match: Match | null;
  error: string;
}
@Component({ imports: [CommonModule, FormsModule], templateUrl: './match.html' })
export class MatchPage {
  private readonly api = inject(Api);
  private readonly destroyRef = inject(DestroyRef);
  private readonly changes = new Subject<void>();
  readonly players = signal<Player[]>([]);
  readonly models = signal<ModelInfo[]>([]);
  readonly results = signal<Comparison[]>([]);
  readonly error = signal('');
  readonly loading = signal(false);
  a = 0;
  b = 0;
  asOf = '';
  surface: Surface = 'HARD';
  bestOf = 3;
  constructor() {
    this.changes
      .pipe(
        debounceTime(150),
        switchMap(() => {
          this.results.set([]);
          if (!this.a || !this.b || this.a === this.b) {
            this.error.set('Wybierz dwóch różnych zawodników.');
            return of([] as Comparison[]);
          }
          this.error.set('');
          this.loading.set(true);
          return forkJoin(
            this.models().map((model) =>
              this.api
                .preview({
                  title: 'Porównanie meczu',
                  asOf: this.asOf,
                  playerIds: [Number(this.a), Number(this.b)],
                  surface: this.surface,
                  bestOf: this.bestOf,
                  modelVersion: model.version,
                })
                .pipe(
                  switchMap((draw) =>
                    of({
                      model,
                      match: draw.forecast?.rounds[0].matches[0] || null,
                      error: draw.predictionError || '',
                    }),
                  ),
                  catchError((error) => of({ model, match: null, error: apiError(error) })),
                ),
            ),
          ).pipe(finalize(() => this.loading.set(false)));
        }),
        takeUntilDestroyed(),
      )
      .subscribe((results) => this.results.set(results));
    const first = Number(inject(ActivatedRoute).snapshot.queryParamMap.get('a'));
    forkJoin({ catalog: this.api.players(), models: this.api.models() })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ catalog, models }) => {
          this.players.set(catalog.players);
          this.models.set(models);
          this.asOf = catalog.latestRankingDate || '';
          this.a =
            catalog.players.find((player) => player.id === first)?.id ||
            catalog.players[0]?.id ||
            0;
          this.b = catalog.players.find((player) => player.id !== this.a)?.id || 0;
          this.changed();
        },
        error: (error) => this.error.set(apiError(error)),
      });
  }
  changed() {
    this.changes.next();
  }
  swap() {
    [this.a, this.b] = [this.b, this.a];
    this.changed();
  }
}
