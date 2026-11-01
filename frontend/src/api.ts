export type Surface = 'HARD' | 'CLAY' | 'GRASS';
export type Player = { id: string; name: string; country: string; hardElo: number; clayElo: number; grassElo: number };
export type Draw = { title: string; surface: Surface; playerIds: string[]; prizes: number[]; currency: 'EUR' | 'USD' | 'GBP' | 'PLN' };
export type Standing = { player: Player; advancement: number[]; titleProbability: number; expectedPrize: number };
export type Match = { firstId: string; secondId: string; winnerId: string; firstProbability: number };
export type Result = { id: string; title: string; surface: Surface; currency: string; prizes: number[]; createdAt: string; modelVersion: string; dataMode: string; standings: Standing[]; bracket: Match[][] };
export type Summary = { id: string; title: string; surface: Surface; createdAt: string };
export type Prediction = { first: Player; second: Player; firstProbability: number; ratingDifference: number; modelVersion: string };
export type Fixture = { id: string; date: string; time: string; first: string; second: string; tournament: string; status: string };

export const defaultDraw: Draw = {
  title: 'CourtVision Invitational', surface: 'HARD',
  playerIds: ['sinner', 'hurkacz', 'medvedev', 'fritz', 'alcaraz', 'ruud', 'djokovic', 'zverev'],
  prizes: [25000, 50000, 100000, 200000], currency: 'EUR',
};

export async function api<T>(path: string, body?: unknown): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`/api${path}`, {
      method: body === undefined ? 'GET' : 'POST',
      headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: AbortSignal.timeout(20000),
    });
  } catch { throw new Error('Cannot reach the backend. Start Spring Boot on port 8080 and try again.'); }
  const payload = await response.json().catch(() => null);
  if (!response.ok) throw new Error(payload?.detail || `Request failed (${response.status}). Check that the backend is running.`);
  if (payload === null) throw new Error('The backend returned an empty response.');
  return payload as T;
}

export function parseDraw(text: string, knownIds: string[]): Draw {
  const d = JSON.parse(text);
  if (!d || typeof d !== 'object' || Array.isArray(d)) throw new Error('The file must contain a JSON draw object.');
  const keys = ['title', 'surface', 'playerIds', 'prizes', 'currency'];
  if (Object.keys(d).some(key => !keys.includes(key))) throw new Error('Use only title, surface, playerIds, prizes and currency.');
  if (typeof d.title !== 'string' || !d.title.trim() || d.title.length > 100) throw new Error('Provide a title of 1–100 characters.');
  if (!['HARD', 'CLAY', 'GRASS'].includes(d.surface)) throw new Error('Surface must be HARD, CLAY or GRASS.');
  const ids = d.playerIds;
  if (!Array.isArray(ids) || ids.length < 2 || ids.length > 64 || (ids.length & (ids.length - 1)) !== 0) throw new Error('Use 2, 4, 8, 16, 32 or 64 player IDs in bracket order.');
  if (new Set(ids).size !== ids.length) throw new Error('Each player may appear only once.');
  if (ids.some(id => typeof id !== 'string' || !knownIds.includes(id))) throw new Error('Unknown player ID. Open the player library to see supported IDs.');
  if (!Array.isArray(d.prizes) || d.prizes.length !== Math.log2(ids.length) + 1 || d.prizes.some((v: unknown, i: number) => typeof v !== 'number' || !Number.isInteger(v) || v < 0 || v > 1e9 || (i > 0 && v < d.prizes[i-1]))) throw new Error('Use ascending, non-negative whole-number total payouts: one per exit round, then the champion.');
  if (!['EUR', 'USD', 'GBP', 'PLN'].includes(d.currency)) throw new Error('Currency must be EUR, USD, GBP or PLN.');
  return d as Draw;
}

export const rating = (p: Player, surface: Surface) => ({ HARD: p.hardElo, CLAY: p.clayElo, GRASS: p.grassElo })[surface];
export const percent = (p: number) => `${(p * 100).toFixed(1)}%`;
export const money = (value: number, currency: string) => new Intl.NumberFormat('en', { style: 'currency', currency, maximumFractionDigits: 0 }).format(value);
export const roundName = (players: number) => ({ 2: 'Final', 4: 'Semi-finals', 8: 'Quarter-finals' })[players] || `Round of ${players}`;
