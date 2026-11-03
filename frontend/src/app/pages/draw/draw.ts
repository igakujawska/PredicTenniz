import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EMPTY, Subject, catchError, debounceTime, finalize, forkJoin, of, switchMap } from 'rxjs';
import { Api, apiError } from '../../core/api';
import type {
  Draw,
  DrawRequest,
  DrawSummary,
  Forecast,
  ModelInfo,
  Player,
  TournamentFormatInfo,
} from '../../core/contracts';
import { parseDrawImport, seedSlots, validateSlots } from '../../core/draw-files';
import { Bracket } from '../../shared/bracket';

@Component({
  imports: [CommonModule, FormsModule, Bracket],
  templateUrl: './draw.html',
  styleUrl: './draw.css',
})
export class DrawPage {
  private readonly api = inject(Api);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly changes = new Subject<void>();
  readonly players = signal<Player[]>([]);
  readonly models = signal<ModelInfo[]>([]);
  readonly savedDraws = signal<DrawSummary[]>([]);
  readonly saved = signal<Draw | null>(null);
  readonly forecast = signal<Forecast | null>(null);
  readonly error = signal('');
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly dirty = signal(true);
  readonly size = signal(32);
  readonly page = signal(0);
  readonly formats = signal<TournamentFormatInfo[]>([]);
  readonly selectedFormat = () =>
    this.formats().find((format) => format.code === (this.form.formatCode || 'CUSTOM'));
  readonly slots = computed(() =>
    Array.from(
      { length: Math.min(16, this.size() - this.page() * 16) },
      (_, index) => this.page() * 16 + index,
    ),
  );
  readonly pickerSlot = signal<number | null>(null);
  readonly playerQuery = signal('');
  readonly pickerPlayers = computed(() =>
    this.players()
      .filter((player) =>
        player.name
          .toLocaleLowerCase('pl')
          .includes(this.playerQuery().trim().toLocaleLowerCase('pl')),
      )
      .slice(0, 40),
  );
  readonly playerMap = computed(() => new Map(this.players().map((player) => [player.id, player])));
  readonly today = new Date(Date.now() - new Date().getTimezoneOffset() * 60_000)
    .toISOString()
    .slice(0, 10);
  form: DrawRequest = {
    title: 'Moja symulacja ATP',
    formatCode: 'ATP250_32',
    asOf: this.today,
    playerIds: [],
    surface: 'HARD',
    bestOf: 3,
    modelVersion: 'ranking-baseline-v1',
  };

  constructor() {
    this.changes
      .pipe(
        debounceTime(180),
        switchMap(() => {
          try {
            const body = this.request();
            this.loading.set(true);
            this.error.set('');
            return this.api.preview(body).pipe(
              catchError((error) => {
                this.error.set(apiError(error));
                this.forecast.set(null);
                return EMPTY;
              }),
              finalize(() => this.loading.set(false)),
            );
          } catch (error) {
            this.error.set(apiError(error));
            this.forecast.set(null);
            return EMPTY;
          }
        }),
        takeUntilDestroyed(),
      )
      .subscribe((draw) => this.forecast.set(draw.forecast));
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set('');
    forkJoin({
      catalog: this.api.players(),
      models: this.api.models().pipe(
        catchError((error) => {
          this.error.set(apiError(error));
          return of([]);
        }),
      ),
      saved: this.api.draws(),
      formats: this.api.formats(),
    })
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ({ catalog, models, saved, formats }) => {
          this.formats.set(formats);
          this.players.set(catalog.players);
          this.models.set(models);
          this.savedDraws.set(saved);
          this.form.asOf = catalog.latestRankingDate || this.today;
          const selected = models
            .filter(
              (model) =>
                model.trained && (!model.trainedThrough || model.trainedThrough <= this.form.asOf),
            )
            .at(-1);
          this.form.modelVersion = selected?.version || 'ranking-baseline-v1';
          const id = this.route.snapshot.queryParamMap.get('draw');
          if (id) this.loadSaved(id);
          else this.applyFormat(this.form.formatCode || 'CUSTOM');
        },
        error: (error) => this.error.set(apiError(error)),
      });
  }

  changed() {
    this.dirty.set(true);
    if (this.models().length) this.changes.next();
  }
  resize(size: number) {
    this.size.set(Number(size));
    this.page.set(0);
    try {
      this.form.playerIds = seedSlots(
        this.players().map((player) => player.id),
        this.size(),
        this.selectedFormat()?.entrants || this.size(),
      );
      this.changed();
    } catch (error) {
      this.forecast.set(null);
      this.error.set(apiError(error));
    }
  }
  applyFormat(code: string) {
    this.form.formatCode = code;
    const format = this.formats().find((item) => item.code === code);
    this.form.bestOf = format?.bestOf || 3;
    this.resize(format?.drawSize || 8);
  }
  openPicker(index: number, dialog: HTMLDialogElement) {
    this.pickerSlot.set(index);
    this.playerQuery.set('');
    dialog.showModal();
    dialog.querySelector('input')?.focus();
  }
  pick(id: number, dialog: HTMLDialogElement) {
    const index = this.pickerSlot();
    if (index === null) return;
    const other = this.form.playerIds.indexOf(id);
    if (other >= 0) this.form.playerIds[other] = this.form.playerIds[index];
    this.form.playerIds[index] = id;
    dialog.close();
    this.changed();
  }
  slotName(index: number) {
    const id = this.form.playerIds[index];
    const player = id ? this.playerMap().get(id) : null;
    return player ? player.position + '. ' + player.name : 'Wolny los';
  }
  move(index: number, direction: number) {
    const other = index + direction;
    if (other < 0 || other >= this.form.playerIds.length) return;
    [this.form.playerIds[index], this.form.playerIds[other]] = [
      this.form.playerIds[other],
      this.form.playerIds[index],
    ];
    this.changed();
  }
  shuffle() {
    const selected = new Set(this.form.playerIds);
    const fixed = new Set(
      this.players()
        .filter((player) => selected.has(player.id))
        .slice(0, this.selectedFormat()?.seeds || 0)
        .map((player) => player.id),
    );
    const movable = this.form.playerIds
      .map((id, index) => ({ id, index }))
      .filter((item) => item.id !== null && !fixed.has(item.id));
    const values = movable.map((item) => item.id);
    for (let i = values.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [values[i], values[j]] = [values[j], values[i]];
    }
    movable.forEach((item, index) => (this.form.playerIds[item.index] = values[index]));
    this.changed();
  }
  setDate(date: string) {
    this.form.asOf = date;
    this.api
      .players(date)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (catalog) => {
          this.players.set(catalog.players);
          this.resize(this.size());
        },
        error: (error) => this.error.set(apiError(error)),
      });
  }

  private request(): DrawRequest {
    if (!this.form.title.trim() || this.form.title.trim().length > 100)
      throw new Error('Podaj nazwę turnieju do 100 znaków.');
    if (!this.form.asOf || this.form.asOf > this.today)
      throw new Error('Wybierz datę danych nie późniejszą niż dzisiaj.');
    validateSlots(this.form.playerIds);
    const format = this.selectedFormat();
    if (
      format &&
      format.code !== 'CUSTOM' &&
      (this.form.playerIds.length !== format.drawSize ||
        this.form.playerIds.filter((id) => id !== null).length !== format.entrants)
    )
      throw new Error('Liczba graczy i wolnych losów nie pasuje do formatu.');
    return {
      ...this.form,
      title: this.form.title.trim(),
      playerIds: [...this.form.playerIds],
      version: this.saved()?.version ?? 0,
    };
  }

  save(copy = false) {
    try {
      const request = this.request();
      this.saving.set(true);
      this.error.set('');
      const id = this.saved()?.id;
      const operation = id && !copy ? this.api.update(id, request) : this.api.create(request);
      operation
        .pipe(
          finalize(() => this.saving.set(false)),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe({
          next: (draw) => {
            this.saved.set(draw);
            this.forecast.set(draw.forecast);
            this.dirty.set(false);
            this.router.navigate(['/draw'], { queryParams: { draw: draw.id }, replaceUrl: true });
            this.api.draws().subscribe((items) => this.savedDraws.set(items));
          },
          error: (error) => this.error.set(apiError(error)),
        });
    } catch (error) {
      this.error.set(apiError(error));
    }
  }

  loadSaved(id: string) {
    if (!id) {
      this.newDraw();
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.api
      .draw(id)
      .pipe(
        switchMap((draw) =>
          this.api.players(draw.asOf).pipe(switchMap((catalog) => of({ draw, catalog }))),
        ),
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ({ draw, catalog }) => {
          this.players.set(catalog.players);
          this.saved.set(draw);
          this.forecast.set(draw.forecast);
          this.dirty.set(false);
          const ids = draw.playerIds;
          this.form = {
            formatCode: draw.formatCode || 'CUSTOM',
            title: draw.title,
            asOf: draw.asOf,
            playerIds: [...ids],
            surface: draw.surface || draw.forecast?.surface || 'HARD',
            bestOf: draw.bestOf || draw.forecast?.bestOf || 3,
            modelVersion: draw.modelVersion || draw.forecast?.modelVersion || 'ranking-baseline-v1',
          };
          this.size.set(ids.length);
          this.page.set(0);
          if (draw.predictionError) this.error.set(draw.predictionError);
          this.router.navigate(['/draw'], { queryParams: { draw: id }, replaceUrl: true });
        },
        error: (error) => this.error.set(apiError(error)),
      });
  }
  newDraw() {
    this.saved.set(null);
    this.form.title = 'Mój turniej ATP';
    this.router.navigate(['/draw'], { replaceUrl: true });
    this.changed();
  }

  async importFile(event: Event) {
    const input = event.target as HTMLInputElement;
    try {
      const file = input.files?.[0];
      if (!file) return;
      if (file.size > 100_000) throw new Error('Wybierz plik JSON do 100 KB.');
      const definition = parseDrawImport(
        JSON.parse(await file.text()),
        this.models().map((model) => model.version),
        this.today,
      );
      this.api
        .players(definition.asOf)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (catalog) => {
            if (
              definition.playerIds.some(
                (id) => id !== null && !catalog.players.some((player) => player.id === id),
              )
            ) {
              this.error.set('Niektórzy zawodnicy z pliku nie mają rankingów dla wybranej daty.');
              return;
            }
            this.players.set(catalog.players);
            this.saved.set(null);
            this.form = definition;
            this.size.set(definition.playerIds.length);
            this.page.set(0);
            this.router.navigate(['/draw'], { replaceUrl: true });
            this.changed();
          },
          error: (error) => this.error.set(apiError(error)),
        });
    } catch (error) {
      this.error.set(error instanceof SyntaxError ? 'Nieprawidłowy plik JSON.' : apiError(error));
    } finally {
      input.value = '';
    }
  }
  exportFile() {
    try {
      const url = URL.createObjectURL(
        new Blob([JSON.stringify({ ...this.request(), version: undefined }, null, 2)], {
          type: 'application/json',
        }),
      );
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = 'predictteniz-drabinka.json';
      anchor.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (error) {
      this.error.set(apiError(error));
    }
  }
  modelLabel() {
    return (
      this.models().find((model) => model.version === this.form.modelVersion)?.label ||
      'Wybrany model'
    );
  }
}
