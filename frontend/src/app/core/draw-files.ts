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
  ]);
  if (Object.keys(data).some((key) => !allowed.has(key)))
    throw new Error('Plik zawiera nieobsługiwane pola. Użyj eksportu drabinki.');
  const title = data['title'];
  const asOf = data['asOf'];
  const ids = data['playerIds'];
  const surface = data['surface'] ?? 'HARD';
  const bestOf = data['bestOf'] ?? 3;
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
  if (
    !Array.isArray(ids) ||
    ![2, 4, 8, 16].includes(ids.length) ||
    ids.some((id) => !Number.isSafeInteger(id) || id <= 0) ||
    new Set(ids).size !== ids.length
  )
    throw new Error('Drabinka wymaga 2, 4, 8 lub 16 różnych zawodników.');
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
    playerIds: ids as number[],
    surface: surface as Surface,
    bestOf: bestOf as number,
    modelVersion,
  };
}
