import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { RadarService } from '../../core/services/radar.service';
import { ToastService } from '../../core/services/toast.service';
import { Mission, MissionStatus } from '../../core/models/mission.model';
import { ScoreBadgeComponent } from '../../shared/components/score-badge/score-badge.component';
import { OutreachModalComponent } from '../../shared/components/outreach-modal/outreach-modal.component';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-mission-detail',
  standalone: true,
  imports: [RouterLink, FormsModule, ScoreBadgeComponent, OutreachModalComponent],
  templateUrl: './mission-detail.component.html',
  styleUrl: './mission-detail.component.scss'
})
export class MissionDetailComponent implements OnInit {
  mission = signal<Mission | null>(null);
  loading = signal(true);
  showOutreach = false;

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

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private radar: RadarService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.radar.getMission(id).subscribe({
      next: m  => { this.mission.set(m); this.loading.set(false); },
      error: () => { this.toast.show('Mission introuvable', 'error'); this.router.navigate(['/missions']); }
    });
  }

  updateStatus(status: MissionStatus) {
    const m = this.mission();
    if (!m) return;
    this.radar.updateMissionStatus(m.id, status).subscribe({
      next: updated => { this.mission.set(updated); this.toast.show('Statut mis à jour', 'success'); },
      error: () => this.toast.show('Erreur mise à jour', 'error')
    });
  }

  toggleFavorite() {
    const m = this.mission();
    if (!m) return;
    this.radar.toggleFavorite(m.id, !m.favorite).subscribe({
      next: updated => { this.mission.set(updated); },
      error: () => this.toast.show('Erreur', 'error')
    });
  }

  formatTjm(min?: number, max?: number): string {
    if (!min && !max) return 'TJM non précisé';
    if (!max || min === max) return `${min} €/j`;
    return `${min} – ${max} €/j`;
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

  formatDate(dt: string): string {
    return new Date(dt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
  }
}
