import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RadarService } from '../../core/services/radar.service';
import { ToastService } from '../../core/services/toast.service';
import { Mission, MissionStatus } from '../../core/models/mission.model';
import { ScoreBadgeComponent } from '../../shared/components/score-badge/score-badge.component';

@Component({
  selector: 'app-missions',
  standalone: true,
  imports: [FormsModule, ScoreBadgeComponent],
  templateUrl: './missions.component.html',
  styleUrl: './missions.component.scss'
})
export class MissionsComponent implements OnInit, OnDestroy {
  missions  = signal<Mission[]>([]);
  loading   = signal(true);
  minScore  = 0; // propriété simple pour [(ngModel)] — signal() incompatible avec two-way binding
  private refreshListener = () => this.load();

  readonly statusLabels: Record<MissionStatus, string> = {
    NEW: 'Nouveau', ANALYZED: 'Analysé', SHORTLISTED: 'Shortlisté',
    CONTACTED: 'Contacté', REPLIED: 'Répondu',
    IN_DISCUSSION: 'En discussion', ARCHIVED: 'Archivé'
  };

  readonly statuses = Object.keys(this.statusLabels) as MissionStatus[];

  constructor(private radar: RadarService, private toast: ToastService) {}

  ngOnInit() {
    this.load();
    window.addEventListener('radar:refresh', this.refreshListener);
  }

  ngOnDestroy() {
    window.removeEventListener('radar:refresh', this.refreshListener);
  }

  load() {
    this.loading.set(true);
    this.radar.getMissions(this.minScore).subscribe({
      next: m  => { this.missions.set(m); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  updateStatus(mission: Mission, status: MissionStatus) {
    this.radar.updateMissionStatus(mission.id, status).subscribe({
      next: updated => {
        this.missions.update(list =>
          list.map(m => m.id === updated.id ? updated : m)
        );
        this.toast.show('Statut mis à jour', 'success');
      },
      error: () => this.toast.show('Erreur mise à jour', 'error')
    });
  }

  formatTjm(min?: number, max?: number): string {
    if (!min && !max) return '—';
    if (!max || min === max) return `${min}€/j`;
    return `${min}–${max}€/j`;
  }

  sourceBadgeClass(source: string): string {
    const map: Record<string, string> = {
      MALT: 'badge-malt', FREELANCE_COM: 'badge-freelance',
      LINKEDIN: 'badge-linkedin', MANUAL: 'badge-manual'
    };
    return map[source] ?? 'badge-manual';
  }

  sourceLabel(source: string): string {
    const map: Record<string, string> = {
      MALT: 'Malt', FREELANCE_COM: 'Freelance.com',
      LINKEDIN: 'LinkedIn', TALENT_IO: 'Talent.io', MANUAL: 'Manuel'
    };
    return map[source] ?? source;
  }
}
