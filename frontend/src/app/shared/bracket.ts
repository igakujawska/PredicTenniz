import { CommonModule } from '@angular/common';
import { Component, computed, input, signal } from '@angular/core';
import { Forecast } from '../core/contracts';

@Component({
  selector: 'app-bracket',
  imports: [CommonModule],
  templateUrl: './bracket.html',
  styleUrl: './bracket.css',
})
export class Bracket {
  readonly forecast = input.required<Forecast>();
  readonly roundIndex = signal(0);
  readonly showAll = signal(false);
  readonly large = computed(() => this.forecast().rounds.length > 4);
  readonly visibleRounds = computed(() =>
    this.large() && this.roundIndex() >= 0
      ? [this.forecast().rounds[Math.min(this.roundIndex(), this.forecast().rounds.length - 1)]]
      : this.forecast().rounds,
  );
  readonly visibleChances = computed(() =>
    this.showAll() ? this.forecast().titleChances : this.forecast().titleChances.slice(0, 16),
  );
  readonly focused = signal<number | null>(null);
}
