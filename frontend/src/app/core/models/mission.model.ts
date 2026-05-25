export type MissionStatus =
  | 'NEW' | 'ANALYZED' | 'SHORTLISTED' | 'CONTACTED'
  | 'REPLIED' | 'IN_DISCUSSION' | 'ARCHIVED';

export type Source = 'MALT' | 'FREELANCE_COM' | 'TALENT_IO' | 'LINKEDIN' | 'MANUAL';

export interface Mission {
  id: number;
  source: Source;
  title: string;
  company?: string;
  location?: string;
  remote?: boolean;
  tjmMin?: number;
  tjmMax?: number;
  duration?: string;
  skills?: string[];
  url?: string;
  status: MissionStatus;
  fitScore?: number;
  fitSummary?: string;
  decisionMakerHint?: string;
  detectedAt: string;
}

export interface RadarStats {
  missionsLast7Days: number;
  topMissions: number;
  avgFitScore: number;
  hotSignals: number;
  missionsBySource: Record<string, number>;
}

export interface ScanResult {
  newMissions: number;
  duplicates: number;
  analyzed: number;
  errors: string[];
}

export interface SignalScanResult {
  newSignals: number;
  errors: string[];
}
