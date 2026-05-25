import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TopbarComponent } from './layout/topbar/topbar.component';
import { ToastContainerComponent } from './shared/components/toast-container/toast-container.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, TopbarComponent, ToastContainerComponent],
  template: `
    <app-topbar />
    <main class="main-content">
      <router-outlet />
    </main>
    <app-toast-container />
  `,
  styles: [`
    .main-content {
      max-width: 1400px;
      margin: 0 auto;
      padding: 28px 32px;
    }
    @media (max-width: 768px) {
      .main-content { padding: 16px; }
    }
  `]
})
export class AppComponent {}
