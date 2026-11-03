import test from 'node:test';
import assert from 'node:assert/strict';
import { parseDrawImport, seedSlots, validateSlots } from '../src/app/core/draw-files.ts';

const models = ['ranking-baseline-v1', 'rank-logistic-v1'];
const today = '2026-11-03';
const original = { title: 'ATP', asOf: '2026-06-08', playerIds: [1, 2, 3, 4] };

test('older exports receive documented match defaults', () => {
  const result = parseDrawImport(original, models, today);
  assert.equal(result.surface, 'HARD');
  assert.equal(result.bestOf, 3);
  assert.equal(result.modelVersion, 'ranking-baseline-v1');
});
test('duplicate players and unsafe numeric identities are rejected', () => {
  assert.throws(() => parseDrawImport({ ...original, playerIds: [1, 1] }, models, today));
  assert.throws(() =>
    parseDrawImport({ ...original, playerIds: [1, Number.MAX_SAFE_INTEGER + 1] }, models, today),
  );
});
test('invalid calendar dates and future data are rejected', () => {
  assert.throws(() => parseDrawImport({ ...original, asOf: '2026-02-31' }, models, today));
  assert.throws(() => parseDrawImport({ ...original, asOf: '2027-01-01' }, models, today));
});
test('model selection is retained while persistence version is discarded on import', () => {
  const result = parseDrawImport(
    { ...original, surface: 'CLAY', bestOf: 5, modelVersion: 'rank-logistic-v1', version: 14 },
    models,
    today,
  );
  assert.equal(result.modelVersion, 'rank-logistic-v1');
  assert.equal(result.bestOf, 5);
  assert.equal('version' in result, false);
  assert.throws(() => parseDrawImport({ ...original, modelVersion: 'unknown' }, models, today));
});

test('96 entrants occupy 128 slots with byes for the leading 32 seeds', () => {
  const slots = seedSlots(
    Array.from({ length: 500 }, (_, i) => i + 1),
    128,
    96,
  );
  assert.equal(slots.filter((id) => id === null).length, 32);
  assert.equal(new Set(slots.filter((id) => id !== null)).size, 96);
  for (let i = 0; i < 128; i += 2)
    if (slots[i] === null || slots[i + 1] === null) assert.ok((slots[i] ?? slots[i + 1]) <= 32);
});
test('large imports preserve explicit byes but reject empty matches', () => {
  const slots = seedSlots(
    Array.from({ length: 500 }, (_, i) => i + 1),
    32,
    28,
  );
  const imported = parseDrawImport(
    { ...original, playerIds: slots, formatCode: 'ATP250_28' },
    models,
    today,
  );
  assert.deepEqual(imported.playerIds, slots);
  assert.throws(() => validateSlots([null, null, 1, 2]));
  assert.throws(() => parseDrawImport({ ...original, formatCode: 'unknown' }, models, today));
});
