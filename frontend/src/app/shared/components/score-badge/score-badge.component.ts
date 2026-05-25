import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-score-badge',
  standalone: true,
  template: `
    <div class="score-ring" [class]="cls">
      <svg viewBox="0 0 36 36" class="ring-svg">
        <circle cx="18" cy="18" r="15" class="ring-bg"/>
        <circle cx="18" cy="18" r="15" class="ring-fill"
          [style.stroke-dasharray]="dash"
          [style.stroke]="color"/>
      </svg>
      <span class="ring-val">{{ score ?? '—' }}</span>
    </div>
  `,
  styles: [`
    .score-ring {
      position: relative; width: 48px; height: 48px;
      display: flex; align-items: center; justify-content: center;
      flex-shrink: 0;
    }
    .ring-svg {
      position: absolute; inset: 0; width: 100%; height: 100%;
      transform: rotate(-90deg);
    }
    .ring-bg  { fill: none; stroke: rgba(255,255,255,.06); stroke-width: 3; }
    .ring-fill {
      fill: none; stroke-width: 3; stroke-linecap: round;
      stroke-dashoffset: 0;
      transition: stroke-dasharray .6s ease;
    }
    .ring-val {
      font-size: 12px; font-weight: 700; line-height: 1;
      position: relative; z-index: 1;
    }
    .high .ring-val { color: #22c55e; }
    .mid  .ring-val { color: #f97316; }
    .low  .ring-val { color: #ef4444; }
    .none .ring-val { color: #64748b; }
  `]
})
export class ScoreBadgeComponent {
  @Input() score?: number | null;

  get cls(): string {
    if (this.score == null) return 'none';
    if (this.score >= 70) return 'high';
    if (this.score >= 50) return 'mid';
    return 'low';
  }

  get color(): string {
    if (this.score == null) return '#2a3045';
    if (this.score >= 70) return '#22c55e';
    if (this.score >= 50) return '#f97316';
    return '#ef4444';
  }

  get dash(): string {
    const circ = 2 * Math.PI * 15;
    const fill = this.score != null ? (this.score / 100) * circ : 0;
    return `${fill} ${circ}`;
  }
}
