import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/landing/landing.component').then(m => m.LandingComponent)
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/auth.component').then(m => m.AuthComponent)
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'missions',
    canActivate: [authGuard],
    loadComponent: () => import('./features/missions/missions.component').then(m => m.MissionsComponent)
  },
  {
    path: 'signals',
    canActivate: [authGuard],
    loadComponent: () => import('./features/signals/signals.component').then(m => m.SignalsComponent)
  },
  {
    path: 'favorites',
    canActivate: [authGuard],
    loadComponent: () => import('./features/favorites/favorites.component').then(m => m.FavoritesComponent)
  },
  { path: '**', redirectTo: '' }
];
