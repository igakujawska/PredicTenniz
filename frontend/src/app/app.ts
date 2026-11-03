import { Component, effect, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  readonly theme = signal<'light' | 'dark'>('light');

  constructor() {
    try {
      this.theme.set(localStorage.getItem('predictteniz-theme') === 'dark' ? 'dark' : 'light');
    } catch {
      /* Storage may be unavailable. */
    }
    effect(() => {
      document.documentElement.dataset['theme'] = this.theme();
      try {
        localStorage.setItem('predictteniz-theme', this.theme());
      } catch {
        /* The theme still works for this visit. */
      }
    });
  }

  toggleTheme() {
    this.theme.update((value) => (value === 'light' ? 'dark' : 'light'));
  }
}
