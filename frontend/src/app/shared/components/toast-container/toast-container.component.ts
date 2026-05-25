import { Component } from '@angular/core';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  template: `
    <div class="toast-container">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="toast toast-{{ toast.type }}" (click)="toastService.remove(toast.id)">
          {{ toast.message }}
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed; bottom: 24px; right: 24px;
      z-index: 999; display: flex; flex-direction: column; gap: 8px;
    }
    .toast {
      background: var(--surface); border: 1px solid var(--border);
      border-radius: 10px; padding: 14px 18px;
      font-size: 13px; min-width: 260px; cursor: pointer;
      box-shadow: 0 4px 20px rgba(0,0,0,.4);
      animation: slideIn .2s ease;
      &.toast-success { border-left: 3px solid var(--green); }
      &.toast-error   { border-left: 3px solid var(--red);   }
      &.toast-info    { border-left: 3px solid var(--accent); }
    }
    @keyframes slideIn {
      from { transform: translateX(100%); opacity: 0; }
      to   { transform: translateX(0);    opacity: 1; }
    }
  `]
})
export class ToastContainerComponent {
  constructor(public toastService: ToastService) {}
}
