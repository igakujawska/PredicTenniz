import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'draw', pathMatch: 'full' },
  { path: 'draw', loadComponent: () => import('./pages/draw/draw').then(page => page.DrawPage) },
  { path: '**', redirectTo: 'draw' },
];
