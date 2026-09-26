# Co-Rider – Projektstatus

Stand: 26. September 2026

## Kurzstatus

Der lokale GPX-Import, eine responsive Routendetailseite und die erste vollständige Übergabe an Google Maps sind implementiert. Die App speichert bis zu zehn importierte Dateien lokal, stellt Theme und aktive Route wieder her und berechnet aus der GPX-Datei Distanz, Trackpunktzahl, Start/Ziel sowie ein Höhenprofil. Für Google Maps wird die Trackspur auf Start, Ziel und bis zu neun formgebende Zwischenpunkte reduziert.

Der Produktscope ist derzeit ausschließlich Android; eine iOS-Version ist nicht geplant.

Der aktuelle Stand baut erfolgreich, ist aber noch nicht releasefähig: Release-Signing, ein signiertes App Bundle sowie weitere Robustheits- und Gerätetests fehlen.

## Aktueller Funktionsstand

- Single-Activity-App mit Jetpack Compose und Material 3.
- Edge-to-edge Hauptansicht mit App-Bar `Co-Rider`.
- Navigation Drawer mit:
  - `Open GPX file`
  - Historie `Recently used`
  - Theme-Auswahl `System`, `Light`, `Dark`
- Empty State mit prominentem GPX-Auswahlbutton.
- Import und Auswahl eines Historieneintrags führen auf dieselbe Routendetailseite.
- Die Detailseite zeigt einen lesbar aufbereiteten Tournamen, den unveränderten Dateinamen, Distanz, Trackpunktzahl, niedrigsten und höchsten Höhenpunkt, ein geglättetes Höhenprofil sowie die bestätigten Positionen Start und Ziel.
- Im Hochformat scrollen die Details ausschließlich vertikal. Ab 760 dp Breite werden Zusammenfassung/Höhenprofil und Routenverlauf in zwei Spalten angeordnet; horizontales Scrollen wird nicht verwendet.
- `Choose another GPX file` und `Open in Google Maps` liegen in einem festen unteren Aktionsbereich, bleiben beim vertikalen Scrollen sichtbar und wechseln ab 620 dp Breite von einer vertikalen in eine horizontale Anordnung.
- Bei aktiver Route erzeugt `Open in Google Maps` eine Google-Maps-Fahrtroute und öffnet bevorzugt die installierte Google-Maps-App; ohne App wird ein allgemeiner URL-Handler versucht.
- GPX-1.1-Tracks (`trk`/`trkseg`/`trkpt`) und GPX-Routen (`rte`/`rtept`) werden mit einem SAX-Parser verarbeitet. Höhe und Zeit werden übernommen, sofern vorhanden.
- Standalone-Wegpunkte (`wpt`) werden eingelesen, aber bewusst nicht als Google-Maps-Zwischenziele verwendet, weil Exporte dort auch allgemeine POIs wie Tankstellen und Hotels enthalten können.
- Für Routendetails gilt die fachliche Trennung `Maneuver`, `Stop` und technischer `Shaping point`: Nur verlässliche Positionen werden angezeigt, Shaping Points bleiben unsichtbar und nicht eindeutig ausgezeichnete GPX-POIs werden nicht als Stops ausgegeben.
- Die Routenreduktion wählt bis zu neun Punkte mit der größten geometrischen Abweichung von der vereinfachten Trackform; sie übernimmt Punkte nicht in festen Abständen.
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
- `MainViewModel` hält den UI-State, startet Importe und Routenaufbereitung außerhalb des Main Threads und liefert die erzeugte Maps-URL als einmaliges UI-Ereignis.
- `DefaultRouteRepository` koordiniert DataStore, internen Dateispeicher, Historienreihenfolge, aktiven Eintrag und Theme. Schreiboperationen sind durch einen `Mutex` serialisiert.
- `DataStoreAppPreferencesStore` speichert maximal zehn feste Metadatensätze sowie aktive Route und Theme. Für diese kleine Datenmenge wird bewusst keine Datenbank verwendet.
- `InternalGpxFileStore` kapselt das Schreiben, Lesen und Löschen unter `filesDir/gpx`.
- Fachliche Modelle für GPX-Dokumente, Tracks und Punkte sind vorhanden.
- `SaxGpxParser`, `ShapePreservingRoutePointSelector` und `DefaultMapsRouteUrlFactory` implementieren die GPX-Auswertung und Routenaufbereitung als testbare Fachlogik ohne Compose- oder Android-Abhängigkeit.
- `RouteDetailsAnalyzer` berechnet lokal und reproduzierbar Routenlänge, Höhenstatistik, ein geglättetes und auf höchstens 320 Zeichenpunkte reduziertes Profil sowie bestätigte Routenpositionen. Die Original-GPX-Datei wird nicht verändert.
- `RoutePointSelector` bleibt die austauschbare Schnittstelle für den Google-Maps-Algorithmus. Maneuvers, bewusst gesetzte Stops und unsichtbare Shaping Points sind voneinander getrennte Konzepte und dürfen nicht vermischt werden.
- Die Übergabe verwendet eine offizielle Maps-URL mit `api=1`, Fahrmodus und Koordinaten. Es gibt keinen API-Key, keine Maps-Cloud-API und kein Backend.
- `MainActivity` öffnet die URL per Android-Intent bevorzugt im Google-Maps-Paket und fällt bei fehlender App auf einen allgemeinen URL-Handler zurück.
- Englisch ist als alleinige Sprache für nutzersichtbare UI-Texte, Meldungen und Accessibility-Beschreibungen festgelegt. Texte bleiben in Android-String-Ressourcen, damit eine spätere Lokalisierung möglich bleibt.
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
- GPX-Namespace, Trackpunkte, Routenpunkte, Höhe, Zeit und ungültige Dokumentwurzel.
- Formbasierte Zwischenpunktauswahl, Auswahl des längsten kontinuierlichen Segments und Ignorieren eigenständiger POIs.
- Korrekte, URL-kodierte Google-Maps-Parameter und Reihenfolge der Zwischenpunkte.
- Distanz- und Höhenprofilauswertung, Reduktion der Diagrammpunkte, Start/Ziel sowie das Ignorieren nicht klassifizierter Standalone-POIs.

Letzte vollständige Prüfung am 26. September 2026:

- `gradlew build`: erfolgreich.
- Debug- und unsigniertes Release-APK: erfolgreich erzeugt.
- 13 Unit Tests: erfolgreich, keine Fehler oder übersprungenen Tests.
- Android Lint: keine Fehler; lediglich Hinweise auf inzwischen neuere Bibliotheksversionen.
- Die bereitgestellte Motorrad-Online-Datei wurde zusätzlich lokal geprüft: 4.612 Trackpunkte und 180 eigenständige POIs wurden korrekt gelesen, Distanz und Höhenprofil erzeugt, neun Track-Zwischenpunkte ausgewählt und eine Maps-URL unterhalb des 2.048-Zeichen-Limits erzeugt. Die Beispieldatei wurde nicht in das Projekt kopiert.

Der vorhandene Instrumentation Test ist weiterhin nur der Template-Test; spezifische Compose-UI- oder End-to-End-Tests existieren noch nicht.

## Bekannte Einschränkungen und Risiken

- Importierte Dateien werden nach dem Speichern für die Detailseite geparst, aber noch nicht vor der Aufnahme in die Historie validiert. Eine falsch benannte oder ungültige Datei kann daher zunächst intern gespeichert werden; die Detailseite zeigt dann eine allgemeine Fehlermeldung.
- Fehlerhafte oder inhaltlich unbrauchbare Dateien erhalten auf der Detailseite derzeit nur eine allgemeine Fehlermeldung; eine konkrete Diagnose fehlt noch.
- Bei mehreren Tracks oder Segmenten wird derzeit nur das längste kontinuierliche Segment verwendet. Unterbrechungen, bewusst getrennte Etappen und Rundtouren benötigen noch eine definierte Behandlung.
- Die geometrisch ausgewählten Zwischenpunkte erhöhen die Routentreue, können Google Maps aber nicht zwingen, exakt der ursprünglichen GPX-Spur zu folgen. Ein manueller Vergleich verschiedener Touren steht noch aus.
- Der Google-Maps-Modus `two-wheeler` ist nicht universell verfügbar; die aktuelle Übergabe verwendet deshalb den breit unterstützten Fahrmodus `driving`.
- Das Verhalten während einer Mehrziel-Navigation und insbesondere an Zwischenzielen muss noch auf einem realen Gerät zusammen mit Honda RoadSync geprüft werden.
- Die aktuelle Beispieldatei enthält keine verlässlichen Abbiegeanweisungen oder ausgezeichneten Treffpunkte. Deshalb werden noch keine Maneuvers oder zusätzlichen Stops angezeigt; Google Maps liefert die eigentlichen Fahranweisungen und Straßennamen.
- Nutzerdefinierte Stops besitzen noch keine Eingabe- oder Persistenzoberfläche. Eine spätere lokale Begleitdatei kann solche Co-Rider-Metadaten aufnehmen, ohne die importierte GPX-Datei zu verändern.
- Ein Fehler beim nachträglichen Löschen einer verdrängten Datei wird aktuell toleriert; dadurch könnte im Ausnahmefall eine nicht mehr referenzierte Datei im internen Speicher verbleiben.
- Es gibt noch keine Wartungsroutine, die fehlende oder verwaiste interne Dateien mit den DataStore-Metadaten abgleicht.
- Release-Signing, Upload-Key/Keystore, Play App Signing und ein signiertes `.aab` sind nicht konfiguriert.
- Release-Optimierung und konkrete R8-/Keep-Regeln sind noch nicht für einen Produktionsrelease geprüft.
- Es wurden noch keine manuellen Tests auf API 24 oder mit Honda RoadSync dokumentiert. Der aktuelle Emulator-Test verwendet API 37.2.

## Offene Aufgaben

1. Den neuen `Open in Google Maps`-Ablauf auf Emulator und realem Android-Gerät mit mehreren realen Touren manuell vergleichen.
2. GPX-Dateien bereits beim Import validieren und konkrete, nutzerfreundliche Fehlerzustände abbilden.
3. Rundtouren und mehrere Tracksegmente erkennen und verständlich anzeigen.
4. Die Zwischenpunktauswahl anhand unterschiedlicher Motorradtouren bewerten und die Strategie bei Bedarf um Kurven-, Abzweig- oder Straßennetzmerkmale erweitern.
5. Verhalten für sehr große Tracks, mehrere Segmente, unterbrochene GPX-Dateien und Rundtouren definieren.
6. Mehrziel-Navigation, automatische Fortsetzung und Honda-RoadSync-Ausgabe praktisch prüfen.
7. Ein einfaches Modell und eine UI zum bewussten Markieren persistenter Stops entwickeln; nicht klassifizierte GPX-POIs dürfen dabei nicht automatisch übernommen werden.
8. Verwaiste Dateien erkennen und bereinigen sowie Dateilöschfehler nachvollziehbar behandeln.
9. Aussagekräftige Compose-UI-/Instrumentierungstests und manuelle Gerätetests ergänzen.
10. App-Icon, Store-Texte, Datenschutzangaben und weitere Release-Metadaten finalisieren. Die vorgesehene Icon-Richtung ist eine reduzierte Motorrad-Front mit Vorderrad, Frontpartie/Gabel und angedeutetem Lenker, verbunden mit Navigation. Das textfreie Adaptive Icon muss auch monochrom sowie klein funktionieren; ein finales Icon ist noch nicht beauftragt.
11. Release-Build optimieren, Signing sicher konfigurieren und ein signiertes Android App Bundle erzeugen.

## Sinnvollster nächster Entwicklungsschritt

Als Nächstes sollte die neue Detailseite mit der bereitgestellten Berlin-Hamburg-Tour im Emulator sowohl hochkant als auch quer geprüft und anschließend `Open in Google Maps` ausgeführt werden. Entscheidend sind die Lesbarkeit des Höhenprofils, dauerhaft erreichbare Aktionen und der visuelle Vergleich der Google-Maps-Strecke mit der ursprünglichen Motorradtour. Danach sollte die GPX-Validierung in den Importzeitpunkt vorgezogen werden.

## Notwendige manuelle Entwickleraktionen

- Das Projekt in Android Studio synchronisieren und auf mindestens einem API-24-Gerät/Emulator sowie einem aktuellen Android-Gerät testen.
- Den Dokumentenpicker mit verschiedenen Providern und realen kleinen sowie großen GPX-Dateien testen.
- Die bereitgestellte Berlin-Hamburg-Datei öffnen, `Open in Google Maps` betätigen und prüfen, ob Start, Ziel und der Streckencharakter plausibel übernommen werden.
- Detailseite, vertikales Scrollen und festen Aktionsbereich im Hoch- und Querformat prüfen; es darf kein horizontales Scrollen auftreten und keine Button-Beschriftung abgeschnitten werden.
- Prüfen, wie Google Maps die Navigation an den erzeugten Zwischenzielen fortsetzt und ob die Ausgabe über Honda RoadSync ohne Bedienung des Smartphones funktioniert.
- Import, App-Neustart, Theme-Wiederherstellung, Auswahl historischer Routen und Verdrängung des elften Eintrags auf einem Gerät verifizieren.
- Vor einem Release einen Upload-Key/Keystore außerhalb des Repositories sicher erstellen und aufbewahren.
- Signing-Daten ausschließlich über lokale Gradle-Konfiguration oder sichere CI-Secrets bereitstellen.
- Google Play App Signing einrichten und das signierte Release-`.aab` in einem internen Test-Track prüfen.
- Vor Veröffentlichung die dann aktuellen Target-SDK-, Data-Safety-, Datenschutz-, Store-Listing- und Richtlinienanforderungen in der Play Console kontrollieren.

## Repository-Zustand bei dieser Aktualisierung

- Die oben beschriebene Routendetailseite und Google-Maps-Übergabe liegen aktuell als noch nicht committete Änderungen im Arbeitsbaum vor und werden nach der Abschlussprüfung zum Git-Index hinzugefügt.
- Die externe Motorrad-Online-Beispieldatei wurde ausschließlich gelesen und nicht in das Repository kopiert.
- Es wurde kein Commit und kein Push ausgeführt.
