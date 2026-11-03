import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'draw', pathMatch: 'full' },
  { path: 'draw', loadComponent: () => import('./pages/draw/draw').then(page => page.DrawPage) },
  { path: 'players', loadComponent: () => import('./pages/players/players').then(page => page.PlayersPage) },
  { path: 'match', loadComponent: () => import('./pages/match/match').then(page => page.MatchPage) },
  { path: 'models', loadComponent: () => import('./pages/models/models').then(page => page.ModelsPage) },
  { path: '**', redirectTo: 'draw' },
];
