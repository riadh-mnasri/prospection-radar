import { Component } from '@angular/core';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  template: `
    <div class="toast-container">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="toast" [class]="'toast toast-' + toast.type" (click)="toastService.remove(toast.id)">
          <span class="toast-icon">
            @if (toast.type === 'success') { ✓ }
            @else if (toast.type === 'error') { ✕ }
            @else { ● }
          </span>
          <span class="toast-msg">{{ toast.message }}</span>
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed; bottom: 28px; right: 28px;
      z-index: 9999; display: flex; flex-direction: column; gap: 8px;
      pointer-events: none;
    }
    .toast {
      display: flex; align-items: center; gap: 10px;
      background: var(--surface2);
      border: 1px solid var(--border2);
      border-radius: 12px; padding: 13px 18px;
      font-size: 13px; font-weight: 500;
      min-width: 280px; max-width: 400px;
      cursor: pointer; pointer-events: all;
      box-shadow: 0 8px 32px rgba(0,0,0,.5), 0 0 0 1px rgba(255,255,255,.04);
      animation: toastIn .22s cubic-bezier(.34,1.56,.64,1);
    }
    .toast-icon {
      width: 22px; height: 22px; border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-size: 11px; font-weight: 700; flex-shrink: 0;
    }
    .toast-success .toast-icon { background: rgba(34,197,94,.15); color: #22c55e; }
    .toast-error   .toast-icon { background: rgba(239,68,68,.15);  color: #ef4444; }
    .toast-info    .toast-icon { background: rgba(124,58,237,.15); color: #a78bfa; }
    .toast-success { border-left: 3px solid #22c55e; }
    .toast-error   { border-left: 3px solid #ef4444; }
    .toast-info    { border-left: 3px solid #7c3aed; }
    .toast-msg { flex: 1; }

    @keyframes toastIn {
      from { transform: translateX(20px) scale(.96); opacity: 0; }
      to   { transform: translateX(0) scale(1); opacity: 1; }
    }
  `]
})
export class ToastContainerComponent {
  constructor(public toastService: ToastService) {}
}
