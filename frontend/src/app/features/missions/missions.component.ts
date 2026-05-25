import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RadarService } from '../../core/services/radar.service';
import { ToastService } from '../../core/services/toast.service';
import { Mission, MissionStatus } from '../../core/models/mission.model';
import { ScoreBadgeComponent } from '../../shared/components/score-badge/score-badge.component';
import { OutreachModalComponent } from '../../shared/components/outreach-modal/outreach-modal.component';

@Component({
  selector: 'app-missions',
  standalone: true,
  imports: [FormsModule, ScoreBadgeComponent, OutreachModalComponent],
  templateUrl: './missions.component.html',
  styleUrl: './missions.component.scss'
})
export class MissionsComponent implements OnInit, OnDestroy {
  missions   = signal<Mission[]>([]);
  loading    = signal(true);
  minScore   = 0;
  expandedId: number | null = null;
  outreachMission: Mission | null = null;
  private refreshListener = () => this.load();

  readonly statusLabels: Record<MissionStatus, string> = {
    NEW: 'Nouveau', ANALYZED: 'Analysé', SHORTLISTED: 'Shortlisté',
    CONTACTED: 'Contacté', REPLIED: 'Répondu',
    IN_DISCUSSION: 'En discussion', ARCHIVED: 'Archivé'
  };

  readonly statusColors: Record<MissionStatus, string> = {
    NEW: 'status-new', ANALYZED: 'status-analyzed', SHORTLISTED: 'status-shortlisted',
    CONTACTED: 'status-contacted', REPLIED: 'status-replied',
    IN_DISCUSSION: 'status-discussion', ARCHIVED: 'status-archived'
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

  toggle(id: number) {
    this.expandedId = this.expandedId === id ? null : id;
  }

  isNew(detectedAt: string): boolean {
    return Date.now() - new Date(detectedAt).getTime() < 24 * 60 * 60 * 1000;
  }

  openOutreach(mission: Mission, event: MouseEvent) {
    event.stopPropagation();
    this.outreachMission = mission;
  }

  archive(mission: Mission, event: MouseEvent) {
    event.stopPropagation();
    this.radar.updateMissionStatus(mission.id, 'ARCHIVED').subscribe({
      next: updated => {
        this.missions.update(list => list.map(m => m.id === updated.id ? updated : m));
        this.toast.show('Mission archivée', 'success');
      },
      error: () => this.toast.show('Erreur', 'error')
    });
  }

  updateStatus(mission: Mission, status: MissionStatus) {
    this.radar.updateMissionStatus(mission.id, status).subscribe({
      next: updated => {
        this.missions.update(list => list.map(m => m.id === updated.id ? updated : m));
        this.toast.show('Statut mis à jour', 'success');
      },
      error: () => this.toast.show('Erreur mise à jour', 'error')
    });
  }

  formatTjm(min?: number, max?: number): string {
    if (!min && !max) return '';
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
      MALT: 'Hellowork', FREELANCE_COM: 'Free-Work',
      LINKEDIN: 'LinkedIn', TALENT_IO: 'Talent.io', MANUAL: 'Manuel'
    };
    return map[source] ?? source;
  }
}
