export type Surface = 'HARD' | 'CLAY' | 'GRASS';
export interface Player {
  id: number;
  name: string;
  countryCode: string | null;
  position: number;
  rankingDate: string;
  sourceId?: string | null;
}
export interface Catalog {
  players: Player[];
  latestRankingDate: string | null;
}
export interface Scores {
  logLoss: number;
  brierScore: number;
  accuracy: number;
}
export interface ModelInfo {
  version: string;
  label: string;
  trained: boolean;
  trainedThrough?: string | null;
  testYear?: number | null;
  testMatches?: number | null;
  metrics?: {
    ml: Scores;
    baseline: Scores;
    coinFlip: Scores;
    logistic?: Scores | null;
    elo?: Scores | null;
  } | null;
  dataThrough?: string | null;
  trainMatches?: number | null;
  historyMatches?: number | null;
  featureNames?: string[];
  featureImportance?: { feature: string; gainShare: number }[];
  categoryScores?: Record<string, { matches: number; model: Scores; ranking: Scores }>;
  notes?: string[];
}
export interface Match {
  playerA: Player | null;
  playerB: Player | null;
  probabilityA: number;
  probabilityB: number;
  predictedWinnerId: number;
  bye?: boolean;
}
export interface Round {
  number: number;
  name: string;
  matches: Match[];
}
export interface Forecast {
  modelVersion: string;
  surface: Surface;
  bestOf: number;
  rounds: Round[];
  titleChances: { player: Player; probability: number }[];
}
export interface DrawRequest {
  title: string;
  asOf: string;
  playerIds: (number | null)[];
  formatCode?: string;
  surface: Surface;
  bestOf: number;
  modelVersion: string;
  version?: number;
}
export interface Draw {
  id: string | null;
  title: string;
  asOf: string;
  createdAt: string | null;
  version?: number;
  playerIds: (number | null)[];
  formatCode?: string;
  surface: Surface;
  bestOf: number;
  modelVersion: string;
  forecast: Forecast | null;
  predictionError?: string | null;
  formatInfo?: TournamentFormatInfo;
}
export interface DrawSummary {
  id: string;
  title: string;
  asOf: string;
  players: number;
}

export interface TournamentFormatInfo {
  code: string;
  label: string;
  category: string;
  entrants: number;
  drawSize: number;
  byes: number;
  seeds: number;
  bestOf: number;
  championPoints: number;
}
