# Jak uczyć się na CourtVision

Nie próbuj od razu rozumieć całego repo. Przechodź po jednej pionowej ścieżce: kliknięcie → żądanie → logika → baza → odpowiedź.

## 1. Uruchom i zmień jedną rzecz

Uruchom frontend i backend. Zmień tytuł aplikacji. W narzędziach przeglądarki znajdź `GET /api/players` i `POST /api/simulations`. Wyjaśnij własnymi słowami JSON, HTTP 200, HTTP 400 i proxy.

**Gotowe, gdy:** potrafisz prześledzić jedno kliknięcie bez zgadywania, skąd pochodzą dane.

## 2. Java, Spring Boot i testowanie

Przeczytaj `PredictionService`, potem `DrawService`, na końcu kontroler. Policz ręcznie szansę wygranej przy różnicy Elo 100. Dodaj test, że przy dwóch równych zawodnikach każdy ma 50%.

**Gotowe, gdy:** umiesz wytłumaczyć dependency injection, DTO, walidację i różnicę między testem jednostkowym a integracyjnym.

## 3. SQL i trwałość

Zapisz turniej, odśwież aplikację i załaduj wynik z historii. Przeczytaj migrację Flyway. Przełącz H2 na PostgreSQL. Zapytaniem SQL policz liczbę symulacji według nawierzchni.

**Gotowe, gdy:** umiesz wyjaśnić klucz główny, indeks, migrację i dlaczego sekretów nie zapisujemy w Git.

## 4. Prawdopodobieństwo i pieniądze

Wczytaj drabinkę czterech równych graczy. Każdy powinien mieć 25% na tytuł. Zmień wypłaty i policz ręcznie wartość oczekiwaną. Zauważ, że faworyt meczu nie musi być pewnym zwycięzcą turnieju.

**Gotowe, gdy:** rozróżniasz prawdopodobieństwo warunkowe, szansę awansu, kalibrację, dokładność i wartość oczekiwaną.

## 5. Zewnętrzne API

Przeczytaj dokumentację API Tennis. Ustaw `TENNIS_API_KEY` w środowisku backendu i pobierz terminarz. Obsłuż brak klucza, timeout i limit dostawcy. Klucza nie wklejaj do frontendu ani README.

**Gotowe, gdy:** rozumiesz adapter dostawcy, timeout, status 502/503 i ograniczenia płatnego planu. Identyfikatory dostawcy i lokalnych zawodników wymagają osobnej mapy — nie łącz ich automatycznie po podobnym nazwisku.

## 6. DS i ML na prawdziwych danych

Przeczytaj `research/README.md`. Pobierz historyczny zbiór zgodnie z licencją. Uruchom ewaluację. Porównaj 50%, Elo i regresję logistyczną na późniejszym okresie. Zbadaj brakujące dane oraz wrażliwość wyniku na podział czasowy.

**Gotowe, gdy:** potrafisz wskazać data leakage, uzasadnić podział czasowy i powiedzieć, dlaczego wynik na zbiorze treningowym nie jest dowodem jakości.

## 7. DL jako sprawdzana hipoteza

Dopiero po solidnej regresji dodaj mały MLP w PyTorch. Zamroź preprocessing na treningu, dobieraj hiperparametry na walidacji, raportuj ostateczny test jednokrotnie. Zachowaj także wyniki, w których sieć przegrywa z Elo.

**Gotowe, gdy:** umiesz obronić wybór modelu i jego koszty, a nie tylko listę użytych bibliotek.

## Jak opowiedzieć o projekcie na rozmowie

Przygotuj 90-sekundowe demo: problem → dane → decyzja projektowa → test → ograniczenie → następny eksperyment. Pokaż konkretny błąd, który wykrył test, i decyzję opisaną w ADR. Nie deklaruj ML/DL w CV jako wdrożonych, dopóki rzeczywiście nie działają na danych.

## Rytm commitów

Mały ukończony etap → odpowiednie testy → commit opisujący zmianę → push. Nie commituj każdego zapisu pliku. Nigdy nie commituj kluczy API, `.env`, bazy ani pobranych danych bez sprawdzenia licencji.
