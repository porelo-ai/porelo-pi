# PI74 — README und doc/index.md gegen Implementierung prüfen und korrigieren

```json
{
  "$schema": "issue-1.json",
  "id": "PI74",
  "title": "README und doc/index.md gegen Implementierung prüfen und korrigieren",
  "state": "In analysis",
  "size": "-",
  "priority": "Medium",
  "rank": 9,
  "blockedBy": [],
  "covers": "README und doc/index.md gegen Implementierung prüfen und korrigieren"
}
```

**What must be true.** Jede Aussage in README.md und doc/index.md ist gegen pi.yaml, den Java-Code, das npm-Lock und die Build-Skripte geprüft (u. a. Paketname, Größe ~79 MB, Pfade /opt und /usr/local/bin/pi, Provider openrouter und github-copilot, OAuth-App-Id, Beispielmodell, erreichbare und gesperrte Hosts, Lognamen und Meldungen der Fehlertabelle). Abweichungen sind in der Doku korrigiert, wo der Code richtig ist; wo der Code falsch scheint, ist ein eigenes Issue angelegt statt die Doku anzupassen. Links in beiden Dateien funktionieren. build.md und decisions.md bleiben außen vor.
