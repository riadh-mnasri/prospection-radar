import { Component, Input, Output, EventEmitter, signal } from '@angular/core';
import { RadarService } from '../../../core/services/radar.service';
import { Mission } from '../../../core/models/mission.model';
import { ToastService } from '../../../core/services/toast.service';

export interface OutreachMessage {
  linkedinMessage: string;
  emailSubject: string;
  emailBody: string;
}

@Component({
  selector: 'app-outreach-modal',
  standalone: true,
  templateUrl: './outreach-modal.component.html',
  styleUrl: './outreach-modal.component.scss'
})
export class OutreachModalComponent {
  @Input() mission!: Mission;
  @Output() close = new EventEmitter<void>();

  loading = signal(true);
  outreach = signal<OutreachMessage | null>(null);
  activeTab = signal<'linkedin' | 'email'>('linkedin');
  copied = signal(false);

  constructor(private radar: RadarService, private toast: ToastService) {}

  ngOnInit() {
    this.radar.generateOutreach(this.mission.id).subscribe({
      next: msg => { this.outreach.set(msg); this.loading.set(false); },
      error: () => {
        this.toast.show('Erreur génération message', 'error');
        this.close.emit();
      }
    });
  }

  setTab(tab: 'linkedin' | 'email') {
    this.activeTab.set(tab);
    this.copied.set(false);
  }

  copy() {
    const msg = this.outreach();
    if (!msg) return;
    const text = this.activeTab() === 'linkedin'
      ? msg.linkedinMessage
      : `Objet : ${msg.emailSubject}\n\n${msg.emailBody}`;
    navigator.clipboard.writeText(text).then(() => {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    });
  }

  onBackdrop(e: MouseEvent) {
    if ((e.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.close.emit();
    }
  }
}
