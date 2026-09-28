# Co-Rider – Projektstatus

Stand: 28. September 2026

## Kurzstatus

Der lokale GPX-Import, eine responsive Routendetailseite und die vollständige Übergabe an Google Maps sind implementiert. Die App speichert bis zu zehn importierte Dateien lokal, stellt Theme und aktive Route wieder her und berechnet aus der GPX-Datei Distanz, Höhenprofil sowie eine geometrische Repräsentationsqualität. Für Google Maps wird die Trackspur auf Start, Ziel und bis zu neun formgebende Zwischenpunkte reduziert.

Der Produktscope ist derzeit ausschließlich Android; eine iOS-Version ist nicht geplant.

Der bewusst kleine Produktkern bleibt ein kostenloser, werbefreier und quelloffener GPX-Durchreicher. Eine Pro-Version, Abonnements oder In-App-Käufe sind nicht vorgesehen. Eine unbegrenzte Routenbibliothek und Kategorien wie Motorrad, Auto oder Caravan werden vorerst nicht entwickelt, weil sie Co-Rider von diesem Kern in Richtung einer umfangreichen Routenverwaltung verschieben würden. Einzelne Historieneinträge löschen zu können, bleibt dagegen eine sinnvolle spätere Ergänzung; bevorzugt mit einer kurzzeitig angebotenen `Undo`-Aktion statt eines bestätigenden Yes/No-Dialogs.

Der aktuelle Stand baut erfolgreich und die Release-Signierung ist eingerichtet. Ein privater Upload-Keystore wurde außerhalb des Repositories erzeugt und das erste signierte Android App Bundle wurde erfolgreich gebaut, geprüft und von der Play Console in einem Produktionsrelease-Entwurf angenommen. Die Richtlinienangaben werden dort aktuell vervollständigt. Vor der Veröffentlichung fehlen insbesondere die öffentliche Datenschutzseite, der aktualisierte Bundle-Upload, die Store-Darstellung sowie weitere Robustheits- und Gerätetests.

## Verbindlicher V1-Scope

Der V1-Workflow ist abschließend festgelegt: **GPX importieren → Track lokal analysieren → maximal neun strategische Zwischenpunkte auswählen → eine Google-Maps-Route erzeugen → Google Maps starten → Navigation über Honda RoadSync/TFT verwenden.** Co-Rider bleibt ein kleiner GPX-Durchreicher und wird für V1 weder Navigationssystem noch Routing-Engine.

- Keine Google Routes API, kein API-Key, kein Billing, kein Backend und keine Cloud-Abhängigkeit. Eine vorgelagerte API-Routenberechnung würde den Flaschenhals der anschließenden Übergabe an die normale Google-Maps-App nicht beseitigen.
- Keine eigene Turn-by-Turn-Auswertung: Co-Rider ermittelt weder Abbiegeanweisungen noch Straßennamen oder reale Straßenentscheidungen. Google Maps übernimmt das Straßenrouting zwischen den übergebenen Koordinaten.
- Keine automatische Etappenteilung in V1. Mehrere Etappen würden wegen des fehlenden zuverlässigen Abschluss-Callbacks von Google Maps den Kernworkflow mit dem Smartphone in der Tasche gefährden.
- Kein OSM-Routing, Map Matching, Navigation SDK, Offline-Kartenmaterial, RoadSync-Patching, Nachahmen von Google Maps oder eigene BLE-/TFT-Navigation in V1.
- Ein öffentliches RoadSync-BLE-Reverse-Engineering-PoC aus dem Jahr 2026 bleibt lediglich ein möglicher späterer Forschungsansatz. Eine direkte Kette `GPX → eigene Navigation → Honda BLE → TFT` wäre ein wesentlich größeres und anderes Produkt.

Der verbindliche Leitsatz für die V1-Routenlogik lautet: **Reduce the complete GPX track to at most nine ordered intermediate coordinates while preserving the large-scale geometric shape of the entire route as well as possible.** Fachlich ist dies eine Fixed-Budget-Polyline-Vereinfachung mit höchstens neun Zwischenpunkten.

## Aktueller Funktionsstand

- Single-Activity-App mit Jetpack Compose und Material 3.
- Edge-to-edge Hauptansicht mit App-Bar `Co-Rider`.
- Navigation Drawer mit:
  - `Open GPX file`
  - Historie `Recently used`
  - Theme-Auswahl `System`, `Light`, `Dark`
  - `Privacy policy`, das die öffentliche Datenschutzseite im Browser öffnet
- Empty State mit prominentem GPX-Auswahlbutton.
- Import und Auswahl eines Historieneintrags führen auf dieselbe Routendetailseite.
- Die Detailseite zeigt einen lesbar aufbereiteten Tournamen, den unveränderten Dateinamen, Original-GPX-Distanz, niedrigsten und höchsten Höhenpunkt, ein geglättetes Original-GPX-Höhenprofil sowie eine verständliche Qualitätsaussage zur geometrischen Repräsentation.
- Technische Shaping Points, Trackpunktzahlen und vermeintliche Stationen oder Manöver werden bewusst nicht angezeigt, weil sie für den Nutzer keine verlässliche Aussage über Googles spätere Straßenauswahl liefern.
- Im Hochformat scrollen die Details ausschließlich vertikal. Ab 760 dp Breite werden Zusammenfassung und Höhenprofil nebeneinander angeordnet; horizontales Scrollen wird nicht verwendet.
- `Choose another GPX file` und `Open in Google Maps` liegen in einem festen unteren Aktionsbereich, bleiben beim vertikalen Scrollen sichtbar und wechseln ab 620 dp Breite von einer vertikalen in eine horizontale Anordnung.
- Bei aktiver Route erzeugt `Open in Google Maps` eine Google-Maps-Fahrtroute und öffnet bevorzugt die installierte Google-Maps-App; ohne App wird ein allgemeiner URL-Handler versucht.
- GPX-1.1-Tracks (`trk`/`trkseg`/`trkpt`) und GPX-Routen (`rte`/`rtept`) werden mit einem SAX-Parser verarbeitet. Höhe und Zeit werden übernommen, sofern vorhanden.
- Standalone-Wegpunkte (`wpt`) werden eingelesen, aber bewusst nicht als Google-Maps-Zwischenziele verwendet, weil Exporte dort auch allgemeine POIs wie Tankstellen und Hotels enthalten können.
- Für Routendetails gilt die fachliche Trennung `Maneuver`, `Stop` und technischer `Shaping point`: Shaping Points bleiben unsichtbar, nicht eindeutig ausgezeichnete GPX-POIs werden nicht als Stops ausgegeben und die eigentlichen Manöver kommen ausschließlich von Google Maps.
- Die Routenreduktion arbeitet als Fixed-Budget-RDP-ähnliche Vereinfachung: Start und Ziel bleiben fest, anschließend wird wiederholt der Trackpunkt mit der größten verbleibenden geometrischen Abweichung eingefügt. Dadurch wird die gesamte Trackform mit bis zu neun geordneten Shaping Points approximiert; es werden weder die ersten neun Punkte noch feste Entfernungsintervalle verwendet. Bei geometrisch gleichwertigen Kandidaten bevorzugt der Selektor die Mitte des noch ungestützten Abschnitts und verhindert so eine Punktanhäufung am Routenanfang.
- Nach der Reduktion werden maximale geometrische Abweichung, maximale ungestützte Tracklänge und ignorierte zusätzliche Segmente ermittelt. Daraus entsteht eine ehrliche lokale Einstufung `good`, `limited` oder `complex`; sie bewertet ausdrücklich nicht die von Google Maps später berechnete Straßenroute.
- Importierte Dateien werden vor der Aufnahme in die Historie erneut aus dem internen Speicher geparst und auf ein nutzbares Tracksegment geprüft. Ungültige oder routenlose Dateien werden wieder gelöscht.
- Dateiauswahl über das Storage Access Framework mit `ActivityResultContracts.OpenDocument`.
- Ausgewählte Dateien werden gestreamt und als `<UUID>.gpx` unter `filesDir/gpx` gespeichert.
- Für den Import werden keine Speicherberechtigungen angefordert und keine dauerhaften externen URI-Rechte benötigt, weil sofort eine interne Kopie erstellt wird.
- Historie und Einstellungen werden in Preferences DataStore `co_rider_preferences` gespeichert.
- Neue oder erneut ausgewählte Routen stehen oben in der Historie.
- Die Historie ist auf zehn Einträge begrenzt. Beim elften Import werden die ältesten Metadaten entfernt und die zugehörigen internen Dateien gelöscht.
- Die aktive Route und der Theme-Modus werden persistent wiederhergestellt.
- Theme-Default ist `System`; Hell und Dunkel sind ebenfalls persistent auswählbar.
- Das finale Helm-/Navigationsmotiv ist als klassisches Launcher-Icon für alle Android-Dichten, rundes Icon, Adaptive Icon und vereinfachtes monochromes Themed Icon eingebunden. Das Adaptive Icon verwendet eine eigene, nach innen komponierte Vollbildfassung als Hintergrundebene, damit Android das Motiv unter Kreis- und Squircle-Masken nicht vergrößert anschneidet und kein quadratischer Innenrahmen entsteht. Der Empty State verwendet dasselbe Grundmotiv; die Designquelle der Adaptive-Variante und ein separates 512×512-Store-Asset liegen unter `store-assets`.
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
- `RouteDetailsAnalyzer` berechnet lokal und reproduzierbar Routenlänge, Höhenstatistik, ein geglättetes und auf höchstens 320 Zeichenpunkte reduziertes Profil sowie die geometrische Repräsentationsqualität. Die Original-GPX-Datei wird nicht verändert.
- `RoutePointSelector` bleibt die austauschbare Schnittstelle für den Google-Maps-Algorithmus. Maneuvers, bewusst gesetzte Stops und unsichtbare Shaping Points sind voneinander getrennte Konzepte und dürfen nicht vermischt werden.
- Fixed-Budget RDP ist der für V1 verbindlich gewählte Grundalgorithmus. Es wird keine feste Fehlertoleranz vorgegeben; stattdessen ist das gemeinsame Budget aus höchstens neun Google-Maps-Zwischenpunkten fest. Der Algorithmus soll die großräumige Form der gesamten Route optimieren und insbesondere große lokale Repräsentationsfehler vermeiden.
- GPX 1.1 enthält keine standardisierte, herstellerübergreifend zuverlässige Kennzeichnung für einen bewusst gesetzten Pflichtstopp. Die untersuchten Motorrad-Online-Dateien speichern ihre Editor-Anker nicht als unterscheidbare Route Points oder Extensions. V1 arbeitet daher ausschließlich mit der Trackgeometrie; Standalone-Wegpunkte werden nicht zu Stops umgedeutet.
- Stops und technische Shaping Points bleiben fachlich getrennte Konzepte. Eine spätere Stop-Unterstützung kommt nur infrage, wenn eine Quelle ihre Semantik verlässlich kennzeichnet; sie ist kein Bestandteil von V1.
- Die geometrische Repräsentationsqualität wird über die maximale Abweichung des Originaltracks von der vereinfachten Linie bewertet. Maximale ungestützte Tracklänge und zusätzliche Segmente werden ebenfalls erfasst. Die UI-Zustände `good`, `limited` und `complex` versprechen ausdrücklich keine exakte Übereinstimmung mit der von Google berechneten Straßenroute.
- Die Übergabe verwendet eine offizielle Maps-URL mit `api=1`, Fahrmodus und Koordinaten. Es gibt keinen API-Key, keine Maps-Cloud-API und kein Backend.
- Ein praktischer Test mit Start, Ziel und zwölf Zwischenkoordinaten ergab, dass Google Maps genau die ersten neun Zwischenpunkte in der ursprünglichen Reihenfolge übernahm und die letzten drei verwarf. Google Maps optimiert ein zu großes Eingabebudget daher nicht für Co-Rider; die App muss selbst höchstens neun geeignete Punkte übergeben.
- `MainActivity` öffnet die URL per Android-Intent bevorzugt im Google-Maps-Paket und fällt bei fehlender App auf einen allgemeinen URL-Handler zurück.
- Englisch ist als alleinige Sprache für nutzersichtbare UI-Texte, Meldungen und Accessibility-Beschreibungen festgelegt. Texte bleiben in Android-String-Ressourcen, damit eine spätere Lokalisierung möglich bleibt.
- Backup ist vollständig deaktiviert beziehungsweise explizit ausgeschlossen, damit importierte GPX-Daten lokal bleiben.
- Die vorbereitete öffentliche Datenschutzseite beschreibt lokale GPX-Verarbeitung, Aufbewahrung und Löschung sowie die ausschließlich nutzerinitiierte Übergabe ausgewählter Routenkoordinaten an Google Maps. Sie kommt ohne Cookies, Analytics, externe Schriften oder Skripte aus und wird separat unter `https://baumwollschaf.github.io/co-rider-legal/` veröffentlicht.
- Der Ordner `Pics` enthält fertig benannte Play-Store-Assets: App-Symbol in 512×512 Pixeln, eine kinoreife Vorstellungsgrafik in 1024×500 Pixeln mit dem Claim „The missing link between GPX and your ride.“ und vier echte Smartphone-Screenshots im 9:16-Format. Tablet-, Desktop- und XR-Assets werden für die telefonzentrierte V1 bewusst nicht bereitgestellt, da sie in der Play Console optional sind.
- Der Produktscope bleibt bewusst klein: keine Pro-Ausgabe, keine Bezahlschranken und vorerst weder unbegrenzte Bibliothek noch Routenkategorien. Solche Verwaltungsfunktionen werden nur bei einem später nachgewiesenen praktischen Bedarf neu bewertet.

## Build-Konfiguration

- Namespace/Application ID: `com.ralphdietrich.corider`
- Minimum SDK: 24
- Compile SDK: 37
- Target SDK: 37
- Version: `1.0` (`versionCode` 2; der bereits hochgeladene erste Entwurf verwendete Code 1)
- Kotlin: 2.2.10
- Android Gradle Plugin: 9.4.1
- Compose BOM: 2026.02.01
- DataStore: 1.2.1
- Java-Quell- und Zielkompatibilität: 11
- Der Release-Build liest eine optionale, von Git ausgeschlossene `keystore.properties` und wird automatisch signiert, sobald der lokale Upload-Keystore konfiguriert ist. Ohne diese lokale Datei bleibt der Release-Build unsigniert. Release-Optimierung ist für den ersten Test-Build bewusst noch deaktiviert.

## Tests und letzter Prüfstand

Vorhandene lokale Unit Tests prüfen:

- Begrenzung der Historie auf zehn Einträge.
- Entfernen der ältesten Metadaten und internen Datei beim elften Import.
- Wiederherstellung der aktiven Route und des Themes nach Repository-Neuerstellung.
- Lesen und Schreiben des tatsächlichen Preferences DataStore.
- GPX-Namespace, Trackpunkte, Routenpunkte, Höhe, Zeit und ungültige Dokumentwurzel.
- Formbasierte Zwischenpunktauswahl, gleichmäßige Verteilung bei geometrisch gleichwertigen Kandidaten, Qualitätsklassifizierung, Auswahl des längsten kontinuierlichen Segments und Ignorieren eigenständiger POIs.
- Korrekte, URL-kodierte Google-Maps-Parameter und Reihenfolge der Zwischenpunkte.
- Distanz- und Höhenprofilauswertung, Reduktion der Diagrammpunkte sowie das Ignorieren nicht klassifizierter Standalone-POIs.
- Importvalidierung mit Entfernung einer ungültigen internen Datei, ohne Historieneintrag zu erzeugen.

Letzte vollständige Prüfung am 27. September 2026:

- `gradlew build`: erfolgreich.
- Debug- und unsigniertes Release-APK: erfolgreich erzeugt.
- Erstes mit dem privaten Upload-Key signiertes Release-Bundle: erfolgreich erzeugt; Signaturintegrität und wesentliche Bundle-Einträge wurden lokal geprüft. Das generierte `.aab` bleibt als Build-Artefakt von Git ausgeschlossen.
- 16 Unit Tests: erfolgreich, keine Fehler oder übersprungenen Tests.
- Android Lint: 0 Fehler; 8 reine Hinweise auf neuere stabile Bibliotheksversionen.
- Der ergänzte Datenschutz-Menüpunkt wurde für Debug und Release erfolgreich kompiliert; 16 Unit Tests, Debug-Lint und ein Release-Bundle-Lauf waren danach erfolgreich.
- Das aktuelle Debug-APK wurde zusätzlich erfolgreich gebaut, im Pixel-10a-Emulator installiert und für die finalen Store-Screenshots mit der Berlin-Hamburg-Testdatei geprüft.
- Drei bereitgestellte Motorrad-Online-Varianten wurden zusätzlich lokal geprüft: 4.612, 4.996 und 5.149 Trackpunkte wurden korrekt gelesen und jeweils auf neun strategische Zwischenpunkte reduziert. Die unterschiedlichen Streckenverläufe erzeugen unterschiedliche Stützpunkte; alle drei langen Touren werden wegen der verbleibenden maximalen Abweichung ehrlich als `limited` eingestuft. Die Beispieldateien wurden nicht in das Projekt kopiert.

Der vorhandene Instrumentation Test ist weiterhin nur der Template-Test; spezifische Compose-UI- oder End-to-End-Tests existieren noch nicht.

## Bekannte Einschränkungen und Risiken

- Fehlerhafte oder inhaltlich unbrauchbare Dateien werden beim Import abgewiesen, erhalten in der UI aber derzeit nur eine allgemeine Fehlermeldung; eine konkrete Diagnose fehlt noch.
- Bei mehreren Tracks oder Segmenten wird derzeit nur das längste kontinuierliche Segment verwendet. Zusätzliche Segmente führen zur Qualitätsstufe `complex`, werden aber noch nicht einzeln erklärt.
- Die geometrisch ausgewählten Zwischenpunkte erhöhen die Routentreue, können Google Maps aber nicht zwingen, exakt der ursprünglichen GPX-Spur zu folgen. Ein manueller Vergleich verschiedener Touren steht noch aus.
- Die Qualitätsstufe ist eine geometrische Heuristik und kein Beweis dafür, dass Google Maps dieselben Straßen wählt. Schwellenwerte müssen mit weiteren realen Motorradtouren praktisch validiert werden.
- Echte Editor-Zwischenstopps sind in gewöhnlichen GPX-Tracks nicht zuverlässig rekonstruierbar. V1 versucht dies bewusst nicht; eine zukünftige Unterstützung benötigt eindeutig ausgezeichnete Quelldaten oder lokale Zusatzmetadaten.
- Neun Zwischenpunkte können eine beliebig komplexe Tour mathematisch nicht verlustfrei darstellen. V1 soll innerhalb dieses Budgets die bestmögliche geometrische Repräsentation liefern und bei unzureichender Qualität ehrlich warnen, nicht automatisch Etappen erzeugen oder Genauigkeit versprechen.
- Der Google-Maps-Modus `two-wheeler` ist nicht universell verfügbar; die aktuelle Übergabe verwendet deshalb den breit unterstützten Fahrmodus `driving`.
- Das Verhalten während einer Mehrziel-Navigation und insbesondere an Zwischenzielen muss noch auf einem realen Gerät zusammen mit Honda RoadSync geprüft werden.
- Die aktuelle Beispieldatei enthält keine verlässlichen Abbiegeanweisungen oder ausgezeichneten Treffpunkte. Deshalb werden noch keine Maneuvers oder zusätzlichen Stops angezeigt; Google Maps liefert die eigentlichen Fahranweisungen und Straßennamen.
- Nutzerdefinierte Stops besitzen noch keine Eingabe- oder Persistenzoberfläche. Eine spätere lokale Begleitdatei kann solche Co-Rider-Metadaten aufnehmen, ohne die importierte GPX-Datei zu verändern.
- Ein Fehler beim nachträglichen Löschen einer verdrängten Datei wird aktuell toleriert; dadurch könnte im Ausnahmefall eine nicht mehr referenzierte Datei im internen Speicher verbleiben.
- Es gibt noch keine Wartungsroutine, die fehlende oder verwaiste interne Dateien mit den DataStore-Metadaten abgleicht.
- Das erste signierte `.aab` mit `versionCode` 1 wurde von der Play Console im Produktionsrelease-Entwurf angenommen. Wegen des anschließend ergänzten verpflichtenden In-App-Datenschutzlinks muss es vor der Freigabe durch ein neu signiertes Bundle mit `versionCode` 2 ersetzt werden.
- Release-Optimierung und konkrete R8-/Keep-Regeln sind noch nicht für einen Produktionsrelease geprüft.
- Es wurden noch keine manuellen Tests auf API 24 oder mit Honda RoadSync dokumentiert. Der aktuelle Emulator-Test verwendet API 37.2.

## Offene Aufgaben

1. Den neuen `Open in Google Maps`-Ablauf auf Emulator und realem Android-Gerät mit mehreren realen Touren manuell vergleichen.
2. Doppelte GPX-Importe und Namenskollisionen sauber behandeln: Maßgeblich für ein Duplikat soll ein stabiler Fingerabdruck der geparsten, routenrelevanten GPX-Geometrie sein, nicht Dateiname, URI, interne UUID oder unwesentliche XML-Formatierung beziehungsweise Metadaten. Bei gleichem Routeninhalt darf keine zweite interne Kopie und kein zweiter Historieneintrag entstehen; stattdessen wird der vorhandene Eintrag aktiviert und eine kurze englische Meldung mit dessen Anzeigenamen ausgegeben, beispielsweise `This route is already in your history as “%1$s”.` Unterschiedliche Routeninhalte mit demselben Quelldateinamen müssen dagegen beide erhalten bleiben und in Co-Rider eindeutige Anzeigenamen bekommen, beispielsweise `Tour.gpx` und `Tour (2).gpx`; die Quelldateien selbst werden nicht umbenannt. Die Umsetzung benötigt passende Repository- und ViewModel-Tests für identische Inhalte unter gleichen und unterschiedlichen Dateinamen sowie für unterschiedliche Inhalte mit gleichem Dateinamen.
3. Konkretere, nutzerfreundliche Importfehler für ungültige oder routenlose GPX-Dateien abbilden.
4. Rundtouren und mehrere Tracksegmente praktisch prüfen und zusätzliche Segmente verständlicher erklären.
5. Qualitätsgrenzen der Fixed-Budget-RDP-Auswahl anhand weiterer kurzer, langer, kurviger und sich kreuzender Motorradtouren kalibrieren.
6. Mehrziel-Navigation, automatische Fortsetzung und Honda-RoadSync-Ausgabe praktisch prüfen.
7. Verwaiste Dateien erkennen und bereinigen sowie Dateilöschfehler nachvollziehbar behandeln.
8. Das gezielte Löschen eines Historieneintrags vorsehen; bevorzugt direktes Löschen mit Snackbar-Aktion `Undo` statt eines zusätzlichen Bestätigungsdialogs.
9. Aussagekräftige Compose-UI-/Instrumentierungstests und manuelle Gerätetests ergänzen.
10. Die fertigen Dateien aus `Pics` in den Store-Eintrag hochladen und die verbleibenden Release-Metadaten finalisieren. Datenschutzseite, GitHub Pages, In-App-Link, Store-Texte und die erforderlichen Store-Grafiken sind vorbereitet. Das App-Icon einschließlich Adaptive-, Round-, Legacy- und monochromer Variante ist fertig eingebunden; die tatsächliche Darstellung unter verschiedenen Launcher-Masken wird noch auf Geräten kontrolliert.
11. Ein neues signiertes Android App Bundle mit `versionCode` 2 erzeugen, den Bundle-Entwurf mit Code 1 ersetzen und die Release-Optimierung vor der Produktionsfreigabe erneut bewerten.

## Sinnvollster nächster Entwicklungsschritt

Der V1-Funktionskern ist weit genug für den entscheidenden Praxistest. Als Nächstes sollte die neue Detailseite mit den bereitgestellten Berlin-Hamburg-Varianten im Emulator hochkant und quer geprüft und anschließend `Open in Google Maps` ausgeführt werden. Entscheidend sind der visuelle Vergleich der Google-Maps-Strecke mit dem jeweiligen Originaltrack sowie das Verhalten an den erzeugten Zwischenzielen auf einem realen Gerät mit Honda RoadSync. Vor diesem Test wird keine Etappenteilung, Stop-Verwaltung oder neue Großarchitektur begonnen.

## Notwendige manuelle Entwickleraktionen

- Das Projekt in Android Studio synchronisieren und auf mindestens einem API-24-Gerät/Emulator sowie einem aktuellen Android-Gerät testen.
- Den Dokumentenpicker mit verschiedenen Providern und realen kleinen sowie großen GPX-Dateien testen.
- Die bereitgestellte Berlin-Hamburg-Datei öffnen, `Open in Google Maps` betätigen und prüfen, ob Start, Ziel und der Streckencharakter plausibel übernommen werden.
- Detailseite, vertikales Scrollen und festen Aktionsbereich im Hoch- und Querformat prüfen; es darf kein horizontales Scrollen auftreten und keine Button-Beschriftung abgeschnitten werden.
- Launcher-Icon auf einem aktuellen Gerät beziehungsweise Emulator mit Kreis-, Squircle- und Themed-Icon-Darstellung kontrollieren.
- Prüfen, wie Google Maps die Navigation an den erzeugten Zwischenzielen fortsetzt und ob die Ausgabe über Honda RoadSync ohne Bedienung des Smartphones funktioniert.
- Mit weiteren Testdateien bestätigen, dass frühe und späte große Schlenker beziehungsweise Schleifen durch die budgetgesteuerte Auswahl berücksichtigt werden und die Qualitätswarnung bei unvertretbar großen lokalen Fehlern greift.
- Import, App-Neustart, Theme-Wiederherstellung, Auswahl historischer Routen und Verdrängung des elften Eintrags auf einem Gerät verifizieren.
- Den außerhalb des Repositories erstellten Upload-Key/Keystore zusammen mit dem Passwort verschlüsselt und redundant sichern.
- Signing-Daten ausschließlich über lokale Gradle-Konfiguration oder sichere CI-Secrets bereitstellen.
- Das separate Repository `co-rider-legal` einchecken, GitHub Pages für `main` und den Root-Ordner aktivieren und anschließend `https://baumwollschaf.github.io/co-rider-legal/` kontrollieren.
- Den In-App-Link `Privacy policy` im neu gebauten Release testen und diese öffentliche URL in der Play Console eintragen.
- Ein signiertes Release-`.aab` mit `versionCode` 2 erzeugen, hochladen und den Play-ausgelieferten Build möglichst vor der Produktionsfreigabe auf einem realen Gerät prüfen.
- Vor Veröffentlichung die dann aktuellen Target-SDK-, Data-Safety-, Datenschutz-, Store-Listing- und Richtlinienanforderungen in der Play Console kontrollieren.

## Repository-Zustand bei dieser Aktualisierung

- Ausgangspunkt der Implementierung war Commit `0c98780` auf `main` und `origin/main` sowie die bereits vorbereitete V1-Entscheidungsdokumentation.
- Die V1-Qualitätsmessung, Importvalidierung und reduzierte Detailseite sind implementiert und geprüft.
- Das finale App-Icon wurde aus der bereitgestellten Vorlage ohne den äußeren hellen Rand abgeleitet und in alle Android-Iconvarianten sowie den Empty State übernommen.
- Die externen Motorrad-Online-Beispieldateien wurden ausschließlich gelesen und nicht in das Repository kopiert.
- Es wurde kein Commit und kein Push ausgeführt.
