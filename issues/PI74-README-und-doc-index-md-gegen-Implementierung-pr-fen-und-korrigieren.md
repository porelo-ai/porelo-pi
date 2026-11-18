# PI74 — README und doc/index.md gegen Implementierung prüfen und korrigieren

```json
{
  "$schema": "issue-1.json",
  "id": "PI74",
  "title": "README und doc/index.md gegen Implementierung prüfen und korrigieren",
  "state": "Under estimation",
  "size": "S",
  "priority": "Medium",
  "rank": 9,
  "blockedBy": [],
  "covers": "README und doc/index.md gegen Implementierung prüfen und korrigieren"
}
```

**What must be true.** Jede Aussage in README.md und doc/index.md ist gegen pi.yaml, den Java-Code, das npm-Lock und die Build-Skripte geprüft (u. a. Paketname, Größe ~79 MB, Pfade /opt und /usr/local/bin/pi, Provider openrouter und github-copilot, OAuth-App-Id, Beispielmodell, erreichbare und gesperrte Hosts, Lognamen und Meldungen der Fehlertabelle). Abweichungen sind in der Doku korrigiert, wo der Code richtig ist; wo der Code falsch scheint, ist ein eigenes Issue angelegt statt die Doku anzupassen. Links in beiden Dateien funktionieren. build.md und decisions.md bleiben außen vor.

**Size:** S - Two Markdown files, a handful of sentence fixes; the cost is verification: one -Pdist build to read the tree and sizes, reading Porelo's broker messages and provider hosts, and an online link check.

## Plan

### Starting point, read in today's code

The two documents make about 40 checkable statements. Each one is backed by one of four sources, and the plan works through them source by source:

- **This repository, readable now:** `src/main/resources/agent/pi.yaml`, `src/deb/control/control`, `pom.xml`, `buildtools/build-pi-tree.sh`, `src/main/npm/package.json` and its lock, `mkdocs.yml`, the Java under `src/main/java/org/fuin/porelo/agent/impl/pi/`.
- **The built tree** (`target/pi-tree.tar.gz` from `-Pdist`): the 1.5.0 `pi-ai` and `pi-coding-agent` sources, the Copilot catalogue, the package size.
- **Porelo itself** at the pinned `porelo.version` (0.5.1-SNAPSHOT): `providers/openrouter.yaml` and the Copilot provider (hosts), the broker's `vault.log` lines and messages, `relay.log`, `--token-hours`, `porelo agents --verbose`, `porelo project default add/list`, and `task start` adding the checkout by itself. None of these is in this repository.
- **The network:** the external links and the documentation site address.

### Already matched against the code (no change)

- Package name `porelo-agent-pi`: `pom.xml` artifactId and the control file's `Package: [[name]]`. `Provides: porelo-agent` and the install into `/usr/libexec/porelo/agents` support "`porelo agents` lists it at once; nothing has to be registered".
- `@earendil-works/pi-coding-agent`: `package.json` pins 1.5.0, as does `agent.cli.version`.
- Tree contents `pi`, `node`, `tools` (fd, rg): `build-pi-tree.sh` tars these three, and pi.yaml unpacks them with `target: /opt`. The wrapper is written to `/usr/local/bin/pi`, which fits "under `/opt`, run as `/usr/local/bin/pi`, the image build downloads nothing".
- Telemetry and version check off (`PI_SKIP_VERSION_CHECK=1`, `PI_TELEMETRY=0` in the wrapper), `refused_domains: [pi.dev]`, and fd and rg on PATH: all match.
- OAuth app id `Iv1.b507a08c87ecfe98`, device flow, and the owner wording: pi.yaml `login.grants.github-copilot` matches word for word. `BuiltTreeCheck.theCopilotAppPoreloGrantsIsTheOneTheShippedPiSignsInWith` holds the tree to the id.
- Default provider `openrouter`: pi.yaml `provider.default`. `github-copilot` is used throughout `PiFindings`, `PiSignInFacts` and `PiCredentialExtractor`.
- "Not your credential": `PiRoutingExtension` writes only the task token as `apiKey`, with the broker as `baseUrl`.
- Local links `doc/images/crazy-chicken.svg`, `doc/index.md`, `build.md`, `CONTRIBUTING.md`, `SECURITY.md`, `doc/decisions.md`, `LICENSE`, and `decisions.md` from inside `doc/`: all exist. `mkdocs.yml` navigates both pages.

### Deviations found so far

1. **Copilot model sentence in `doc/index.md`** ("a model it does not list is answered with `400`") reflects 0.85.0 (decisions.md). [PI44](README.md#pi44) measured 1.4.0 and read 1.5.0. A bare unknown id stops with Pi's `Model "x" not found`. A listed model the account lacks gets 400 `model_not_supported`. Whether `gpt-5-mini` is still in 1.5.0's `pi-ai/dist/providers/data/github-copilot.json` has to be read in the tree. Proposed fix: say a model not in the catalogue is refused by Pi as not found, and one the account lacks gets 400 from Copilot. Keep the example only if it is still listed, otherwise use `gpt-5.6-terra`, which [PI44](README.md#pi44) and decisions.md measured. Do not anticipate [PI44](README.md#pi44)'s fix: the doc describes today's code, and [PI44](README.md#pi44) already plans to change these sentences.
2. **Documentation site address in `README.md`** `https://porelo-ai.github.io/pi/`: the repository is `porelo-ai/porelo-pi` (pom.xml, CHANGELOG rename entry). `mkdocs.yml` calls `doc/` "this repository's section on the documentation site". `SECURITY.md` and `CONTRIBUTING.md` link the project site under `porelo-ai.github.io/porelo-project/`. So the `/pi/` path is doubtful. Check it online and set it to the address that answers.
3. **Size "about 79 MB"**: no source in the code holds it. build.md gives ~74 MB for `pi-tree.tar.gz` and ~79 MB for the package, and index.md applies 79 MB to what is "copied into the task's image". Measure the `.deb` and `.rpm` after `-Pdist`, and the tarball, then name which one the figure is (package or tree) and correct the number. The comment drift on package counts (pi.yaml says 162, the script says 167, the lock holds 123 entries) is not in either document. Correct it only if the operator wants it in scope.
4. **"These two are the providers measured with Pi"** holds as decisions.md states it. But `PiFindings` knows anthropic, openai and google too, and index.md says nothing about `porelo vault import pi` (pi.yaml `config_dir`, measured in decisions.md on 1.4.0). A missing statement is not a false one. Adding an import line is an operator choice (see toBeChecked).

### Steps

1. Build `./mvnw -s settings.xml -Pnative,dist verify` (or with `PI_TREE_DIRECT=1`). Record `du -h` of `target/pi-tree.tar.gz`, the `.deb` and the `.rpm`, and the unpacked size under `/opt`.
2. In the unpacked tree, read `pi-ai/dist/auth/oauth/github-copilot.js` for `Editor-Version: vscode/` and `Copilot-Integration-Id: vscode-chat`, and `pi-ai/dist/providers/data/github-copilot.json` for the example model. Also read the base URL Pi uses for Copilot (`api.githubcopilot.com` or an `api.individual.` variant), which it reaches only through the broker.
3. In Porelo at the pinned version, read the provider definitions for `openrouter.ai` and the Copilot upstream host. Read the broker's log lines for `401 from the provider`, `401 token not accepted`, `401 this task's token expired at`, `503` and the `request` line, plus the path `/run/user/<uid>/porelo/<container>/` and the names `vault.log` and `relay.log`. Check that `--token-hours`, `porelo vault put --type api-key`, `vault authorize`, `vault unlock`, `project default add/list` and `agents --verbose` exist with these spellings. Compare each against the table and the commands.
4. Check every external link and the site address with an HTTP request.
5. Edit `README.md` and `doc/index.md` only, one statement per change, wording as the surrounding text. Wherever Porelo or the shipped Pi looks wrong instead (for example a log message that differs from what the broker should say), file a new issue in this repository or note it for Porelo, and leave the doc as it is.
6. Run `mkdocs build --strict -d <scratch>` and `check-doc-site doc mkdocs.yml`. The shared-rules workflow covers docs, and the Build ignores `**/*.md` and `doc/**`.

### Out of scope

`build.md` and `doc/decisions.md`, as the issue says, even where they hold the same stale 400 sentence and the 74/79 MB figures.

## To be checked

- Copilot example: keep gpt-5-mini if the 1.5.0 catalogue still lists it, or switch to gpt-5.6-terra, which [PI44](README.md#pi44) and decisions.md measured with a real account?
- Does the 79 MB figure name the .deb/.rpm or what is copied into the image? Today index.md says image, build.md says package and gives 74 MB for the tarball.
- Is the documentation site really at porelo-ai.github.io/pi/, or under the project site (porelo-project) or porelo-pi? Only an online check or the operator can tell.
- Should index.md also name signing in by importing Pi's own host sign-in (porelo vault import pi), which decisions.md measured on 1.4.0, or stay with the two measured providers?
- Is access to Porelo's source or release at porelo.version 0.5.1-SNAPSHOT available for checking the broker log messages, vault.log/relay.log paths, provider hosts and CLI flags? None of these lives in this repository.
- Should the stale package counts in comments (pi.yaml 162, build-pi-tree.sh 167, lock 123 entries) be fixed here too, or left out because they are code comments outside the two documents?

## Findings

- doc/index.md says a model Pi does not list for Copilot is answered with 400; [PI44](README.md#pi44) measured on 1.4.0 that Pi itself stops with Model not found, and 400 model_not_supported comes for a listed model the account lacks.
- README.md gives the documentation site as porelo-ai.github.io/pi/ while the repository is porelo-ai/porelo-pi and SECURITY.md and CONTRIBUTING.md link the project site porelo-ai.github.io/porelo-project/; the path is doubtful.
- The 79 MB in doc/index.md has no source in code; build.md gives about 74 MB for pi-tree.tar.gz and 79 MB for the package, and index.md attaches 79 MB to what is copied into the image.
- Package name, the /opt target, the /usr/local/bin/pi wrapper, fd and ripgrep, the telemetry and version check switches, refused pi.dev, the OAuth app id and its owner wording, and the default provider openrouter all match pi.yaml, the control file and build-pi-tree.sh.
- All local links in both files resolve (image, doc/index.md, build.md, CONTRIBUTING.md, SECURITY.md, doc/decisions.md, LICENSE, decisions.md). External links could not be checked: no network in this task.
- Reachable hosts (openrouter.ai, api.githubcopilot.com), the vault.log/relay.log location and every message in the error table come from Porelo's broker and provider definitions, not this repository, so they need Porelo at the pinned version.
- Comment drift outside the two documents: pi.yaml says Pi is 162 npm packages, build-pi-tree.sh speaks of 167, and the lock has 123 package entries for 1.5.0.
- [PI44](README.md#pi44) (Estimated, M) will change the Copilot model sentences in doc/index.md. [PI74](README.md#pi74) should describe today's behaviour without anticipating that fix, and the two touch the same paragraph.
