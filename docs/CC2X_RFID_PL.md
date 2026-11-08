# CC2X RFID Writer — rozszerzenie NFC dla ELEGOO CC2

## Wprowadzone funkcje

1. **Marka + pełna nazwa filamentu** — pola `Brand / Manufacturer` i `Name` zapisane na tagu NFC, niezależnie od lokalnego katalogu.
2. **Temperatury stołu Min/Max** — własne wartości w °C (0–150), przechowywane w rekordzie CC2X, nie w standardowym polu ELEGOO. Katalog SpoolmanDB może podpowiedzieć zakres.
3. **Tag Tools → Read Tag Memory** — bez zapisu; podgląd stron NFC 0–39 w HEX. Zrzut zawiera UID taga, dlatego nie publikuj go bez anonimizacji.
4. **Tag Tools → Clear Tag Filament Profile** — po potwierdzeniu i osobnym przyłożeniu taga czyści WYŁĄCZNIE strony użytkownika 16–39. **To nie jest reset fabryczny ani formatowanie konfiguracji NFC.** Dane identyfikacji filamentu znikną i trzeba zaprogramować tag ponownie. Nie dotyka UID, NDEF (4–15), blokad, hasła ani stron konfiguracyjnych.
5. **Auto-read across tabs** — opcjonalny, zapamiętywany przełącznik, który umożliwia skanowanie także podczas przeglądania katalogu/drukarki/narzędzi. Na zakładce Read odczyt automatyczny był już dostępny.
6. **Export / Import App Database** — zapis katalogu do JSON i import wyłącznie poprawnego formatu CC2X. Import generuje nowe identyfikatory i dodaje wpisy do katalogu zamiast je kasować. Możliwe są duplikaty; plik JSON należy traktować jako kopię zapasową danych.

## Format NFC: CC2X v2

Oryginalny rekord **ELEGOO** na stronach 16–24 pozostaje zgodny z dotychczasową implementacją. Rozszerzenie wykorzystuje **NTAG213/215, strony 25–39** (60 bajtów).

- Bajty 0–3: znacznik ASCII `CC2X`.
- Bajt 4: wersja `2`.
- Bajty 5–6: liczba bajtów UTF-8 marki i nazwy.
- Bajt 7: flagi = 0.
- Dalej: marka i nazwa w UTF-8 (razem najwyżej **46 bajtów**).
- Dalej: minimalna i maksymalna temperatura stołu, uint16 big-endian (4 bajty).
- Dalej: CRC16/CCITT-FALSE i zerowe dopełnienie.

Czytnik rozpoznaje także starsze rekordy **CC2X v1** (bez temperatur stołu, do 50 bajtów tekstu). Zapis tworzy v2. Przed zapisem aplikacja sprawdza, czy strony 25–39 są puste lub zawierają poprawny rekord CC2X; nie nadpisuje obcych, nierozpoznanych danych. Odczyt kontrolny weryfikuje zapis.

## Katalog lokalny

Room DB używa schematu v5. Zawiera migracje 3→4 (marka) oraz 4→5 (temperatury stołu). Testowy APK ma odrębny identyfikator aplikacji i katalog od oryginalnego Filament RFID Writer.

## Ograniczenia

**Brak potwierdzonej fizycznie zgodności z CANVAS.** Drukarka może nadal wyświetlać `ELEGOO` i nie odczyta dodatkowej marki, nazwy ani temperatur stołu bez osobnej implementacji w CC2 Control. Nie wykonano zmian firmware drukarki.

Operacje NFC nie są atomowe. Jeśli zapis lub czyszczenie zostaną przerwane, tag może zawierać niepełne dane. Funkcję czyszczenia testuj WYŁĄCZNIE na zapasowym chipie, nie na używanych szpulach.

U1 RFID dla Snapmaker U1 korzysta z OpenSpool (NDEF JSON), a nie z protokołu ELEGOO. CC2X nie kopiuje zapisu OpenSpool na tag ELEGOO.

## Kompilacja i test

```sh
./gradlew :app:assembleDebug
```

GitHub Actions: `CC2X Debug APK` zapisuje testowy APK jako artefakt. Po pobraniu ZIP wyodrębnij APK. Testy kodu i kompilacja NIE zastępują testu fizycznego zapisu/odczytu NFC.

Zalecana kolejność prób: utwórz profil w CC2X → zapisz na **pustym zapasowym** NTAG213/215 → odczytaj na telefonie → sprawdź, czy CANVAS wczytuje standardowe pola → dopiero później testuj funkcję czyszczenia.
