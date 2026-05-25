import { Component, computed, signal } from '@angular/core';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { TopbarComponent } from './layout/topbar/topbar.component';
import { ToastContainerComponent } from './shared/components/toast-container/toast-container.component';

const PUBLIC_ROUTES = ['/', '/login'];

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, TopbarComponent, ToastContainerComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  readonly year = new Date().getFullYear();
  private readonly _currentUrl = signal('/');

  readonly isPublicRoute = computed(() => PUBLIC_ROUTES.includes(this._currentUrl()));

  constructor(router: Router) {
    router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe(e => {
      this._currentUrl.set((e as NavigationEnd).urlAfterRedirects);
    });
  }
}
