import { Component, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { forkJoin } from 'rxjs';
import { RadarService } from '../../core/services/radar.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="topbar">
      <div class="topbar-left">
        <div class="status-dot" [class.offline]="offline()"></div>
        <span class="logo">Prospection <span class="logo-accent">Radar</span></span>
        <nav class="nav">
          <a routerLink="/dashboard" routerLinkActive="active">Dashboard</a>
          <a routerLink="/missions"  routerLinkActive="active">Missions</a>
          <a routerLink="/signals"   routerLinkActive="active">Signaux</a>
        </nav>
      </div>
      <div class="topbar-right">
        <span class="last-update">{{ lastUpdate() }}</span>
        <button class="btn btn-secondary btn-sm" (click)="refresh()">↻</button>
        <button class="btn btn-primary" [disabled]="scanning()" (click)="scan()">
          @if (scanning()) {
            <span class="spinner"></span> Scan…
          } @else {
            ⚡ Scanner
          }
        </button>
      </div>
    </header>

    @if (scanning()) {
      <div class="scan-bar">
        <div class="scan-bar-fill"></div>
        <span class="scan-label">{{ scanStep() }}</span>
      </div>
    }
  `,
  styleUrl: './topbar.component.scss'
})
export class TopbarComponent {
  scanning = signal(false);
  offline  = signal(false);
  lastUpdate = signal('Jamais scanné');
  scanStep   = signal('Connexion aux sources…');

  private readonly steps = [
    'Scraping Malt…', 'Scraping Freelance.com…',
    'Analyse Claude AI…', 'Calcul des scores…',
    'Détection signaux…', 'Finalisation…'
  ];
  private stepTimer: any;

  constructor(
    private radar: RadarService,
    private toast: ToastService,
    private router: Router
  ) {}

  refresh() {
    window.dispatchEvent(new CustomEvent('radar:refresh'));
    this.lastUpdate.set('Actualisé à ' + new Date().toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }));
  }

  scan() {
    this.scanning.set(true);
    let i = 0;
    this.stepTimer = setInterval(() => this.scanStep.set(this.steps[i++ % this.steps.length]), 3000);

    forkJoin([this.radar.scanMissions(), this.radar.scanSignals()]).subscribe({
      next: ([m, s]) => {
        this.stopScan();
        this.toast.show(`✅ ${m.newMissions} nouvelles missions · ${s.newSignals} signaux`, 'success');
        this.lastUpdate.set('Scanné à ' + new Date().toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }));
        window.dispatchEvent(new CustomEvent('radar:refresh'));
      },
      error: (e) => {
        this.stopScan();
        this.toast.show('❌ Erreur scan : ' + e.message, 'error');
      }
    });
  }

  private stopScan() {
    clearInterval(this.stepTimer);
    this.scanning.set(false);
  }
}
