import type { DrawRequest, Surface } from './contracts';

export function parseDrawImport(
  value: unknown,
  modelVersions: string[],
  today: string,
): DrawRequest {
  if (!value || typeof value !== 'object' || Array.isArray(value))
    throw new Error('Plik musi zawierać obiekt drabinki.');
  const data = value as Record<string, unknown>;
  const allowed = new Set([
    'title',
    'asOf',
    'playerIds',
    'surface',
    'bestOf',
    'modelVersion',
    'version',
    'formatCode',
  ]);
  if (Object.keys(data).some((key) => !allowed.has(key)))
    throw new Error('Plik zawiera nieobsługiwane pola. Użyj eksportu drabinki.');
  const title = data['title'];
  const asOf = data['asOf'];
  const ids = data['playerIds'];
  const surface = data['surface'] ?? 'HARD';
  const bestOf = data['bestOf'] ?? 3;
  const formatCode = data['formatCode'] ?? 'CUSTOM';
  if (
    typeof formatCode !== 'string' ||
    ![
      'CUSTOM',
      'ATP250_28',
      'ATP250_32',
      'ATP250_48',
      'ATP500_32',
      'ATP500_48',
      'MASTERS_56',
      'MASTERS_96',
      'SLAM_128',
    ].includes(formatCode)
  )
    throw new Error('Nieznany format turnieju.');
  const modelVersion = data['modelVersion'] ?? 'ranking-baseline-v1';
  if (typeof title !== 'string' || !title.trim() || title.trim().length > 100)
    throw new Error('Nazwa turnieju powinna mieć od 1 do 100 znaków.');
  if (
    typeof asOf !== 'string' ||
    !/^\d{4}-\d{2}-\d{2}$/.test(asOf) ||
    asOf > today ||
    Number.isNaN(Date.parse(asOf)) ||
    new Date(asOf).toISOString().slice(0, 10) !== asOf
  )
    throw new Error('Podaj poprawną datę danych, nie późniejszą niż dzisiaj.');
  validateSlots(ids);
  if (
    typeof surface !== 'string' ||
    !['HARD', 'CLAY', 'GRASS'].includes(surface) ||
    ![3, 5].includes(bestOf as number)
  )
    throw new Error('Nieprawidłowa nawierzchnia lub format meczu.');
  if (typeof modelVersion !== 'string' || !modelVersions.includes(modelVersion))
    throw new Error('Wybrany model jest niedostępny.');
  return {
    title: title.trim(),
    asOf,
    playerIds: ids as (number | null)[],
    formatCode,
    surface: surface as Surface,
    bestOf: bestOf as number,
    modelVersion,
  };
}

export function validateSlots(ids: unknown): asserts ids is (number | null)[] {
  if (!Array.isArray(ids) || ![2, 4, 8, 16, 32, 64, 128].includes(ids.length))
    throw new Error('Drabinka wymaga 2–128 miejsc (potęga dwójki).');
  const players = ids.filter((id) => id !== null);
  if (
    players.length < 2 ||
    players.some((id) => !Number.isSafeInteger(id) || id <= 0) ||
    new Set(players).size !== players.length
  )
    throw new Error('Wybierz różnych zawodników; wolny los oznacz jako null.');
  for (let slot = 0; slot < ids.length; slot += 2)
    if (ids[slot] === null && ids[slot + 1] === null)
      throw new Error('Dwa wolne losy nie mogą tworzyć pary.');
}

export function seedSlots(players: number[], size: number, entrants: number): (number | null)[] {
  if (
    ![2, 4, 8, 16, 32, 64, 128].includes(size) ||
    entrants > size ||
    entrants < size / 2 ||
    players.length < entrants
  )
    throw new Error('Za mało zawodników lub niepoprawny rozmiar drabinki.');
  const seeds = (count: number): number[] =>
    count === 2
      ? [1, 2]
      : seeds(count / 2).flatMap((seed, index) =>
          index % 2 === 0 ? [seed, count + 1 - seed] : [count + 1 - seed, seed],
        );
  const slots = seeds(size).map((seed) => (seed <= entrants ? players[seed - 1] : null));
  validateSlots(slots);
  return slots;
}
