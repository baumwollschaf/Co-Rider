# Co-Rider – dauerhafte Projektregeln

Diese Regeln gelten für das gesamte Repository und für alle zukünftigen Arbeiten an Co-Rider.

## Produkt und Zweck

- Projektname: **Co-Rider**.
- Co-Rider ist eine native Android-App und ausdrücklich kein eigenes Navigationssystem.
- Co-Rider wird zunächst ausschließlich für Android entwickelt; eine iOS-Version ist aktuell nicht geplant.
- Der gewünschte Hauptworkflow ist: **GPX auswählen → lokal verarbeiten → geeignete Google-Maps-Route erzeugen → Google Maps öffnen → Navigation dort starten → Smartphone anschließend in der Tasche lassen → Ausgabe unter anderem über Honda RoadSync auf dem Motorrad-TFT.**
- Das Projekt ist Open Source, kostenlos und werbefrei. Es gibt keine Abonnements und keine In-App-Käufe.
- Ziel ist eine echte, veröffentlichungsfähige Google-Play-App und kein Demo- oder Beispielprojekt.

## Technische Basis

- Native Android-App mit Kotlin, Jetpack Compose, Material 3 und Gradle Kotlin DSL.
- Application ID und Namespace: `com.ralphdietrich.corider`.
- Minimum SDK: API 24.
- Keine XML-Views; die Oberfläche wird mit Jetpack Compose umgesetzt.
- Verwende moderne, idiomatische Android-Patterns und stabile AndroidX-/Kotlin-APIs.
- Verwende keine experimentellen Libraries, wenn eine stabile Standardlösung existiert.
- Halte Play-Store- und Android-Anforderungen wie `compileSdk`, `targetSdk`, Manifest und Build-Konfiguration aktuell und korrekt.
- Das Release-Ziel ist ein korrekt signiertes Android App Bundle (`.aab`).

## Architektur und Quellcode

- Halte die Architektur schlank und passend zur Projektgröße.
- Verwende keine unnötigen Frameworks, Abstraktionsschichten oder Architektur nur um der Architektur willen.
- Respektiere die bestehende Trennung zwischen Compose-UI, UI-State/ViewModel, Repository, Persistenz und fachlichen Modellen/Interfaces.
- Schreibe bestehenden Code oder die bestehende Architektur nicht ohne konkreten Nutzen großflächig um.
- Quellcode soll klar, wartbar und professionell sein.
- Wenn eine Anforderung technisch ungünstig formuliert ist, wähle die bessere native Android-Lösung und begründe die Abweichung knapp.

## Routenaufbereitung und Google Maps

- Bevorzuge für die Übergabe an Google Maps einfache native Android-Intents beziehungsweise Google-Maps-URLs.
- Vermeide Google-Maps-APIs, API-Keys, Cloud-Dienste und Backend-Infrastruktur, solange sie für die Kernfunktion nicht zwingend erforderlich sind.
- Die GPX-Aufbereitung darf nicht stumpf Punkte in festen Abständen übernehmen. Sie soll strategische Zwischenpunkte so auswählen, dass Google Maps den Charakter einer Motorradtour möglichst gut erhält.
- Die genaue Algorithmik ist noch nicht festgelegt. Implementiere sie sauber testbar, austauschbar und getrennt von Compose-UI sowie Android-spezifischem Code.

## Lokale Daten und Datenschutz

- Es gibt keine Cloud, kein Backend und keine Benutzerkonten.
- Alle GPX-Daten bleiben lokal auf dem Gerät.
- Importierte GPX-Dateien werden in den internen App-Speicher übernommen.
- Die Historie enthält maximal die 10 zuletzt verwendeten Routen.
- Alles jenseits der 10 neuesten Routen muss sowohl aus den Metadaten als auch aus dem internen Dateispeicher entfernt werden.
- Die zuletzt aktive Route wird persistent gespeichert und nach einem App-Neustart wiederhergestellt.
- Für kleine Einstellungen und Metadaten wird DataStore verwendet. Room oder SQLite nur bei einem später nachgewiesenen echten Bedarf einführen.
- Keine unnötigen Android-Permissions anfordern.
- Bevorzuge immer datenschutzfreundliche, lokal-first Lösungen.

## UI und Einstellungen

- Verwende eine moderne Material-3-Oberfläche.
- Die App soll modern, hochwertig, klar und bewusst reduziert wirken, nicht wie ein technisches Demo-Utility.
- Ein Navigation Drawer mit Hamburger-Menü dient für GPX-Import, Historie und sinnvolle Einstellungen.
- Das Theme unterstützt `System`, `Hell` und `Dunkel`.
- Theme-Default ist `System`; die Auswahl wird persistent gespeichert.
- Die vorgesehene Richtung für ein späteres App-Icon ist eine moderne Verbindung aus Motorrad beziehungsweise Helm und Navigation. Erzeuge kein finales Icon, solange es nicht ausdrücklich beauftragt wird.

## Qualität und Release

- Sichere Änderungen mit passenden Unit Tests ab, sofern dies sinnvoll ist.
- Führe nach substantiellen Änderungen Tests und einen vollständigen Gradle-Build aus und behebe auftretende Fehler selbst.
- Prüfe vor einem Release zusätzlich Manifest, Versionierung, Release-Optimierung, Signing, App Bundle und die zu diesem Zeitpunkt geltenden Play-Store-Vorgaben.
- Keystores, Zugangsdaten, Tokens, Secrets und lokale Signing-Konfigurationen dürfen niemals in das Repository gelangen.

## Arbeitsweise

- Lies vor Änderungen zunächst den aktuellen Repository-Stand, insbesondere diese Datei, `PROJECT_STATUS.md`, den Git-Status und die betroffenen Quell- und Testdateien.
- Respektiere bestehende Entscheidungen und bereits vorhandene Änderungen im Arbeitsbaum.
- Frage nicht bei jeder Kleinigkeit nach, wenn eine technisch eindeutige und risikoarme Entscheidung selbst getroffen werden kann.
- Frage bei Entscheidungen mit dauerhaften Folgen oder mehreren ernsthaft unterschiedlichen Produktoptionen nach.
- Normale Gespräche, Architekturfragen und Brainstorming finden ebenfalls direkt in dieser Arbeitsumgebung statt.
- Wenn der Nutzer nur fragt, diskutiert oder eine Idee äußert, werden weder Dateien geändert noch Befehle ausgeführt. Erst bei einem eindeutigen Umsetzungsauftrag darf der Projektzustand verändert werden.
- Arbeite bei normalen Entwicklungsaufträgen weitgehend selbstständig, führe notwendige Tests und Builds aus und behebe gefundene Fehler.
- Aktualisiere nach jeder größeren abgeschlossenen Aufgabe `PROJECT_STATUS.md`.
- Halte dort den aktuellen Funktionsstand, wichtige Architekturentscheidungen, bekannte Einschränkungen oder Fehler, offene Aufgaben, den sinnvollsten nächsten Entwicklungsschritt und notwendige manuelle Entwickleraktionen fest.

## Git-Regeln

- Füge neue oder geänderte projektrelevante Dateien nach erfolgreicher Prüfung mit `git add` zum Git-Index hinzu.
- Stage niemals generierte Dateien, Build-Artefakte, lokale Konfigurationen, Keystores, Zugangsdaten, Tokens, Secrets oder sonstige maschinen- bzw. benutzerspezifische Dateien.
- Achte bei einem bereits veränderten Arbeitsbaum darauf, keine fremden oder nicht zur Aufgabe gehörenden Änderungen zu überschreiben oder versehentlich zu stagen.
- Erstelle keine Commits und führe keine Pushes ohne ausdrückliche Anweisung aus.
