import { useEffect, useRef, useState } from 'react';
import { Activity, ArrowDownToLine, ArrowRight, BarChart3, BookOpen, Check, ChevronRight, CircleHelp, CircleDot, Clock3, Database, FlaskConical, GitBranch, LayoutDashboard, LoaderCircle, PanelLeftClose, Play, Plus, Radio, RefreshCw, Trophy, Upload, Users, X } from 'lucide-react';
import { api, defaultDraw, money, parseDraw, percent, rating, roundName } from './api';
import type { Draw, Fixture, Match, Player, Prediction, Result, Summary, Surface } from './api';

type Page = 'overview' | 'draw' | 'match' | 'players' | 'model' | 'history' | 'api';
const nav = [
  { id: 'overview', label: 'Overview', icon: LayoutDashboard },
  { id: 'draw', label: 'Draw explorer', icon: GitBranch },
  { id: 'match', label: 'Match lab', icon: FlaskConical },
  { id: 'players', label: 'Player library', icon: Users },
] as const;
const workspace = [
  { id: 'model', label: 'Model notes', icon: BookOpen },
  { id: 'history', label: 'Saved runs', icon: Clock3 },
  { id: 'api', label: 'Data connection', icon: Database },
] as const;
const surfaceLabels: Record<Surface, string> = { HARD: 'Hard court', CLAY: 'Clay court', GRASS: 'Grass court' };
const initials = (name: string) => name.split(' ').map(n => n[0]).slice(0,2).join('');
const shortName = (name: string) => name.split(' ').slice(1).join(' ') || name;

function Ball({ small = false }: { small?: boolean }) {
  return <svg className={small ? 'ball small' : 'ball'} viewBox="0 0 40 40" fill="none" aria-hidden="true"><circle cx="20" cy="20" r="19" fill="currentColor"/><path d="M5 8C17 11 22 22 20 39M20 1C18 17 25 29 36 31" stroke="var(--ball-line, #f6f7f2)" strokeWidth="2.2"/></svg>;
}

function App() {
  const [page, setPage] = useState<Page>('overview');
  const [players, setPlayers] = useState<Player[]>([]);
  const [draw, setDraw] = useState<Draw>(defaultDraw);
  const [result, setResult] = useState<Result | null>(null);
  const [history, setHistory] = useState<Summary[]>([]);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [connected, setConnected] = useState(false);
  const [provider, setProvider] = useState(false);
  const [importOpen, setImportOpen] = useState(false);
  const [json, setJson] = useState(JSON.stringify(defaultDraw, null, 2));
  const [importError, setImportError] = useState('');
  const [first, setFirst] = useState('sinner');
  const [second, setSecond] = useState('alcaraz');
  const [prediction, setPrediction] = useState<Prediction | null>(null);
  const [query, setQuery] = useState('');
  const [fixtureDate, setFixtureDate] = useState(new Date().toLocaleDateString('en-CA', { timeZone: 'Europe/Warsaw' }));
  const [fixtures, setFixtures] = useState<Fixture[] | null>(null);
  const fileInput = useRef<HTMLInputElement>(null);
  const lookup = Object.fromEntries(players.map(p => [p.id, p]));
  const pending = result && (draw.surface !== result.surface || draw.title !== result.title || JSON.stringify(draw.prizes) !== JSON.stringify(result.prizes));

  useEffect(() => {
    let active = true;
    async function init() {
      try {
        const [p, h, status, preview] = await Promise.all([
          api<Player[]>('/players'), api<Summary[]>('/simulations'),
          api<{ configured: boolean }>('/integrations/status'), api<Result>('/draws/preview', defaultDraw),
        ]);
        if (active) { setPlayers(p); setHistory(h); setProvider(status.configured); setResult(preview); setConnected(true); }
      } catch (e) { if (active) setError((e as Error).message); }
      finally { if (active) setLoading(false); }
    }
    void init();
    return () => { active = false; };
  }, []);

  function navigate(next: Page) { setPage(next); setNotice(''); }
  async function run() {
    setBusy(true); setError(''); setNotice('');
    try {
      const r = await api<Result>('/simulations', draw);
      setResult(r); setNotice('Run calculated and saved to your database.'); setConnected(true);
      setHistory(await api<Summary[]>('/simulations'));
    } catch (e) { setError((e as Error).message); }
    finally { setBusy(false); }
  }
  async function importDraw() {
    setImportError(''); setBusy(true);
    try {
      const d = parseDraw(json, players.map(p => p.id));
      const r = await api<Result>('/draws/preview', d);
      setDraw(d); setResult(r); setImportOpen(false); setPage('draw'); setNotice('Draw imported. Run prediction to save it.');
    } catch (e) { setImportError((e as Error).message); }
    finally { setBusy(false); }
  }
  async function loadRun(id: string) {
    setBusy(true); setError('');
    try {
      const r = await api<Result>(`/simulations/${encodeURIComponent(id)}`);
      setResult(r); setDraw({ title: r.title, surface: r.surface, currency: r.currency as Draw['currency'], prizes: r.prizes, playerIds: r.bracket[0].flatMap(m => [m.firstId, m.secondId]) });
      setPage('overview'); setNotice('Saved result loaded with its original model and inputs.');
    } catch(e) { setError((e as Error).message); }
    finally { setBusy(false); }
  }
  async function compare() {
    setBusy(true); setError('');
    try { setPrediction(await api<Prediction>('/predictions', { firstId: first, secondId: second, surface: draw.surface })); }
    catch(e) { setError((e as Error).message); }
    finally { setBusy(false); }
  }
  async function fetchFixtures() {
    setBusy(true); setError(''); setFixtures(null);
    try { setFixtures(await api<Fixture[]>(`/integrations/fixtures?date=${encodeURIComponent(fixtureDate)}`)); }
    catch(e) { setError((e as Error).message); }
    finally { setBusy(false); }
  }
  function download() {
    const blob = new Blob([JSON.stringify(draw, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = 'courtvision-draw.json'; a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
  const title = { overview: 'The bigger picture.', draw: 'Follow every possibility.', match: 'One match. Two possibilities.', players: 'Know the field.', model: 'Behind the probabilities.', history: 'Your prediction notebook.', api: 'Connect the dots.' }[page];
  const favorite = result?.standings[0];
  const totalPayout = result?.standings.reduce((sum, s) => sum + s.expectedPrize, 0) || 0;

  function renderMatch(m: Match, index: number) {
    return <div className="match-card" key={`${m.firstId}-${m.secondId}`}>
      {[{ id: m.firstId, p: m.firstProbability }, { id: m.secondId, p: 1 - m.firstProbability }].map(({ id, p }) => <div className={`match-player ${m.winnerId === id ? 'winner' : ''}`} key={id}>
        <span className="slot">{String(draw.playerIds.indexOf(id) + 1).padStart(2, '0')}</span><span className="player-name">{lookup[id]?.name || id}</span><span className="match-percent">{Math.round(p * 100)}<small>%</small></span>{m.winnerId === id && <span className="win-dot" />}
      </div>)}
      <div className="match-foot">MATCH {String(index + 1).padStart(2,'0')} <span>Conditional forecast</span></div>
    </div>;
  }
  function bracket() {
    if (!result) return null;
    return <section className="card draw-card">
      <div className="card-title"><div><span className="eyebrow">DRAW EXPLORER</span><h2>A possible path to the title</h2></div><span className="subtle-badge"><CircleDot size={12}/> {surfaceLabels[result.surface]}</span></div>
      <div className="bracket-scroll"><div className="bracket" style={{ minWidth: `${Math.max(760, result.bracket.length * 245 + 180)}px` }}>
        {result.bracket.map((matches, r) => <div className="bracket-round" key={r}><div className="round-label">{roundName(draw.playerIds.length / 2 ** r)}<span>{matches.length} {matches.length === 1 ? 'match' : 'matches'}</span></div><div className="round-matches">{matches.map((m, i) => renderMatch(m, i))}</div></div>)}
        <div className="champion-column"><div className="round-label">Projected winner<span>One illustrative path</span></div><div className="champion-box"><div className="trophy-circle"><Trophy size={28} strokeWidth={1.4}/></div><span className="eyebrow">PATH FAVORITE</span><h3>{lookup[result.bracket.at(-1)![0].winnerId]?.name}</h3><span className="champion-country">{lookup[result.bracket.at(-1)![0].winnerId]?.country}</span><div className="champion-chance">{percent(result.standings.find(s => s.player.id === result.bracket.at(-1)![0].winnerId)!.titleProbability)}<span>overall title probability</span></div></div></div>
      </div></div>
      <div className="card-footer"><CircleHelp size={14}/><span>This path follows the favorite in each match. The probabilities below account for every possible path.</span></div>
    </section>;
  }
  function outlook() {
    if (!result) return null;
    return <section className="card outlook"><div className="card-title"><div><span className="eyebrow">TITLE PROBABILITIES</span><h2>Who has the edge?</h2></div><BarChart3 size={19}/></div><div className="probability-list">{result.standings.slice(0,5).map((s,i) => <div className="probability-row" key={s.player.id}><div className={`avatar tone-${i%4}`}>{initials(s.player.name)}</div><div className="probability-content"><div><strong>{s.player.name}</strong><span>{percent(s.titleProbability)}</span></div><div className="probability-track"><div style={{ width: `${s.titleProbability * 100}%` }}/></div></div></div>)}</div><button className="text-button card-link" onClick={() => navigate('players')}>Explore the player library <ArrowRight size={15}/></button></section>;
  }
  function earnings() {
    if (!result) return null;
    return <section className="card earnings"><div className="card-title"><div><span className="eyebrow">EXPECTED PRIZE MONEY</span><h2>More than winning.</h2></div><span className="subtle-badge">{result.currency}</span></div><p className="card-description">Every round has value. Here’s the probability-weighted gross payout for each player.</p><div className="table-wrap"><table><thead><tr><th>PLAYER</th><th>TITLE CHANCE</th><th>EXPECTED PRIZE</th></tr></thead><tbody>{result.standings.slice(0,5).map(s => <tr key={s.player.id}><td><span className="country-code">{s.player.country}</span>{s.player.name}</td><td>{percent(s.titleProbability)}</td><td>{money(s.expectedPrize,result.currency)}</td></tr>)}</tbody></table></div><div className="card-footer"><span className="tiny-dot"/>Illustrative payouts · before costs and taxes</div></section>;
  }

  return <div className="app-shell">
    <aside className="sidebar"><a aria-label="CourtVision overview" href="#" className="brand" onClick={e => { e.preventDefault(); navigate('overview'); }}><Ball/><span>courtvision<span className="brand-dot">.</span></span></a><div className="workspace-name"><span className="workspace-mark">CV</span><div>Personal workspace<small>Tennis prediction lab</small></div><PanelLeftClose size={16}/></div><span className="nav-caption">EXPLORE</span><nav aria-label="Main navigation">{nav.map(({ id, label, icon: Icon }) => <button key={id} className={page === id ? 'nav-item active' : 'nav-item'} onClick={() => navigate(id)}><Icon size={18}/>{label}{page === id && <span className="active-dot"/>}</button>)}</nav><span className="nav-caption second-caption">WORKSPACE</span><nav aria-label="Workspace navigation">{workspace.map(({ id, label, icon: Icon }) => <button key={id} className={page === id ? 'nav-item active' : 'nav-item'} onClick={() => navigate(id)}><Icon size={18}/>{label}{id === 'history' && <span className="count-badge">{history.length}</span>}</button>)}</nav><div className="sidebar-note"><div className="sidebar-note-icon"><FlaskConical size={17}/><span>Built on curiosity.</span></div><p>A little tennis. A lot of possibilities. Explore what the numbers say.</p><button onClick={() => navigate('model')}>Meet the model <ArrowRight size={14}/></button></div><div className="sidebar-bottom"><div className="user-avatar">I</div><div>My research space<small>Local development</small></div><span className="tiny-dot"/></div></aside>
    <div className="main-shell"><header className="topbar"><div className="breadcrumb">Workspace<ChevronRight size={14}/><span>{[...nav,...workspace].find(n => n.id === page)?.label}</span></div><div className="topbar-right"><span className={`api-status ${connected ? 'online' : ''}`}><span/>{connected ? 'Local API connected' : 'API disconnected'}</span><span className="topbar-divider"/><span className="version-label">RESEARCH V0.1</span><div className="top-avatar">I</div></div></header>
    <main>
      <div className="page-heading"><div><div className="eyebrow"><span className="tiny-dot"/> THE TENNIS PREDICTION LAB</div><h1>{title}</h1><p>Turn a tennis draw into probabilities, possibilities, and perspective.</p></div><button className="button secondary" onClick={() => { setJson(JSON.stringify(draw,null,2)); setImportError(''); setImportOpen(true); }} disabled={!players.length}><Upload size={16}/> Import draw</button></div>
      {error && <div className="message error" role="alert"><Activity size={18}/><span>{error}</span><button aria-label="Dismiss error" onClick={() => setError('')}><X size={16}/></button></div>}
      {notice && <div className="message success" role="status"><Check size={17}/><span>{notice}</span><button aria-label="Dismiss notification" onClick={() => setNotice('')}><X size={16}/></button></div>}
      {loading && <div className="loading-state"><LoaderCircle className="spin" size={24}/> Connecting to your research workspace…</div>}
      {!loading && !connected && <div className="card empty-state"><Database size={32}/><h2>Start your local backend</h2><p>Run the Spring Boot app, then reload this page to load the player snapshots and draw engine.</p><button className="button primary" onClick={() => window.location.reload()}><RefreshCw size={16}/> Reconnect</button></div>}
      {connected && <>
        {(page === 'overview' || page === 'draw') && <><section className="tournament-banner"><div className="event-copy"><div className="event-meta"><span className="demo-tag">ILLUSTRATIVE EVENT</span><span>Singles · Knockout</span></div><h2>{draw.title}</h2><p><Users size={14}/>{draw.playerIds.length} players<span className="dot-separator">·</span><span className={`surface-dot ${draw.surface.toLowerCase()}`}/>{surfaceLabels[draw.surface]}<span className="dot-separator">·</span>{draw.currency} prize schedule</p></div><div className="court-art" aria-hidden="true"><div className="court-lines"><div/><span/><i/></div><div className="court-ball"/></div><div className="event-controls"><label htmlFor="surface">PLAYING SURFACE</label><select id="surface" value={draw.surface} onChange={e => setDraw({ ...draw, surface: e.target.value as Surface })}><option value="HARD">Hard court</option><option value="CLAY">Clay court</option><option value="GRASS">Grass court</option></select><button className="button primary" onClick={() => void run()} disabled={busy}>{busy ? <LoaderCircle size={16} className="spin"/> : <Play size={14} fill="currentColor"/>}Run prediction</button></div></section>
        {pending && <div className="pending-note">Inputs changed. Run prediction to update the results below.</div>}
        {result && <div className="stats-grid"><div className="stat-card"><span className="stat-label">TITLE FAVORITE<Trophy size={16}/></span><strong>{favorite && shortName(favorite.player.name)}</strong><span className="stat-detail"><b>{favorite && percent(favorite.titleProbability)}</b> chance to take the title</span></div><div className="stat-card"><span className="stat-label">PRIZE POOL<BarChart3 size={16}/></span><strong>{money(totalPayout,result.currency)}</strong><span className="stat-detail">Total across all finishing positions</span></div><div className="stat-card"><span className="stat-label">POSSIBLE OUTCOMES<GitBranch size={16}/></span><strong>{draw.playerIds.length <= 16 ? (2 ** (draw.playerIds.length-1)).toLocaleString() : `2^${draw.playerIds.length-1}`}</strong><span className="stat-detail">Every path included in the calculation</span></div><div className="stat-card model-stat"><span className="stat-label">PREDICTION MODEL<Activity size={16}/></span><strong>Surface Elo<span className="v1-tag">v1</span></strong><span className="stat-detail"><span className="tiny-dot"/>Transparent, illustrative baseline</span></div></div>}
        {bracket()}
        {page === 'overview' ? <div className="bottom-grid">{outlook()}{earnings()}</div> : <section className="card advancement-card"><div className="card-title"><div><span className="eyebrow">ALL POSSIBLE PATHS</span><h2>Round-by-round outlook</h2></div><button className="text-button" onClick={download}><ArrowDownToLine size={15}/> Export draw</button></div><div className="table-wrap"><table><thead><tr><th>PLAYER</th>{result?.bracket.map((_,i) => <th key={i}>{i === result.bracket.length-1 ? 'TITLE' : `WIN ROUND ${i+1}`}</th>)}<th>EXPECTED PRIZE</th></tr></thead><tbody>{result?.standings.map(s => <tr key={s.player.id}><td><span className="country-code">{s.player.country}</span>{s.player.name}</td>{s.advancement.map((p,i) => <td key={i}>{percent(p)}</td>)}<td>{money(s.expectedPrize,result!.currency)}</td></tr>)}</tbody></table></div></section>}</>}
        {page === 'match' && <section className="card lab-card"><div className="card-title"><div><span className="eyebrow">HEAD TO HEAD</span><h2>Set up your matchup</h2></div><FlaskConical size={22}/></div><div className="match-inputs"><label>FIRST PLAYER<select value={first} onChange={e => { setFirst(e.target.value); setPrediction(null); }}>{players.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</select></label><span className="versus">VS</span><label>SECOND PLAYER<select value={second} onChange={e => { setSecond(e.target.value); setPrediction(null); }}>{players.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</select></label><label>SURFACE<select value={draw.surface} onChange={e => { setDraw({ ...draw, surface: e.target.value as Surface }); setPrediction(null); }}><option value="HARD">Hard court</option><option value="CLAY">Clay court</option><option value="GRASS">Grass court</option></select></label></div><button className="button primary" disabled={busy || first === second} onClick={() => void compare()}><FlaskConical size={16}/> Compare players</button>{first === second && <p className="inline-hint">Choose two different players.</p>}{prediction ? <div className="prediction-output"><div className="prediction-sides"><div><span>{prediction.first.name}</span><strong>{percent(prediction.firstProbability)}</strong></div><div><span>{prediction.second.name}</span><strong>{percent(1-prediction.firstProbability)}</strong></div></div><div className="head-to-head-bar"><span style={{ width: `${prediction.firstProbability * 100}%` }}/></div><p>{prediction.ratingDifference > 0 ? '+' : ''}{prediction.ratingDifference} surface Elo difference · {surfaceLabels[draw.surface]} · illustrative inputs</p></div> : <div className="lab-placeholder"><Ball/><p>One matchup. A clearer view of the odds.</p><span>Compare two illustrative player ratings.</span></div>}</section>}
        {page === 'players' && <section className="card"><div className="card-title"><div><span className="eyebrow">ILLUSTRATIVE SNAPSHOTS</span><h2>The player library <span className="muted-count">{players.length}</span></h2></div><input className="search" aria-label="Search players" placeholder="Find a player…" value={query} onChange={e => setQuery(e.target.value)}/></div><div className="table-wrap"><table><thead><tr><th>PLAYER</th><th>COUNTRY</th><th>HARD ELO</th><th>CLAY ELO</th><th>GRASS ELO</th><th>IMPORT ID</th></tr></thead><tbody>{players.filter(p => `${p.name} ${p.country}`.toLowerCase().includes(query.toLowerCase())).map(p => <tr key={p.id}><td><div className="player-cell"><span className="avatar tone-0">{initials(p.name)}</span><strong>{p.name}</strong></div></td><td>{p.country}</td><td>{p.hardElo}</td><td>{p.clayElo}</td><td>{p.grassElo}</td><td><code>{p.id}</code></td></tr>)}</tbody></table></div><div className="card-footer"><CircleHelp size={14}/>These are authored demo ratings, not current ATP rankings or trained model outputs.</div></section>}
        {page === 'model' && <div className="model-grid"><section className="card prose-card"><span className="eyebrow">MODEL CARD / 001</span><h2>Simple enough to explain.<br/>Useful enough to build on.</h2><p>The first model uses surface-specific Elo ratings. A higher rating increases a player’s win probability; an equal rating means a 50–50 match.</p><div className="formula">P(A wins) = 1 / (1 + 10<sup>(EloB − EloA) / 400</sup>)</div><h3>Every route matters</h3><p>The draw engine sums over all possible opponents in each round. It produces exact probabilities under fixed ratings and independent match outcomes, without sampling noise.</p><h3>Money is an expectation</h3><p>Each exit probability is multiplied by the total prize for that finishing position. Add them up for expected gross prize money, before costs, taxes or sponsorship.</p><h3>Honest limitations</h3><p>Ratings are illustrative. No injury, fatigue or live form is included. There is no measured accuracy for this demo, and no trained ML or DL model serves predictions yet. All players must participate; byes and reseeding are not supported.</p></section><section className="card roadmap-card"><span className="eyebrow">THE RESEARCH ROADMAP</span><h2>From baseline to evidence.</h2>{[{ n:'01', title:'Surface Elo baseline', text:'Working API, exact draw engine and persisted runs.', done:true },{ n:'02',title:'Historical evaluation',text:'Python workflow: time splits, data provenance and proper scores.',done:true},{n:'03',title:'Calibrated machine learning',text:'Validate logistic regression and richer pre-match features.',done:false},{n:'04',title:'Deep learning experiment',text:'Test a small MLP against the same holdout. Keep it only if useful.',done:false}].map(s => <div className="roadmap-step" key={s.n}><span className={s.done ? 'step-number done' : 'step-number'}>{s.done ? <Check size={15}/> : s.n}</span><div><h3>{s.title}</h3><p>{s.text}</p></div></div>)}<div className="model-note"><BookOpen size={18}/><p>Explore the model card, decisions and Polish learning guide in the repository’s <code>docs/</code> folder.</p></div></section></div>}
        {page === 'history' && <section className="card"><div className="card-title"><div><span className="eyebrow">PERSISTED IN SQL</span><h2>Your latest runs</h2></div><span className="subtle-badge">Last 20</span></div>{history.length ? <div className="saved-runs">{history.map(h => <button className="saved-run" disabled={busy} key={h.id} onClick={() => void loadRun(h.id)}><span className="saved-icon"><GitBranch size={19}/></span><span><strong>{h.title}</strong><small>{surfaceLabels[h.surface]} · {new Date(h.createdAt).toLocaleString()}</small></span><ArrowRight size={17}/></button>)}</div> : <div className="empty-state"><Clock3 size={30}/><h3>A fresh notebook.</h3><p>Run a prediction to save your first result here.</p><button className="button secondary" onClick={() => navigate('overview')}>Explore a draw <ArrowRight size={15}/></button></div>}</section>}
        {page === 'api' && <section className="card integration-card"><div className="card-title"><div><span className="eyebrow">EXTERNAL DATA</span><h2>API Tennis</h2></div><span className={`connection-badge ${provider ? 'configured' : ''}`}><span className="tiny-dot"/>{provider ? 'Key configured' : 'Not connected'}</span></div><p className="card-description">Fetch a real fixture list through the backend. Your provider key stays on the server.</p><div className="integration-summary"><Radio size={23}/><div><strong>{provider ? 'Your API key is configured.' : 'Bring your own provider key.'}</strong><p>{provider ? 'Access depends on your provider subscription and availability.' : 'Set TENNIS_API_KEY in the backend environment and restart Spring Boot. A provider subscription or trial is required.'}</p></div></div><div className="fixture-controls"><label htmlFor="fixture-date">FIXTURE DATE<input id="fixture-date" type="date" value={fixtureDate} onChange={e => setFixtureDate(e.target.value)}/></label><button className="button primary" disabled={busy || !provider || !fixtureDate} onClick={() => void fetchFixtures()}>{busy ? <LoaderCircle className="spin" size={16}/> : <RefreshCw size={16}/>}Fetch fixtures</button><a className="text-button" href="https://api-tennis.com/documentation" target="_blank" rel="noreferrer">Provider documentation <ArrowRight size={15}/></a></div>{fixtures && (fixtures.length ? <div className="table-wrap"><table><thead><tr><th>TIME (WARSAW)</th><th>MATCH</th><th>TOURNAMENT</th><th>STATUS</th></tr></thead><tbody>{fixtures.map((f,i) => <tr key={`${f.id}-${i}`}><td>{f.time || 'TBA'}</td><td>{f.first} vs {f.second}</td><td>{f.tournament}</td><td>{f.status || 'Scheduled'}</td></tr>)}</tbody></table><p className="inline-hint">Showing up to 100 fixtures returned by the provider.</p></div> : <div className="empty-state">No fixtures returned for this date and subscription.</div>)}<div className="card-footer"><CircleHelp size={14}/>Provider players need explicit ID mapping before they can be used for predictions.</div></section>}
      </>}
      <footer className="page-footer"><span><Ball small/>Made for the love of the game. Built for the questions.</span><span>Illustrative data <span className="footer-separator">/</span> Surface Elo v1</span></footer>
    </main></div>
    {importOpen && <div className="modal-backdrop" onClick={() => !busy && setImportOpen(false)}><section className="import-modal" role="dialog" aria-modal="true" aria-labelledby="import-title" onKeyDown={e => { if(e.key === 'Escape' && !busy) setImportOpen(false); }} onClick={e => e.stopPropagation()}><div className="modal-heading"><div><span className="eyebrow">BRING YOUR OWN DRAW</span><h2 id="import-title">A new set of possibilities.</h2></div><button className="icon-button" aria-label="Close import" disabled={busy} onClick={() => setImportOpen(false)}><X size={20}/></button></div><p>Upload a JSON file or edit the draw below. Player IDs follow bracket order: positions 1–2 play first, then 3–4, and so on. Payouts are total prizes from first-round exit to champion.</p><div className="import-actions"><button className="button secondary" onClick={() => fileInput.current?.click()}><Upload size={15}/>Choose JSON file</button><button className="text-button" onClick={download}><ArrowDownToLine size={15}/>Download example</button></div><input ref={fileInput} type="file" accept=".json,application/json" className="hidden" onChange={async e => { const file = e.target.files?.[0]; if (!file) return; if(file.size > 65536) { setImportError('The draw file must be smaller than 64 KB.'); return; } try { setJson(await file.text()); setImportError(''); } catch { setImportError('Could not read this file.'); } }}/><label className="sr-only" htmlFor="draw-json">Draw JSON</label><textarea id="draw-json" value={json} spellCheck={false} onChange={e => setJson(e.target.value)} autoFocus/>{importError && <div className="message error" role="alert">{importError}</div>}<div className="modal-footer"><span>{players.length} known players · 2–64 slots</span><button className="button primary" onClick={() => void importDraw()} disabled={busy}>{busy ? <LoaderCircle size={15} className="spin"/> : <Plus size={16}/>}Import & preview</button></div></section></div>}
  </div>;
}

export default App;
