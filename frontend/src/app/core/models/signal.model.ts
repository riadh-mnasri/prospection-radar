export type SignalType =
  | 'FUNDING' | 'CTO_NOMINATION' | 'TECH_HIRING_SPREE'
  | 'JOB_STILL_OPEN' | 'FORMER_COLLEAGUE' | 'APPEL_OFFRES';

export type SignalStatus = 'NEW' | 'ANALYZED' | 'ACTIONED' | 'IGNORED';

export interface Signal {
  id: number;
  type: SignalType;
  company?: string;
  title: string;
  description?: string;
  fundingAmount?: string;
  personName?: string;
  newRole?: string;
  opportunityScore?: number;
  opportunityReason?: string;
  suggestedAction?: string;
  url?: string;
  detectedAt: string;
}
