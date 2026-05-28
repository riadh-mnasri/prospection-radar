import { Component, Output, EventEmitter, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RadarService } from '../../../core/services/radar.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-import-mission-modal',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './import-mission-modal.component.html',
  styleUrl: './import-mission-modal.component.scss'
})
export class ImportMissionModalComponent {
  @Output() close = new EventEmitter<void>();

  loading = signal(false);

  title = '';
  description = '';
  company = '';
  location = '';
  remote = false;
  tjmMin: number | null = null;
  tjmMax: number | null = null;
  duration = '';
  skillsRaw = '';
  url = '';

  constructor(
    private radar: RadarService,
    private toast: ToastService,
    private router: Router
  ) {}

  submit() {
    if (!this.title.trim()) return;
    this.loading.set(true);

    const req = {
      title: this.title.trim(),
      description: this.description.trim() || undefined,
      company: this.company.trim() || undefined,
      location: this.location.trim() || undefined,
      remote: this.remote,
      tjmMin: this.tjmMin || undefined,
      tjmMax: this.tjmMax || undefined,
      duration: this.duration.trim() || undefined,
      skills: this.skillsRaw.split(',').map(s => s.trim()).filter(Boolean),
      url: this.url.trim() || undefined
    };

    this.radar.importMission(req).subscribe({
      next: mission => {
        this.toast.show('Mission importée et analysée !', 'success');
        this.close.emit();
        this.router.navigate(['/missions', mission.id]);
      },
      error: () => {
        this.toast.show('Erreur lors de l\'import', 'error');
        this.loading.set(false);
      }
    });
  }

  onBackdrop(e: MouseEvent) {
    if ((e.target as HTMLElement).classList.contains('modal-backdrop')) this.close.emit();
  }
}
