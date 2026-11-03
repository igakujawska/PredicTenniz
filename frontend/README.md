# Angular — interfejs PredictTeniz

Samodzielna aplikacja. Node.js 24.15+ w linii 24; polecenia w tym folderze:

```powershell
npm ci
npm start
```

Strona: http://localhost:4200. `/api` jest przekazywane do Springa na 127.0.0.1:8080 przez `proxy.conf.json`. Python nie jest wywoływany bezpośrednio z przeglądarki. Zatrzymanie frontendu nie zatrzymuje Javy ani Pythona.

Widoki: `/draw` — drabinka, `/match` — porównanie modeli meczu, `/players` — katalog, `/models` — wyniki oceny. Motyw jasny/ciemny zapisuje się w przeglądarce. Import JSON zawiera definicję drabinki i identyfikatory istniejących zawodników; nie importuje profili ani wyników modelu.

`npm test` sprawdza walidację importu, `npm run build` weryfikuje TypeScript i szablony oraz buduje wersję statyczną. Dockerfile buduje Angulara, a Nginx serwuje wynik i przekazuje `/api` do kontenera backend. Instrukcja wspólnego uruchomienia znajduje się w głównym README.

Turniej obsługuje 2–128 miejsc, formaty ATP 250/500, Masters i szlemowy oraz jawne wolne losy. W dużych drabinkach wybierz rundę zakładką. Obsada jest stronicowana po 16 miejsc; kliknięcie zawodnika otwiera wyszukiwanie wśród 500 profili. Wybór zawodnika już obecnego w siatce zamienia go miejscami. Własne ustawienia są symulacją, nie oficjalnym losowaniem ATP. Zakładka Modele prezentuje XGBoost, metryki na wspólnym teście, znaczenie cech i wyniki według kategorii.
