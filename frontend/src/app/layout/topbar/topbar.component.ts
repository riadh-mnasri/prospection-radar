import { Component, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { forkJoin } from 'rxjs';
import { RadarService } from '../../core/services/radar.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.scss'
})
export class TopbarComponent {
  scanning  = signal(false);
  scanStep  = signal('Connexion aux sources…');
  lastUpdate = signal('');

  private readonly steps = [
    'Scraping Free-Work…', 'Scraping Hellowork…',
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
    this.lastUpdate.set(new Date().toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }));
  }

  scan() {
    this.scanning.set(true);
    let i = 0;
    this.stepTimer = setInterval(() => this.scanStep.set(this.steps[i++ % this.steps.length]), 2800);

    forkJoin([this.radar.scanMissions(), this.radar.scanSignals()]).subscribe({
      next: ([m, s]) => {
        this.stopScan();
        this.lastUpdate.set(new Date().toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }));
        this.toast.show(`${m.newMissions} nouvelles missions · ${s.newSignals} signaux`, 'success');
        window.dispatchEvent(new CustomEvent('radar:refresh'));
      },
      error: (e) => {
        this.stopScan();
        this.toast.show('Erreur scan : ' + e.message, 'error');
      }
    });
  }

  private stopScan() {
    clearInterval(this.stepTimer);
    this.scanning.set(false);
  }
}
