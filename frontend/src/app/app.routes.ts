import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'missions',
    loadComponent: () => import('./features/missions/missions.component').then(m => m.MissionsComponent)
  },
  {
    path: 'signals',
    loadComponent: () => import('./features/signals/signals.component').then(m => m.SignalsComponent)
  }
];
