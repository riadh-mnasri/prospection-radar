import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-score-badge',
  standalone: true,
  template: `
    <div class="score" [class]="scoreClass">{{ score ?? '—' }}</div>
  `,
  styles: [`
    .score {
      display: inline-flex; align-items: center; justify-content: center;
      width: 42px; height: 42px; border-radius: 50%;
      font-weight: 700; font-size: 13px;
    }
    .high { background: rgba(34,197,94,.15); color: var(--green); border: 1px solid rgba(34,197,94,.3); }
    .mid  { background: rgba(249,115,22,.15); color: var(--orange); border: 1px solid rgba(249,115,22,.3); }
    .low  { background: rgba(239,68,68,.15);  color: var(--red);    border: 1px solid rgba(239,68,68,.3); }
    .none { background: var(--surface2); color: var(--muted); border: 1px solid var(--border); }
  `]
})
export class ScoreBadgeComponent {
  @Input() score?: number | null;

  get scoreClass(): string {
    if (this.score == null) return 'none';
    if (this.score >= 70) return 'high';
    if (this.score >= 50) return 'mid';
    return 'low';
  }
}
