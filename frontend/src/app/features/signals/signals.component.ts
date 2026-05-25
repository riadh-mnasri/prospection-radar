import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RadarService } from '../../core/services/radar.service';
import { Signal, SignalType } from '../../core/models/signal.model';

@Component({
  selector: 'app-signals',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './signals.component.html',
  styleUrl: './signals.component.scss'
})
export class SignalsComponent implements OnInit, OnDestroy {
  signals  = signal<Signal[]>([]);
  loading  = signal(true);
  minScore = signal(0);
  private refreshListener = () => this.load();

  readonly typeLabels: Record<SignalType, string> = {
    FUNDING:           '💰 Levée de fonds',
    CTO_NOMINATION:    '👤 Nomination CTO/VP',
    TECH_HIRING_SPREE: '📈 Recrutement massif',
    JOB_STILL_OPEN:    '⏳ Poste ouvert +30j',
    FORMER_COLLEAGUE:  '🤝 Ancien collègue promu',
    APPEL_OFFRES:      '📋 Appel d\'offres'
  };

  readonly typeBadgeClass: Record<SignalType, string> = {
    FUNDING:           'badge-funding',
    CTO_NOMINATION:    'badge-nomination',
    TECH_HIRING_SPREE: 'badge-signal',
    JOB_STILL_OPEN:    'badge-signal',
    FORMER_COLLEAGUE:  'badge-warm',
    APPEL_OFFRES:      'badge-signal'
  };

  constructor(private radar: RadarService) {}

  ngOnInit() {
    this.load();
    window.addEventListener('radar:refresh', this.refreshListener);
  }

  ngOnDestroy() {
    window.removeEventListener('radar:refresh', this.refreshListener);
  }

  load() {
    this.loading.set(true);
    this.radar.getSignals(this.minScore()).subscribe({
      next: s  => { this.signals.set(s); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  scoreColor(score?: number): string {
    if (!score) return 'var(--muted)';
    if (score >= 70) return 'var(--green)';
    if (score >= 50) return 'var(--orange)';
    return 'var(--red)';
  }
}
