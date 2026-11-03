import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api, apiError } from '../../core/api';
import type { Player } from '../../core/contracts';

@Component({ imports: [CommonModule, FormsModule, RouterLink], templateUrl: './players.html' })
export class PlayersPage {
  private readonly api = inject(Api);
  readonly players = signal<Player[]>([]);
  readonly query = signal('');
  readonly country = signal('');
  readonly error = signal('');
  readonly loading = signal(true);
  readonly date = signal<string | null>(null);
  readonly countries = computed(() =>
    [
      ...new Set(
        this.players()
          .map((player) => player.countryCode)
          .filter((value): value is string => !!value),
      ),
    ].sort(),
  );
  readonly filtered = computed(() =>
    this.players().filter(
      (player) =>
        player.name.toLocaleLowerCase('pl').includes(this.query().trim().toLocaleLowerCase('pl')) &&
        (!this.country() || player.countryCode === this.country()),
    ),
  );
  constructor() {
    this.api
      .players()
      .pipe(takeUntilDestroyed(inject(DestroyRef)))
      .subscribe({
        next: (catalog) => {
          this.players.set(catalog.players);
          this.date.set(catalog.latestRankingDate);
          this.loading.set(false);
        },
        error: (error) => {
          this.error.set(apiError(error));
          this.loading.set(false);
        },
      });
  }
}
