# Co-Rider – Projektstatus

Stand: 26. September 2026

## Kurzstatus

Die native Android-Basis für den lokalen GPX-Import ist implementiert. Die App kann eine Datei über den Android-Dokumentenpicker auswählen, in den internen App-Speicher kopieren, als aktive Route speichern und in einer auf zehn Einträge begrenzten Historie anzeigen. Theme und aktive Route werden wiederhergestellt. Die eigentliche GPX-Auswertung, Routenreduktion und Übergabe an Google Maps sind noch nicht implementiert.

Der Produktscope ist derzeit ausschließlich Android; eine iOS-Version ist nicht geplant.

Der aktuelle Stand baut erfolgreich, ist aber noch nicht releasefähig: Release-Signing, ein signiertes App Bundle und die eigentliche Routing-Funktion fehlen.

## Aktueller Funktionsstand

- Single-Activity-App mit Jetpack Compose und Material 3.
- Edge-to-edge Hauptansicht mit App-Bar `Co-Rider`.
- Navigation Drawer mit:
  - `GPX-Datei öffnen`
  - Historie `Zuletzt verwendet`
  - Theme-Auswahl `System`, `Hell`, `Dunkel`
- Empty State mit prominentem GPX-Auswahlbutton.
- Bei aktiver Route wird der Dateiname als lokal gespeicherte Route angezeigt.
- Dateiauswahl über das Storage Access Framework mit `ActivityResultContracts.OpenDocument`.
- Ausgewählte Dateien werden gestreamt und als `<UUID>.gpx` unter `filesDir/gpx` gespeichert.
- Für den Import werden keine Speicherberechtigungen angefordert und keine dauerhaften externen URI-Rechte benötigt, weil sofort eine interne Kopie erstellt wird.
- Historie und Einstellungen werden in Preferences DataStore `co_rider_preferences` gespeichert.
- Neue oder erneut ausgewählte Routen stehen oben in der Historie.
- Die Historie ist auf zehn Einträge begrenzt. Beim elften Import werden die ältesten Metadaten entfernt und die zugehörigen internen Dateien gelöscht.
- Die aktive Route und der Theme-Modus werden persistent wiederhergestellt.
- Theme-Default ist `System`; Hell und Dunkel sind ebenfalls persistent auswählbar.
- Automatisches Cloud-Backup und Gerätetransfer sind im Manifest und in den Backup-Regeln für die App-Daten ausgeschlossen.
- Importfehler werden derzeit als allgemeine Snackbar gemeldet.

## Wichtige Architekturentscheidungen

- `CoRiderApplication` stellt das Repository über einfache manuelle Dependency-Bereitstellung bereit; es gibt kein DI-Framework.
- `MainActivity` verbindet den persistenten Theme-State mit dem Compose-Theme.
- `MainViewModel` hält den UI-State, startet Importe außerhalb des Main Threads und delegiert Persistenz an das Repository.
- `DefaultRouteRepository` koordiniert DataStore, internen Dateispeicher, Historienreihenfolge, aktiven Eintrag und Theme. Schreiboperationen sind durch einen `Mutex` serialisiert.
- `DataStoreAppPreferencesStore` speichert maximal zehn feste Metadatensätze sowie aktive Route und Theme. Für diese kleine Datenmenge wird bewusst keine Datenbank verwendet.
- `InternalGpxFileStore` kapselt das Schreiben und Löschen unter `filesDir/gpx`.
- Fachliche Modelle für GPX-Dokumente, Tracks und Punkte sind vorhanden.
- `GpxParser` und `MapsRouteUrlFactory` sind als Erweiterungspunkte vorhanden, besitzen aber noch keine Implementierungen.
- Die spätere Routenaufbereitung soll als testbare, austauschbare Fachlogik ohne Abhängigkeit von Compose oder Android implementiert werden. Für die Übergabe an Google Maps sind einfache URLs beziehungsweise native Android-Intents ohne API-Key oder Cloud-Infrastruktur vorgesehen.
- Backup ist vollständig deaktiviert beziehungsweise explizit ausgeschlossen, damit importierte GPX-Daten lokal bleiben.

## Build-Konfiguration

- Namespace/Application ID: `com.ralphdietrich.corider`
- Minimum SDK: 24
- Compile SDK: 37
- Target SDK: 37
- Version: `1.0` (`versionCode` 1)
- Kotlin: 2.2.10
- Android Gradle Plugin: 9.4.1
- Compose BOM: 2026.02.01
- DataStore: 1.2.1
- Java-Quell- und Zielkompatibilität: 11
- Der Release-Build ist derzeit nicht signiert; Release-Optimierung ist noch deaktiviert.

## Tests und letzter Prüfstand

Vorhandene lokale Unit Tests prüfen:

- Begrenzung der Historie auf zehn Einträge.
- Entfernen der ältesten Metadaten und internen Datei beim elften Import.
- Wiederherstellung der aktiven Route und des Themes nach Repository-Neuerstellung.
- Lesen und Schreiben des tatsächlichen Preferences DataStore.

Letzte vollständige Prüfung am 26. September 2026:

- `gradlew build`: erfolgreich.
- Debug- und unsigniertes Release-APK: erfolgreich erzeugt.
- 4 Unit Tests: erfolgreich, keine Fehler oder übersprungenen Tests.
- Android Lint: 0 Findings.

Der vorhandene Instrumentation Test ist weiterhin nur der Template-Test; spezifische Compose-UI- oder End-to-End-Tests existieren noch nicht.

## Bekannte Einschränkungen und Risiken

- Importierte Dateien werden noch nicht als GPX geparst oder fachlich validiert. Auch eine falsch benannte oder ungültige Datei kann derzeit intern gespeichert werden.
- Es gibt noch keine Routenstatistik, Vorschau oder verständliche Detaildiagnose bei fehlerhaften Dateien.
- GPX-Tracks werden noch nicht reduziert, segmentiert oder in eine Google-Maps-kompatible Route umgewandelt.
- Google Maps wird noch nicht per Intent beziehungsweise URL geöffnet.
- Die genaue Strategie zur Auswahl charaktererhaltender Zwischenpunkte sowie zur Behandlung von Tracksegmenten, Wegpunkten, Rundtouren und Google-Maps-Zwischenzielgrenzen ist noch offen. Eine starre Auswahl in festen Punktabständen ist ausdrücklich nicht das Ziel.
- Ein Fehler beim nachträglichen Löschen einer verdrängten Datei wird aktuell toleriert; dadurch könnte im Ausnahmefall eine nicht mehr referenzierte Datei im internen Speicher verbleiben.
- Es gibt noch keine Wartungsroutine, die fehlende oder verwaiste interne Dateien mit den DataStore-Metadaten abgleicht.
- Release-Signing, Upload-Key/Keystore, Play App Signing und ein signiertes `.aab` sind nicht konfiguriert.
- Release-Optimierung und konkrete R8-/Keep-Regeln sind noch nicht für einen Produktionsrelease geprüft.
- Es wurden noch keine manuellen Tests auf API 24, aktuellen Android-Versionen oder mit realen GPX-Dateien dokumentiert.
- Die UI-Texte liegen derzeit nur in der Standard-Resource-Datei auf Deutsch vor.

## Offene Aufgaben

1. GPX-Parser implementieren und XML sicher sowie speicherschonend verarbeiten.
2. GPX-Dateien beim Import validieren und konkrete, nutzerfreundliche Fehlerzustände abbilden.
3. Routeigenschaften wie Tracksegmente, Punktzahl, Start/Ziel und Rundtour erkennen und anzeigen.
4. Eine austauschbare und getestete Strategie zur Auswahl strategischer Google-Maps-Zwischenziele entwickeln, welche den Charakter der Motorradtour möglichst gut erhält.
5. Google-Maps-URL beziehungsweise nativen Android-Intent ohne API-Key erzeugen, Google Maps bevorzugt öffnen und einen sauberen Fallback anbieten.
6. Verhalten für fehlendes Google Maps, sehr große Tracks, mehrere Segmente und unterbrochene GPX-Dateien definieren.
7. Verwaiste Dateien erkennen und bereinigen sowie Dateilöschfehler nachvollziehbar behandeln.
8. Aussagekräftige Compose-UI-/Instrumentierungstests und manuelle Gerätetests ergänzen.
9. App-Icon, Store-Texte, Datenschutzangaben und weitere Release-Metadaten finalisieren. Die vorgesehene Icon-Richtung verbindet Motorrad beziehungsweise Helm mit Navigation; ein finales Icon ist noch nicht beauftragt.
10. Release-Build optimieren, Signing sicher konfigurieren und ein signiertes Android App Bundle erzeugen.

## Sinnvollster nächster Entwicklungsschritt

Als Nächstes sollte `GpxParser` implementiert und in den Importfluss eingebunden werden. Der Import sollte nur valide GPX-Dateien übernehmen und aus ihnen ein stabiles internes `GpxDocument` erzeugen. Dazu gehören Unit Tests mit kleinen, realistischen Fixtures für Tracks, mehrere Segmente, optionale Höhen-/Zeitdaten und fehlerhaftes XML. Erst auf dieser geprüften Datenbasis sollte die Routenreduktion für Google Maps entwickelt werden.

## Notwendige manuelle Entwickleraktionen

- Das Projekt in Android Studio synchronisieren und auf mindestens einem API-24-Gerät/Emulator sowie einem aktuellen Android-Gerät testen.
- Den Dokumentenpicker mit verschiedenen Providern und realen kleinen sowie großen GPX-Dateien testen.
- Import, App-Neustart, Theme-Wiederherstellung, Auswahl historischer Routen und Verdrängung des elften Eintrags auf einem Gerät verifizieren.
- Vor einem Release einen Upload-Key/Keystore außerhalb des Repositories sicher erstellen und aufbewahren.
- Signing-Daten ausschließlich über lokale Gradle-Konfiguration oder sichere CI-Secrets bereitstellen.
- Google Play App Signing einrichten und das signierte Release-`.aab` in einem internen Test-Track prüfen.
- Vor Veröffentlichung die dann aktuellen Target-SDK-, Data-Safety-, Datenschutz-, Store-Listing- und Richtlinienanforderungen in der Play Console kontrollieren.

## Repository-Zustand bei dieser Aktualisierung

- Die oben beschriebene Android-Basis liegt aktuell als noch nicht committete Änderungen im Arbeitsbaum vor.
- `AGENTS.md` und `PROJECT_STATUS.md` werden durch diese Aufgabe separat zum Git-Index hinzugefügt.
- Die bereits vorhandene unversionierte Datei `gradle/gradle-daemon-jvm.properties` wurde nicht verändert oder gestaged; vor einer späteren Aufnahme muss geklärt werden, ob sie bewusst projektweit versioniert werden soll oder lokale/generierte Konfiguration ist.
- Es wurde kein Commit und kein Push ausgeführt.
