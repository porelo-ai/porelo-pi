# PI74 — README und doc/index.md gegen Implementierung prüfen und korrigieren

```json
{
  "$schema": "issue-1.json",
  "id": "PI74",
  "title": "README und doc/index.md gegen Implementierung prüfen und korrigieren",
  "state": "Backlog",
  "size": "S",
  "priority": "Medium",
  "rank": 9,
  "blockedBy": [],
  "covers": "README und doc/index.md gegen Implementierung prüfen und korrigieren"
}
```

**What must be true.** Jede Aussage in README.md und doc/index.md ist gegen pi.yaml, den Java-Code, das npm-Lock und die Build-Skripte geprüft (u. a. Paketname, Größe ~79 MB, Pfade /opt und /usr/local/bin/pi, Provider openrouter und github-copilot, OAuth-App-Id, Beispielmodell, erreichbare und gesperrte Hosts, Lognamen und Meldungen der Fehlertabelle). Abweichungen sind in der Doku korrigiert, wo der Code richtig ist; wo der Code falsch scheint, ist ein eigenes Issue angelegt statt die Doku anzupassen. Links in beiden Dateien funktionieren. build.md und decisions.md bleiben außen vor.

**Size:** S - Two Markdown files with a handful of sentence fixes; the cost is verifying them: one -Pdist build, reading Porelo's broker and provider sources, and an online link check. No contract changes.

**Estimate:** 180 min

## Plan

### Estimate

S, guessed at 180 minutes. stats/ holds only state moves for this repository, with no wall or VM minutes, so there is no measured median for S. The figure is AGENTS.md's "a few hours" for S, labelled as a guess.

### Where the time goes

- Reading and checking the about 40 statements against pi.yaml, the control file, pom.xml, build-pi-tree.sh, the npm lock and the Java: about 30 minutes. The plan has already done most of this and lists the matches.
- One -Pdist build to get the tree, the .deb and .rpm sizes and the 1.5.0 Copilot catalogue and headers: about 30 to 45 minutes, including the wait for the build.
- Checking the broker messages, the vault.log and relay.log location, provider hosts and CLI spellings in Porelo at 0.5.1-SNAPSHOT: about 45 minutes, if its source or a release can be read.
- Checking the external links and the site address online, then editing the two files and running mkdocs build --strict and check-doc-site: about 45 minutes.

### What would push it up

If Porelo's broker shows log wordings that differ from the error table, each difference becomes a new issue rather than a doc edit. That adds filing time, and the work could reach M if the error table turns out to be mostly wrong.

## To be checked

- No measured velocity exists: stats/ holds only state moves, with no wall or VM minutes, so 180 minutes is a guess based on S meaning a few hours.
- Whether Porelo's source or a release at 0.5.1-SNAPSHOT can be read. Without it, the error table, the hosts and the vault.log and relay.log location cannot be checked, and the work blocks or shrinks.
- Whether network access exists for the link and site check. This task had none, so the site address (porelo-ai.github.io/pi/) could not be checked from here.
- Whether a -Pdist build runs in the implementing environment. It downloads Node, fd and rg and the npm tree, and its time is the largest single uncertainty.
- The operator's scope choices: the 79 MB package-or-image wording, gpt-5-mini or gpt-5.6-terra, adding porelo vault import pi, and fixing the package-count comment drift.
- [PI44](README.md#pi44) changes the same Copilot paragraph in doc/index.md. If [PI44](README.md#pi44) goes first, this paragraph is rechecked against its result.

## Findings

- Checked again: pi.yaml has default openrouter, the app id Iv1.b507a08c87ecfe98 and target /opt; pom.xml has artifactId porelo-agent-pi and the repository URL porelo-ai/porelo-pi. The plan's matches hold.
- The comment drift is confirmed: pi.yaml line 146 says 162 npm packages and build-pi-tree.sh line 151 says 167.
- build.md gives about 74 MB for pi-tree.tar.gz and about 79 MB for the package. Nothing in the code holds either number, and build-pi-tree.sh prints the real sizes at build time.
