'use strict';

const byId = (id) => document.getElementById(id);
const dateLabel = (date) => new Intl.DateTimeFormat('pl-PL', { dateStyle: 'medium' }).format(new Date(`${date}T12:00:00`));
const percent = (value) => new Intl.NumberFormat('pl-PL', { style: 'percent', maximumFractionDigits: 1 }).format(value);
let players = [];
let previewSequence = 0;
let previewTimer;

function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

async function request(path, method = 'GET', body) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    headers[document.querySelector('meta[name="_csrf_header"]').content] = document.querySelector('meta[name="_csrf"]').content;
  }
  const response = await fetch(path, { method, headers, credentials: 'same-origin', body: body === undefined ? undefined : JSON.stringify(body) });
  const data = await response.json();
  if (!response.ok) throw new Error(data.detail || 'Nie udało się wykonać operacji. Sprawdź dane i spróbuj ponownie.');
  return data;
}

function showError(error) {
  byId('error').textContent = error.message || String(error);
  byId('error').hidden = false;
  byId('loading').hidden = true;
  byId('results').hidden = true;
}

function clearError() { byId('error').hidden = true; }

function payload() {
  const playerIds = [...byId('slots').querySelectorAll('select')].map((select) => Number(select.value));
  if (playerIds.length !== Number(byId('draw-size').value) || playerIds.some((id) => !Number.isInteger(id) || id <= 0))
    throw new Error('Wybierz zawodników dla wszystkich miejsc w drabince.');
  if (new Set(playerIds).size !== playerIds.length) throw new Error('Każdy zawodnik może wystąpić w drabince tylko raz. Zmień powtarzającą się osobę.');
  return { title: byId('draw-title').value.trim() || 'Mój turniej ATP', asOf: byId('as-of').value, playerIds };
}

function defaultIds(size) {
  const seedOrder = (count) => {
    if (count === 2) return [1, 2];
    return seedOrder(count / 2).flatMap((seed, index) => index % 2 === 0 ? [seed, count + 1 - seed] : [count + 1 - seed, seed]);
  };
  return seedOrder(size).map((seed) => players[seed - 1]?.id);
}

function renderSlots(ids = defaultIds(Number(byId('draw-size').value))) {
  const container = byId('slots');
  container.replaceChildren();
  for (let i = 0; i < ids.length; i += 2) {
    const pair = element('div', 'pair');
    pair.append(element('div', 'pair-label', `PARA ${String(i / 2 + 1).padStart(2, '0')}`));
    for (let offset = 0; offset < 2; offset++) {
      const index = i + offset;
      const row = element('div', 'slot');
      const label = element('label', '', String(index + 1));
      label.htmlFor = `slot-${index}`;
      const select = element('select');
      select.id = `slot-${index}`;
      select.setAttribute('aria-label', `Zawodnik na miejscu ${index + 1}`);
      for (const player of players) select.add(new Option(`#${player.position} · ${player.name}`, String(player.id)));
      select.value = ids[index] === undefined ? '' : String(ids[index]);
      select.addEventListener('change', schedulePreview);
      row.append(label, select);
      pair.append(row);
    }
    container.append(pair);
  }
  byId('save-button').disabled = players.length < ids.length;
}

async function loadCatalog(date) {
  const query = date ? `?asOf=${encodeURIComponent(date)}` : '';
  const catalog = await request(`/api/tournaments/players${query}`);
  players = catalog.players;
  byId('player-count').textContent = String(players.length);
  byId('snapshot-date').textContent = catalog.latestRankingDate ? dateLabel(catalog.latestRankingDate) : 'Brak danych';
  if (players.length < 2) {
    byId('slots').replaceChildren();
    byId('save-button').disabled = true;
    throw new Error('Brak wystarczających rankingów dla tej daty. Wybierz późniejszą datę lub wykonaj import opisany w README projektu.');
  }
  byId('as-of').value = date || catalog.latestRankingDate;
  if (Number(byId('draw-size').value) > players.length) byId('draw-size').value = '2';
}

function schedulePreview() {
  clearTimeout(previewTimer);
  previewSequence++;
  byId('results').hidden = true;
  byId('loading').hidden = false;
  history.replaceState(null, '', location.pathname);
  previewTimer = setTimeout(() => preview().catch(showError), 250);
}

async function preview() {
  const sequence = ++previewSequence;
  clearError();
  const data = await request('/api/tournaments/preview', 'POST', payload());
  if (sequence === previewSequence) renderResult(data);
}

function renderResult(data) {
  clearError();
  byId('loading').hidden = true;
  byId('results').hidden = false;
  byId('result-title').textContent = data.title;
  byId('result-meta').textContent = `Dane dostępne do ${dateLabel(data.asOf)} · ${data.forecast.titleChances.length} graczy · ATP`;
  byId('save-status').textContent = data.id ? '✓ Zapisano' : 'Podgląd';
  const bracket = byId('bracket');
  bracket.replaceChildren();
  for (const round of data.forecast.rounds) {
    const column = element('section', 'round');
    column.append(element('h4', '', round.name));
    const matches = element('div', 'round-matches');
    for (const match of round.matches) {
      const card = element('div', 'match');
      for (const [player, probability] of [[match.playerA, match.probabilityA], [match.playerB, match.probabilityB]]) {
        const row = element('div', `match-player${player.id === match.predictedWinnerId ? ' winner' : ''}`);
        const name = element('span', 'match-name');
        name.title = player.name;
        name.append(element('span', 'match-rank', `#${player.position}`), document.createTextNode(player.name));
        row.append(name, element('span', 'match-probability', percent(probability)));
        card.append(row);
      }
      matches.append(card);
    }
    column.append(matches);
    bracket.append(column);
  }
  const chances = byId('title-chances');
  chances.replaceChildren();
  data.forecast.titleChances.forEach((chance, index) => {
    const row = element('div', 'chance-row');
    const name = element('span', 'chance-name');
    name.title = chance.player.name;
    name.append(element('span', 'chance-number', String(index + 1).padStart(2, '0')), document.createTextNode(chance.player.name));
    const track = element('div', 'bar-track');
    const fill = element('div', 'bar-fill');
    fill.style.width = `${chance.probability * 100}%`;
    track.append(fill);
    row.append(name, track, element('span', 'chance-value', percent(chance.probability)));
    chances.append(row);
  });
}

async function refreshSaved() {
  const draws = await request('/api/tournaments');
  const container = byId('saved-draws');
  container.replaceChildren();
  if (!draws.length) container.append(element('p', 'tiny', 'Zapisz pierwszą drabinkę — pojawi się tutaj.'));
  for (const draw of draws) {
    const button = element('button', 'saved-card');
    button.type = 'button';
    const text = element('span');
    text.append(element('strong', '', draw.title), element('small', '', `${draw.players} graczy · ${dateLabel(draw.asOf)}`));
    button.append(text, element('span', '', '↗'));
    button.addEventListener('click', () => openSaved(draw.id).catch(showError));
    container.append(button);
  }
}

async function openSaved(id) {
  clearTimeout(previewTimer);
  previewSequence++;
  const data = await request(`/api/tournaments/${encodeURIComponent(id)}`);
  await loadCatalog(data.asOf);
  byId('draw-title').value = data.title;
  byId('draw-size').value = String(data.forecast.titleChances.length);
  const ids = data.forecast.rounds[0].matches.flatMap((match) => [match.playerA.id, match.playerB.id]);
  renderSlots(ids);
  renderResult(data);
  history.replaceState(null, '', `?draw=${encodeURIComponent(data.id)}`);
}

byId('draw-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  clearTimeout(previewTimer);
  previewSequence++;
  const button = byId('save-button');
  button.disabled = true;
  try {
    const data = await request('/api/tournaments', 'POST', payload());
    renderResult(data);
    history.replaceState(null, '', `?draw=${encodeURIComponent(data.id)}`);
    await refreshSaved();
  } catch (error) { showError(error); }
  finally { button.disabled = false; }
});

byId('draw-size').addEventListener('change', () => { renderSlots(); schedulePreview(); });
byId('draw-title').addEventListener('input', schedulePreview);
byId('as-of').addEventListener('change', async () => {
  clearTimeout(previewTimer);
  previewSequence++;
  try { await loadCatalog(byId('as-of').value); renderSlots(); await preview(); }
  catch (error) { showError(error); }
});

byId('draw-file').addEventListener('change', async (event) => {
  try {
    const file = event.target.files[0];
    if (!file) return;
    if (file.size > 100_000) throw new Error('Plik drabinki jest za duży. Wybierz JSON do 100 KB.');
    const data = JSON.parse(await file.text());
    if (!data || typeof data.title !== 'string' || !data.title.trim() || data.title.length > 100
        || typeof data.asOf !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(data.asOf)
        || !Array.isArray(data.playerIds) || ![2, 4, 8, 16].includes(data.playerIds.length)
        || data.playerIds.some((id) => !Number.isSafeInteger(id) || id <= 0)
        || new Set(data.playerIds).size !== data.playerIds.length)
      throw new Error('Nieprawidłowy JSON. Użyj formatu wyeksportowanego z tego ekranu.');
    clearTimeout(previewTimer);
    previewSequence++;
    await loadCatalog(data.asOf);
    if (data.playerIds.some((id) => !players.some((player) => player.id === id)))
      throw new Error('Niektórzy zawodnicy z pliku nie mają rankingu dla podanej daty w tej bazie.');
    byId('draw-title').value = data.title;
    byId('draw-size').value = String(data.playerIds.length);
    renderSlots(data.playerIds);
    history.replaceState(null, '', location.pathname);
    await preview();
  } catch (error) { showError(error instanceof SyntaxError ? new Error('Nie można odczytać JSON. Sprawdź format pliku.') : error); }
  finally { event.target.value = ''; }
});

byId('export-button').addEventListener('click', () => {
  try {
    const url = URL.createObjectURL(new Blob([JSON.stringify(payload(), null, 2)], { type: 'application/json' }));
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = 'predictteniz-drabinka.json';
    anchor.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  } catch (error) { showError(error); }
});

async function start() {
  byId('as-of').max = new Date().toLocaleDateString('en-CA');
  const id = new URLSearchParams(location.search).get('draw');
  if (id) await openSaved(id);
  else { await loadCatalog(); renderSlots(); await preview(); }
  await refreshSaved();
}

start().catch(showError);
