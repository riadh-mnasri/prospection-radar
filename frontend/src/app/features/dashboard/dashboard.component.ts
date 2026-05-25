import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { RadarService } from '../../core/services/radar.service';
import { RadarStats } from '../../core/models/mission.model';

const SOURCE_LABELS: Record<string, string> = {
  FREELANCE_COM: 'Free-Work',
  MALT: 'Hellowork',
  LINKEDIN: 'LinkedIn',
  TALENT_IO: 'Talent.io',
  MANUAL: 'Manuel'
};

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit, OnDestroy {
  stats = signal<RadarStats | null>(null);
  loading = signal(true);
  private refreshListener = () => this.load();

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
    this.radar.getStats().subscribe({
      next: s  => { this.stats.set(s); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  sourceEntries(): [string, number][] {
    const s = this.stats();
    if (!s) return [];
    return Object.entries(s.missionsBySource).sort((a, b) => b[1] - a[1]);
  }

  maxSourceCount(): number {
    const entries = this.sourceEntries();
    return entries.length ? Math.max(...entries.map(e => e[1])) : 1;
  }

  sourceLabel(key: string): string {
    return SOURCE_LABELS[key] ?? key;
  }

  gaugeArc(score: number): string {
    const total = Math.PI * 40;
    const fill = (score / 100) * total;
    return `${fill} ${total}`;
  }
}
