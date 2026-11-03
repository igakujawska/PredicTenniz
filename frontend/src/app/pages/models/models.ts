import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Api, apiError } from '../../core/api';
import type { ModelInfo } from '../../core/contracts';
@Component({ imports: [CommonModule], templateUrl: './models.html' })
export class ModelsPage {
  readonly models = signal<ModelInfo[]>([]);
  readonly error = signal('');
  constructor() {
    inject(Api)
      .models()
      .pipe(takeUntilDestroyed(inject(DestroyRef)))
      .subscribe({
        next: (models) => this.models.set([...models].reverse()),
        error: (error) => this.error.set(apiError(error)),
      });
  }
  featureLabel(name: string) {
    const labels: Record<string, string> = {
      log_rank_ratio: 'Różnica logarytmów rankingów',
      elo_difference: 'Różnica Elo',
      surface_elo_difference: 'Elo na nawierzchni',
      form_90_difference: 'Forma z 90 dni',
      surface_form_180_difference: 'Forma na nawierzchni (180 dni)',
      win_rate_365_difference: 'Wyniki z 365 dni',
      experience_365_difference: 'Liczba meczów w roku',
      head_to_head_advantage: 'Historia bezpośrednich spotkań',
      best_of_five: 'Best of 5',
      clay: 'Clay',
      grass: 'Grass',
      atp_500: 'ATP 500',
      masters_1000: 'Masters 1000',
      grand_slam: 'Wielki Szlem',
      finals: 'ATP Finals',
    };
    return labels[name] || name;
  }
}
