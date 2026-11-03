import { CommonModule } from '@angular/common';
import { Component, input, signal } from '@angular/core';
import { Forecast } from '../core/contracts';

@Component({
  selector: 'app-bracket',
  imports: [CommonModule],
  templateUrl: './bracket.html',
  styleUrl: './bracket.css',
})
export class Bracket {
  readonly forecast = input.required<Forecast>();
  readonly focused = signal<number | null>(null);
}
