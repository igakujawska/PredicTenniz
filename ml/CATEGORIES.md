# Kategorie turniejów w danych ML

`tourney_level=A` nie odróżnia ATP 250 od ATP 500. `tournament_categories.py` używa identyfikatorów archiwum oraz roku wydarzenia. Stałe ATP 500: Rotterdam, Acapulco, Dubai, Barcelona, Halle, Queen’s, Hamburg, Washington, Beijing, Tokyo, Vienna, Basel, Rio. Dallas, Doha i Munich są ATP 500 od 2025, a Astana była ATP 500 tylko w 2022 w analizowanym okresie.

Źródła pierwotne:
- [ATP: awans Dallas, Doha i Munich od 2025](https://www.atptour.com/en/news/atp-500-upgrades-from-2025).
- [ATP: kalendarz 2026](https://www.atptour.com/en/news/what-is-the-2026-atp-tour-calendar/).
- [Federacja Kazachstanu: Astana 250 i wyjątek 500 w 2022](https://ktf.kz/en/tournaments/international/atp-250-astana-open/).

Wykluczamy NextGen (krótkie sety), Olympics i wydarzenia drużynowe (ATP Cup, Laver Cup, United Cup). ATP Finals zostaje oddzielną kategorią meczową; aplikacja nie symuluje jego fazy grupowej. Mapa jest wersjonowanym opracowaniem do przypiętego archiwum, nie automatycznym źródłem przyszłych kalendarzy. Przy nowych sezonach wymaga ponownego sprawdzenia.
