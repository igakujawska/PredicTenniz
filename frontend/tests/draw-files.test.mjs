import test from 'node:test';
import assert from 'node:assert/strict';
import { parseDrawImport } from '../src/app/core/draw-files.ts';

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
