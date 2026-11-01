import { describe, it, expect } from 'vitest';
import { defaultDraw, parseDraw } from './api';

const known = defaultDraw.playerIds;
describe('draw import', () => {
  it('accepts a full structured draw without changing bracket order', () => {
    expect(parseDraw(JSON.stringify(defaultDraw), known).playerIds).toEqual(known);
  });
  it('rejects duplicate, unknown and malformed participants', () => {
    for (const ids of [['sinner', 'sinner'], ['sinner', 'missing'], known.slice(0,3)]) {
      expect(() => parseDraw(JSON.stringify({ ...defaultDraw, playerIds: ids }), known)).toThrow();
    }
  });
  it('rejects fractional or descending payouts and unsupported fields', () => {
    for (const patch of [{ prizes: [1,2,3,2] }, { prizes: [1,2,3,4.5] }, { currency: 'XXX' }, { extra: true }]) {
      expect(() => parseDraw(JSON.stringify({ ...defaultDraw, ...patch }), known)).toThrow();
    }
  });
});
