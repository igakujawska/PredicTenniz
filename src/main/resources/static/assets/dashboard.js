'use strict';

const byId = (id) => document.getElementById(id);
const dateLabel = (date) => new Intl.DateTimeFormat('pl-PL', { dateStyle: 'medium' }).format(new Date(`${date}T12:00:00`));
const percent = (value) => new Intl.NumberFormat('pl-PL', { style: 'percent', maximumFractionDigits: 1 }).format(value);
const decimal = (value) => new Intl.NumberFormat('pl-PL', { minimumFractionDigits: 4, maximumFractionDigits: 4 }).format(value);
const reducedMotion = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches;
let players = [];
let models = [];
let previewSequence = 0;
let previewTimer;
let previewController;
let counterFrame;
let lastChances = new Map();

function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

async function request(path, method = 'GET', body, signal) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    headers[document.querySelector('meta[name="_csrf_header"]').content] = document.querySelector('meta[name="_csrf"]').content;
  }
  const response = await fetch(path, { method, headers, signal, credentials: 'same-origin', body: body === undefined ? undefined : JSON.stringify(body) });
  const data = await response.json();
  if (!response.ok) throw new Error(data.detail || 'Nie udało się wykonać operacji. Sprawdź dane i spróbuj ponownie.');
  return data;
}

function setPending(pending) {
  const results = byId('results');
  results.classList.toggle('is-pending', pending);
  results.setAttribute('aria-busy', String(pending));
  byId('save-button').disabled = pending || players.length < Number(byId('draw-size').value);
  if (pending) {
    byId('save-status').textContent = 'Przeliczam…';
    byId('loading').hidden = !results.hidden;
  }
}

function showError(error) {
  setPending(false);
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
  if (new Set(playerIds).size !== playerIds.length) throw new Error('Każdy zawodnik może wystąpić tylko raz. Zmień powtarzającą się osobę.');
  return { title: byId('draw-title').value.trim() || 'Mój turniej ATP', asOf: byId('as-of').value, playerIds,
    surface: document.querySelector('input[name="surface"]:checked').value,
    bestOf: Number(byId('best-of').value), modelVersion: byId('model-version').value };
}

function defaultIds(size) {
  const seedOrder = (count) => count === 2 ? [1, 2] : seedOrder(count / 2)
    .flatMap((seed, index) => index % 2 === 0 ? [seed, count + 1 - seed] : [count + 1 - seed, seed]);
  return seedOrder(size).map((seed) => players[seed - 1]?.id);
}

function renderSlots(ids = defaultIds(Number(byId('draw-size').value))) {
  const container = byId('slots');
  container.replaceChildren();
  for (let i = 0; i < ids.length; i += 2) {
    const pair = element('div', 'pair');
    const heading = element('div', 'pair-label', `PARA ${String(i / 2 + 1).padStart(2, '0')}`);
    const swap = element('button', '', '⇅');
    swap.type = 'button';
    swap.setAttribute('aria-label', `Zamień miejscami zawodników pary ${i / 2 + 1}`);
    swap.addEventListener('click', () => {
      const selects = pair.querySelectorAll('select');
      [selects[0].value, selects[1].value] = [selects[1].value, selects[0].value];
      schedulePreview();
    });
    heading.append(swap);
    pair.append(heading);
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
  setPending(false);
}

function updateSurface() {
  const surface = document.querySelector('input[name="surface"]:checked').value;
  document.body.dataset.surface = surface;
  byId('surface-caption').textContent = `${surface} COURT`;
}

function updateModelAvailability() {
  for (const option of byId('model-version').options) {
    const model = models.find((item) => item.version === option.value);
    option.disabled = Boolean(model?.trainedThrough && byId('as-of').value < model.trainedThrough);
  }
  if (byId('model-version').selectedOptions[0]?.disabled) byId('model-version').value = 'ranking-baseline-v1';
}

async function loadCatalog(date) {
  const catalog = await request(`/api/tournaments/players${date ? `?asOf=${encodeURIComponent(date)}` : ''}`);
  players = catalog.players;
  byId('player-count').textContent = String(players.length);
  byId('snapshot-date').textContent = catalog.latestRankingDate ? dateLabel(catalog.latestRankingDate) : 'Brak danych';
  if (players.length < 2) {
    byId('slots').replaceChildren();
    byId('save-button').disabled = true;
    throw new Error('Brak rankingów dla tej daty. Wybierz późniejszą datę lub wykonaj import opisany w README.');
  }
  byId('as-of').value = date || catalog.latestRankingDate;
  if (Number(byId('draw-size').value) > players.length) byId('draw-size').value = '2';
  updateModelAvailability();
}

function schedulePreview() {
  clearTimeout(previewTimer);
  previewController?.abort();
  previewSequence++;
  clearError();
  setPending(true);
  history.replaceState(null, '', location.pathname);
  previewTimer = setTimeout(preview, 220);
}

async function preview() {
  const sequence = ++previewSequence;
  previewController?.abort();
  previewController = new AbortController();
  clearError();
  setPending(true);
  try {
    const data = await request('/api/tournaments/preview', 'POST', payload(), previewController.signal);
    if (sequence === previewSequence) renderResult(data);
  } catch (error) {
    if (sequence === previewSequence && error.name !== 'AbortError') showError(error);
  } finally {
    if (sequence === previewSequence) setPending(false);
  }
}

function animateResults(data, oldChances) {
  if (reducedMotion()) return;
  byId('bracket').querySelectorAll('.match').forEach((card, index) => {
    card.animate([{ opacity: 0, transform: 'translateY(7px)' }, { opacity: 1, transform: 'translateY(0)' }],
      { duration: 300, delay: Math.min(index * 22, 150), easing: 'cubic-bezier(.2,.7,.3,1)', fill: 'backwards' });
  });
  const values = [...byId('title-chances').querySelectorAll('.chance-value')];
  byId('title-chances').querySelectorAll('.bar-fill').forEach((bar, index) => {
    const chance = data.forecast.titleChances[index];
    bar.animate([{ width: `${(oldChances.get(chance.player.id) || 0) * 100}%` }, { width: `${chance.probability * 100}%` }],
      { duration: 420, easing: 'cubic-bezier(.2,.7,.3,1)' });
  });
  cancelAnimationFrame(counterFrame);
  const started = performance.now();
  function tick(now) {
    const progress = Math.min((now - started) / 420, 1);
    const ease = 1 - (1 - progress) ** 3;
    data.forecast.titleChances.forEach((chance, index) => {
      const from = oldChances.get(chance.player.id) || 0;
      values[index].textContent = percent(from + (chance.probability - from) * ease);
    });
    if (progress < 1 && values[0]?.isConnected) counterFrame = requestAnimationFrame(tick);
  }
  counterFrame = requestAnimationFrame(tick);
}

function renderResult(data) {
  clearError();
  setPending(false);
  byId('loading').hidden = true;
  byId('results').hidden = false;
  byId('result-title').textContent = data.title;
  const surface = data.forecast.surface || 'HARD';
  const bestOf = data.forecast.bestOf || 3;
  byId('result-meta').textContent = `${dateLabel(data.asOf)} · ${data.forecast.titleChances.length} graczy · ${surface} · Best of ${bestOf}`;
  byId('save-status').textContent = data.id ? '✓ Zapisano' : 'Podgląd';
  const model = models.find((item) => item.version === data.forecast.modelVersion);
  byId('model-label').textContent = model?.trained ? 'Regresja logistyczna · ML.' : 'Model rankingowy · bazowy.';
  byId('model-description').textContent = model?.trained
    ? `Ranking, kort i format meczu. Dane treningowe do ${dateLabel(model.trainedThrough)}. Prognoza jest wynikiem modelu.`
    : 'Heurystyka oparta na rankingu. Kort i format wpływają na prognozę po wybraniu modelu ML.';
  byId('model-indicator').textContent = model?.trained ? 'ML' : 'BASELINE';
  const bracket = byId('bracket');
  bracket.replaceChildren();
  bracket.classList.remove('has-focus');
  for (const round of data.forecast.rounds) {
    const column = element('section', 'round');
    column.append(element('h4', '', round.name));
    const matches = element('div', 'round-matches');
    for (const match of round.matches) {
      const card = element('div', 'match');
      for (const [player, probability] of [[match.playerA, match.probabilityA], [match.playerB, match.probabilityB]]) {
        const row = element('div', `match-player${player.id === match.predictedWinnerId ? ' winner' : ''}`);
        row.dataset.playerId = String(player.id);
        row.tabIndex = 0;
        row.setAttribute('aria-label', `${player.name}, szansa w tym meczu ${percent(probability)}`);
        const initials = player.name.split(' ').filter(Boolean).map((part) => part[0]).filter((_, index, all) => index === 0 || index === all.length - 1).join('');
        const name = element('span', 'match-name');
        name.title = player.name;
        name.append(document.createTextNode(player.name), element('span', 'match-rank', `ATP #${player.position}`));
        row.append(element('span', 'match-avatar', initials), name, element('span', 'match-probability', percent(probability)));
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
  animateResults(data, lastChances);
  lastChances = new Map(data.forecast.titleChances.map((chance) => [chance.player.id, chance.probability]));
}

function highlightPlayer(id) {
  byId('bracket').classList.toggle('has-focus', Boolean(id));
  byId('bracket').querySelectorAll('.match-player').forEach((row) => row.classList.toggle('is-highlighted', row.dataset.playerId === id));
}
byId('bracket').addEventListener('pointerover', (event) => highlightPlayer(event.target.closest('.match-player')?.dataset.playerId));
byId('bracket').addEventListener('pointerleave', () => highlightPlayer());
byId('bracket').addEventListener('focusin', (event) => highlightPlayer(event.target.closest('.match-player')?.dataset.playerId));
byId('bracket').addEventListener('focusout', () => highlightPlayer());

function renderModelLab() {
  const model = models.find((item) => item.trained);
  if (!model) return;
  byId('model-lab').hidden = false;
  byId('ml-protocol').textContent = `Regresja logistyczna · testowano na ${model.testMatches.toLocaleString('pl-PL')} meczach z ${model.testYear} roku. Te wyniki nie są oceną obecnej drabinki.`;
  byId('ml-accuracy').textContent = percent(model.metrics.ml.accuracy);
  byId('ml-logloss').textContent = decimal(model.metrics.ml.logLoss);
  byId('ml-brier').textContent = decimal(model.metrics.ml.brierScore);
  byId('ml-comparison').textContent = `Porównanie log loss: wzór bazowy ${decimal(model.metrics.baseline.logLoss)}, ML ${decimal(model.metrics.ml.logLoss)}. Różnica jest niewielka; trafność typów pozostała taka sama. Bez formy, kontuzji i statystyk serwisu.`;
}

async function refreshSaved() {
  const draws = await request('/api/tournaments');
  const container = byId('saved-draws');
  container.replaceChildren();
  if (!draws.length) container.append(element('p', 'tiny', 'Zapisz pierwszy scenariusz — pojawi się tutaj.'));
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

function cancelPreview() {
  clearTimeout(previewTimer);
  previewController?.abort();
  previewSequence++;
}

async function openSaved(id) {
  cancelPreview();
  const data = await request(`/api/tournaments/${encodeURIComponent(id)}`);
  await loadCatalog(data.asOf);
  byId('draw-title').value = data.title;
  byId('draw-size').value = String(data.forecast.titleChances.length);
  byId('model-version').value = data.forecast.modelVersion;
  byId('best-of').value = String(data.forecast.bestOf || 3);
  document.querySelector(`input[name="surface"][value="${data.forecast.surface || 'HARD'}"]`).checked = true;
  updateSurface();
  renderSlots(data.forecast.rounds[0].matches.flatMap((match) => [match.playerA.id, match.playerB.id]));
  renderResult(data);
  history.replaceState(null, '', `?draw=${encodeURIComponent(data.id)}`);
}

byId('draw-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  cancelPreview();
  const sequence = previewSequence;
  byId('save-button').disabled = true;
  try {
    const data = await request('/api/tournaments', 'POST', payload());
    if (sequence === previewSequence) {
      renderResult(data);
      history.replaceState(null, '', `?draw=${encodeURIComponent(data.id)}`);
    }
    await refreshSaved();
  } catch (error) { if (sequence === previewSequence) showError(error); }
  finally { if (sequence === previewSequence) setPending(false); }
});

byId('draw-size').addEventListener('change', () => { renderSlots(); schedulePreview(); });
byId('draw-title').addEventListener('input', schedulePreview);
byId('model-version').addEventListener('change', schedulePreview);
byId('best-of').addEventListener('change', schedulePreview);
document.querySelectorAll('input[name="surface"]').forEach((radio) => radio.addEventListener('change', () => { updateSurface(); schedulePreview(); }));
byId('as-of').addEventListener('change', async () => {
  cancelPreview();
  const sequence = previewSequence;
  try {
    await loadCatalog(byId('as-of').value);
    if (sequence === previewSequence) { renderSlots(); await preview(); }
  } catch (error) { if (sequence === previewSequence) showError(error); }
});

byId('shuffle-button').addEventListener('click', () => {
  try {
    const ids = [...payload().playerIds];
    for (let i = ids.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [ids[i], ids[j]] = [ids[j], ids[i]];
    }
    renderSlots(ids);
    if (!reducedMotion()) {
      byId('slots').classList.remove('is-shuffling');
      requestAnimationFrame(() => byId('slots').classList.add('is-shuffling'));
    }
    schedulePreview();
  } catch (error) { showError(error); }
});

byId('draw-file').addEventListener('change', async (event) => {
  try {
    const file = event.target.files[0];
    if (!file) return;
    if (file.size > 100_000) throw new Error('Wybierz plik JSON do 100 KB.');
    const data = JSON.parse(await file.text());
    const surface = data?.surface ?? 'HARD';
    const bestOf = data?.bestOf ?? 3;
    const modelVersion = data?.modelVersion ?? 'ranking-baseline-v1';
    if (!data || typeof data.title !== 'string' || !data.title.trim() || data.title.length > 100
        || typeof data.asOf !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(data.asOf)
        || !Array.isArray(data.playerIds) || ![2, 4, 8, 16].includes(data.playerIds.length)
        || data.playerIds.some((id) => !Number.isSafeInteger(id) || id <= 0)
        || new Set(data.playerIds).size !== data.playerIds.length || !['HARD', 'CLAY', 'GRASS'].includes(surface)
        || ![3, 5].includes(bestOf) || !models.some((model) => model.version === modelVersion))
      throw new Error('Nieprawidłowy JSON. Użyj formatu wyeksportowanego z tego ekranu.');
    cancelPreview();
    await loadCatalog(data.asOf);
    if (data.playerIds.some((id) => !players.some((player) => player.id === id))) throw new Error('Gracze z pliku nie mają rankingów w tej bazie dla wybranej daty.');
    byId('draw-title').value = data.title;
    byId('draw-size').value = String(data.playerIds.length);
    byId('model-version').value = modelVersion;
    byId('best-of').value = String(bestOf);
    document.querySelector(`input[name="surface"][value="${surface}"]`).checked = true;
    updateSurface();
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
  const now = new Date();
  byId('as-of').max = new Date(now - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 10);
  models = await request('/api/tournaments/models');
  byId('model-version').replaceChildren(...models.map((model) => new Option(model.label, model.version)));
  renderModelLab();
  const id = new URLSearchParams(location.search).get('draw');
  if (id) await openSaved(id);
  else {
    await loadCatalog();
    const trained = models.find((model) => model.trained && byId('as-of').value >= model.trainedThrough);
    if (trained) byId('model-version').value = trained.version;
    renderSlots();
    await preview();
  }
  await refreshSaved();
}

start().catch(showError);
