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
  metrics?: { ml: Scores; baseline: Scores; coinFlip: Scores } | null;
}
export interface Match {
  playerA: Player;
  playerB: Player;
  probabilityA: number;
  probabilityB: number;
  predictedWinnerId: number;
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
  playerIds: number[];
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
  playerIds: number[];
  surface: Surface;
  bestOf: number;
  modelVersion: string;
  forecast: Forecast | null;
  predictionError?: string | null;
}
export interface DrawSummary {
  id: string;
  title: string;
  asOf: string;
  players: number;
}
