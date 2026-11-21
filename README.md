# Kopfschmerzprotokoll

Einfache Android-App zum Führen eines Kopfschmerztagebuchs.

- Einträge mit Datum, Uhrzeit, Dauer, Stärke (1–10), Ort, Art, Begleitsymptomen,
  Medikament + Wirkung, Auslösern und Notizen
- Übersicht: Kopfschmerztage und Medikamententage der letzten 30 Tage
- **Export als PDF** (z. B. für den Arzt) für 30 / 90 Tage oder alle Einträge
- **Export als CSV** (Excel) – gleichzeitig ein Backup
- Alle Daten bleiben lokal auf dem Handy

## APK bekommen (ohne Android Studio)

Bei jedem Push baut GitHub Actions die App automatisch:

1. Im Repo auf **Actions** → Workflow **„Android-App bauen“** → neuesten Lauf öffnen
2. Unten unter **Artifacts** `Kopfschmerzprotokoll-APK` herunterladen (ZIP)
3. ZIP entpacken, `app-debug.apk` aufs Handy kopieren und öffnen
   (einmalig „Installation aus unbekannten Quellen“ erlauben)

Manuell starten: Actions → „Android-App bauen“ → **Run workflow**.

## Lokal bauen

```
./gradlew assembleDebug
```
APK liegt dann unter `app/build/outputs/apk/debug/`.
