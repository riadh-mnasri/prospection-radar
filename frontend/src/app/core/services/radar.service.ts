import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Mission, MissionStatus, RadarStats, ScanResult, SignalScanResult } from '../models/mission.model';
import { Signal } from '../models/signal.model';

@Injectable({ providedIn: 'root' })
export class RadarService {

  private readonly api = '/api/radar';

  constructor(private http: HttpClient) {}

  getStats(): Observable<RadarStats> {
    return this.http.get<RadarStats>(`${this.api}/stats`);
  }

  getMissions(minScore = 0, status?: MissionStatus): Observable<Mission[]> {
    let params = new HttpParams().set('minScore', minScore);
    if (status) params = params.set('status', status);
    return this.http.get<Mission[]>(`${this.api}/missions`, { params });
  }

  getTopMissions(): Observable<Mission[]> {
    return this.http.get<Mission[]>(`${this.api}/missions/top`);
  }

  updateMissionStatus(id: number, status: MissionStatus): Observable<Mission> {
    return this.http.patch<Mission>(`${this.api}/missions/${id}/status`, { status });
  }

  getSignals(minScore = 0): Observable<Signal[]> {
    const params = new HttpParams().set('minScore', minScore);
    return this.http.get<Signal[]>(`${this.api}/signals`, { params });
  }

  getHotSignals(): Observable<Signal[]> {
    return this.http.get<Signal[]>(`${this.api}/signals/hot`);
  }

  getFavorites(): Observable<Mission[]> {
    return this.http.get<Mission[]>(`${this.api}/missions/favorites`);
  }

  toggleFavorite(id: number, favorite: boolean): Observable<Mission> {
    return this.http.patch<Mission>(`${this.api}/missions/${id}/favorite`, { favorite });
  }

  generateOutreach(id: number): Observable<{ linkedinMessage: string; emailSubject: string; emailBody: string }> {
    return this.http.post<{ linkedinMessage: string; emailSubject: string; emailBody: string }>(
      `${this.api}/missions/${id}/outreach`, {}
    );
  }

  scanMissions(): Observable<ScanResult> {
    return this.http.post<ScanResult>(`${this.api}/scan/missions`, {});
  }

  scanSignals(): Observable<SignalScanResult> {
    return this.http.post<SignalScanResult>(`${this.api}/scan/signals`, {});
  }
}
