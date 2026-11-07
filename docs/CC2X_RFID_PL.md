# CC2X v1 — własna marka i nazwa filamentu na tagu NFC (eksperyment)

## Co zmienia ta gałąź

- Formularz nowej szpuli ma osobne pole **Brand / Manufacturer**.
- Wczytana z katalogu SpoolmanDB marka jest zachowywana oddzielnie od nazwy.
- Kodowanie standardowego bloku **ELEGOO pages 16..24** pozostaje identyczne z oryginałem.
- Dodatkowe dane **brand** i **name** są kodowane jako CC2X v1 w pamięci użytkownika NTAG213/215 **pages 25..39** (60 bajtów).
- Zmodyfikowana aplikacja po ponownym odczycie rozpoznaje nazwę i markę CC2X, zamiast rekonstruować je z kodów typu/podtypu.
- Katalog Room jest migrowany z v3 do v4 bez usuwania istniejących danych (dla standardowej migracji 3→4).

## Format rekordów CC2X v1

Bajty 0..3: ASCII CC2X. Bajt 4: wersja 1. Bajt 5: liczba bajtów UTF-8 marki. Bajt 6: liczba bajtów UTF-8 nazwy. Bajt 7: flags=0. Następnie marka i nazwa w UTF-8 oraz CRC16/CCITT-FALSE. Reszta bajtów jest zerowana. Łącznie dostępne **50 bajtów UTF-8** na oba pola.

Przy zapisie aplikacja najpierw sprawdza strony 25..39. Jeśli znajdują się tam niezerowe, nierozpoznane dane, zapis jest odrzucany przed modyfikacją tagu. Uszkodzone rekordy CC2X również nie są nadpisywane automatycznie. Po zapisie dodatkowe dane są weryfikowane przez odczyt zwrotny.

## Ograniczenia i bezpieczeństwo

**To funkcja eksperymentalna, niezweryfikowana na fizycznym CANVAS.** Nie ma jeszcze potwierdzenia, że stock ELEGOO akceptuje tagi z dodatkowym rekordem CC2X. Testować wyłącznie na zapasowym, pustym tagu NTAG213/215; NIE przepisywać od razu używanych szpul. Nie zmieniać stron systemowych/konfiguracyjnych ani haseł NFC.

**Stock CANVAS nie potrafi obecnie wyświetlić dodatkowej nazwy/marki CC2X.** Te dane odczytuje zmodyfikowana aplikacja na Androida. Wyświetlenie ich w CC2 Control wymaga osobnej implementacji odczytu rozszerzenia i mapowania do slotu/bazy. Nie wykonano żadnej modyfikacji firmware drukarki.

Zapis obecnej wersji nie jest transakcją atomową: przerwanie po częściowym zapisie może zostawić niekompletny rekord CC2X (CRC wykryje błąd). Aplikacja odmówi wtedy automatycznego nadpisania. Dlatego używać na testowym chipie.

## Walidacja

Kodek CC2X ma osobny test JVM w \`tools/CC2XCodecSmokeTest.kt\`. Dla JDK21/Kotlin:

\`\`\`sh
kotlinc app/src/main/java/com/blumlaut/filamenttagwriter/data/model/TagExtraMetadata.kt tools/CC2XCodecSmokeTest.kt -include-runtime -d /tmp/cc2x.jar
java -jar /tmp/cc2x.jar
\`\`\`

Pełna kompilacja Androida: \`./gradlew :app:assembleDebug\` (wymaga Android SDK 37, Gradle i pobrania zależności). **Pełny APK ani test fizycznego odczytu/zapisu NFC nie zostały jeszcze wykonane.**

## Następny krok

1. Kompilacja Androida i test odczytu/zapisu na zapasowym NTAG213.
2. Sprawdzenie, czy CANVAS nadal odczytuje standardowe pola po zapisaniu CC2X.
3. Dopiero potem przygotowanie rozszerzenia CC2 Control do odczytu prawdziwej nazwy i marki.
