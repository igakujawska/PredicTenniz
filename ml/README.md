# Python: modele i historia wyników

`train.py` jest odtwarzalnym eksperymentem treningowym, a `service/` niezależną aplikacją FastAPI wykonującą modele. Spring Boot pyta tę usługę przez HTTP i zajmuje się obliczeniami drabinki oraz zapisem. Model jest przechowywany wyłącznie po stronie Pythona.

```text
CSV → ml/train.py → zatwierdzony model w ml/artifacts → FastAPI → Spring Boot → Angular
```

Uruchomienie API, healthchecki i kontrakt: [service/README.md](service/README.md).

## Uruchomienie lokalne

Sprawdzone na Pythonie 3.12. Polecenia z głównego folderu repozytorium, PowerShell:

```powershell
py -3.12 -m venv .venv
./.venv/Scripts/python.exe -m pip install -r ml/requirements.txt
./.venv/Scripts/python.exe ml/download_matches.py
./.venv/Scripts/python.exe -m unittest discover -s ml -p 'test_*.py'
./.venv/Scripts/python.exe ml/train.py
```

Na macOS/Linux użyj `python3 -m venv .venv` i `.venv/bin/python`. Trening zapisuje wynik w `target/ml/rank-logistic-v1.json`. Nie nadpisuje modelu aplikacji. Wbudowany artefakt jest w `ml/artifacts/rank-logistic-v1.json`; wprowadzenie innych danych lub cech powinno otrzymać nową wersję modelu po przeglądzie wyników.

`.venv`, pobrane CSV i robocze wyniki są ignorowane przez Git. Do wykonywania gotowego modelu wystarczą zależności `requirements-api.txt`. Biblioteki treningowe są potrzebne tylko do eksperymentów.

## Co oznacza tu uczenie

Model bazowy ma ustalony wzór: `P(A) = sigmoid(0.5 * log(rankB/rankA))`. Model ML uczy czterech współczynników z wyników meczów:

1. różnica logarytmów rankingu na hard;
2. ta sama różnica na clay;
3. ta sama różnica na grass;
4. dodatkowa korekta dla formatu best of 5.

Dla innych nawierzchni odpowiadające im cechy wynoszą zero. Wynik to `sigmoid(suma cecha * współczynnik)`. Nawierzchnia zmienia sposób przeliczania różnicy rankingu, a nie ocenę konkretnego tenisisty na tym korcie. Model nie zna jeszcze jego formy, serwisu, kontuzji ani indywidualnej historii na nawierzchni.

Nie uczymy wyrazu wolnego. Każdy mecz ma dwie orientacje: `(x, 1)` i `(-x, 0)`, żeby układ winner/loser w CSV nie oznaczał automatycznie wygranej gracza A. Zamiana stron daje dopełniające się szanse. Liczby meczów w raporcie nie są przez to podwajane.

## Podział chronologiczny

- Początkowy trening: starsze mecze z lat 2021–2023.
- Walidacja: rok 2024. Wyłącznie na niej wybieramy `C` spośród 0.1, 1 i 10.
- Końcowy trening: dane sprzed roku testowego, z wybranym `C`.
- Osobny test: rok 2025. Nie wybieramy parametrów według jego wyniku.

Źródło zawiera datę rozpoczęcia turnieju, nie dokładny czas rozegrania meczu. Przed granicami sezonów pomijamy wydarzenia rozpoczynające się mniej niż 21 dni wcześniej, aby ograniczyć przenikanie wyników turniejów zachodzących na następny okres. To jawne założenie o dostępności danych, a nie potwierdzona godzina publikacji wyników. Pola rankingu traktujemy jako dane przedmeczowe z archiwum. W produkcji potrzebne są znaczniki rzeczywistej dostępności danych.

Importer weryfikuje SHA-256 plików, kolumny, duplikaty i spójność identyfikatorów. Odrzuca m.in. nieukończone mecze, walkowery, brakujące rankingi oraz rozgrywki poza wybranym głównym tourem. Liczby wykluczeń są zapisane w `dataQuality` artefaktu. Test nie obejmuje wszystkich możliwych meczów ATP.

## Wynik zapisanego eksperymentu

Końcowy trening: **10 435 meczów**. Osobny test: **2483 mecze z 2025 roku**.

| Model | Log loss ↓ | Brier score ↓ | Trafność typów |
|---|---:|---:|---:|
| Zawsze 50% | 0.6931 | 0.2500 | 50.0% |
| Wzór rankingowy | 0.6276 | 0.2192 | 64.9% |
| Regresja logistyczna ML | 0.6266 | 0.2189 | 64.9% |

ML nie zwiększył tutaj trafności wyboru faworyta; nieznacznie zmienił jakość prawdopodobieństw. Nie badaliśmy istotności tej różnicy. Nie jest to dowód na skuteczność w bieżącym sezonie ani na każdym rodzaju kortu.

Log loss mocno karze pewne, błędne prognozy. Brier score mierzy średni kwadrat błędu prawdopodobieństwa. Artefakt zawiera też przedziały kalibracji: porównanie przewidywanego procentu z odsetkiem zwycięstw w grupie podobnych prognoz.

## XGBoost z historią: form-xgboost-v2

`service/test_service.py` porównuje odpowiedzi z wartościami kontrolnymi artefaktu i sprawdza kontrakt HTTP. FastAPI odrzuca analizę wcześniejszą niż zadeklarowany koniec treningu. Java nie zawiera już kopii współczynników ani implementacji regresji.

`history_features.py` przygotowuje cechy: Elo ogólne i nawierzchniowe, formę 90 dni, formę nawierzchniową 180 dni, wyniki i liczbę meczów 365 dni oraz H2H. Dane historyczne można pobrać przez `python -m ml.history_features --download`. Wyniki są udostępniane dopiero 21 dni po dacie rozpoczęcia turnieju. To konserwatywne założenie badawcze, a nie rzeczywisty czas publikacji. Testy sprawdzają brak użycia przyszłych wyników, niezależność od kolejności rund i zamianę stron.

Te cechy są już wykorzystywane w API przez `form-xgboost-v2`. Model ma 15 wejść: osiem różnic pomiędzy graczami oraz warunki meczu i wskaźniki kategorii turnieju. Parametry trzech z góry ustalonych wariantów wybieramy wyłącznie na sezonie 2024. Następnie uczymy wybrany wariant na 10 166 meczach dostępnych przed rokiem 2025. Dane 2018–2020 służą do rozgrzania historii, nie do doboru parametrów na teście. Historia do obsługi zapytań obejmuje 20 342 mecze.

Odtworzenie z głównego folderu:

```powershell
./.venv/Scripts/python.exe -m pip install -r ml/requirements.txt
./.venv/Scripts/python.exe -m ml.history_features --download
./.venv/Scripts/python.exe -m ml.train_form
```

Wyniki trafiają do `target/ml/form-v2/`. Model aplikacji nie jest automatycznie nadpisywany. Zatwierdzone pliki w `ml/artifacts` to model UBJ, metadane i skompresowana historia. API sprawdza ich SHA-256 i odtwarza stan historii wyłącznie do wskazanej daty. Obsługiwane okno predykcji v2: 2024-12-31–2026-06-08. Model wymaga identyfikatorów źródłowych graczy.

| Metoda na wspólnych 2474 meczach testowych | Log loss ↓ | Brier ↓ | Trafność |
|---|---:|---:|---:|
| XGBoost: Elo i forma | 0.6158 | 0.2142 | 64.63% |
| Poprzednia regresja, oceniona ponownie | 0.6263 | 0.2187 | 65.00% |
| Ranking | 0.6274 | 0.2191 | 65.00% |
| Samo Elo | 0.6271 | 0.2190 | 63.70% |

Wersja v2 wyklucza dodatkowo wydarzenia drużynowe, Olympics i NextGen, stąd inny rozmiar testu niż w v1. [Mapowanie kategorii i zmiany w czasie](CATEGORIES.md) opisuje m.in. awanse Dallas, Doha i Munich od 2025 oraz wyjątek Astany w 2022. Testy XGBoost i starej regresji w powyższej tabeli dotyczą dokładnie tych samych meczów.

Cechy meczów z sezonu testowego mogą korzystać z wcześniejszych wyników tego samego sezonu, jeżeli stały się dostępne przed analizowanym meczem. Wagi modelu pozostają zamrożone; jest to odtwarzanie kolejnych prognoz w czasie.

Mirroring w treningu nie gwarantuje symetrii drzew. Dlatego serwowanie jawnie stosuje `P(A,B) = (f(A,B) + 1 - f(B,A))/2`; zamiana stron daje dopełnienie. Paczka maksymalnie 8128 par jest przetwarzana wektorowo. Historia jest buforowana osobno dla dat zapytań.

Raport zawiera kalibrację, wyniki według kategorii, udziały gain cech i sparowany bootstrap różnicy log loss. Ten ostatni zakłada niezależność meczów; powiązania graczy i turniejów ograniczają jego interpretację. Nie stanowi potwierdzenia przewagi na przyszłych sezonach. Gain nie jest wpływem przyczynowym ani wyjaśnieniem pojedynczej prognozy. Historia obejmuje główny ATP Tour; dla wielu słabiej notowanych graczy jest uboga, a neutralne wartości nie zastępują brakujących wyników Challengerów.

## Źródła i następne ćwiczenie

Dane: Jeff Sackmann, przez [wersjonowane archiwum](https://github.com/Aneeshers/tennis-sackmann-archive/tree/83733587353df8a41f2fd4f516147d5aa83f5a8d), **CC BY-NC-SA 4.0**. Każdy plik ma URL i sumę SHA-256 w manifeście, który trafia do modelu. Dokumentacja algorytmu: [scikit-learn LogisticRegression](https://scikit-learn.org/stable/modules/generated/sklearn.linear_model.LogisticRegression.html).

Następne badania: walidacja na kolejnych oknach czasowych, kalibracja prawdopodobieństw i poszerzenie historii o niższe poziomy rozgrywek. Sieć neuronowa ma sens jako kolejny porównywany model, z identycznymi danymi i podziałem czasowym.
