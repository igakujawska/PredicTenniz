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
        next: (models) => this.models.set(models),
        error: (error) => this.error.set(apiError(error)),
      });
  }
}
