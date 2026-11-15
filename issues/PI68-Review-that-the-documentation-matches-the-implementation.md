# PI68 — Review that the documentation matches the implementation

```json
{
  "$schema": "issue-1.json",
  "id": "PI68",
  "title": "Review that the documentation matches the implementation",
  "state": "Analyzed",
  "size": "S",
  "priority": "Medium",
  "rank": 9,
  "blockedBy": [],
  "covers": "Review that the documentation matches the implementation"
}
```

**What must be true.** - Once, on main as it stands, README.md, doc/index.md, doc/decisions.md, build.md and CONTRIBUTING.md were read against the code, scripts and configuration they describe.
- A report lists every deviation found: the document and passage, the code it contradicts (file and line), and whether the documentation or the code is judged wrong.
- One commit, for review at the gate, corrects every passage where the documentation is wrong.
- Where the code is judged wrong, it is unchanged and only named in the report, for a person to decide.
- When nothing deviates, the report says so and no commit is made.

**Size:** S - The review is already done here; what remains is one documentation-only commit of about ten passage edits in two files, with no code change.

## Plan

**Base design, read in today's code.** PI68 is a one-time review. It reads five documents against what they describe, writes a report, and makes at most one commit, documentation only. No code moves.

**What each document is read against:**

| Document | Read against |
|---|---|
| `README.md`, `doc/index.md` | `src/main/resources/agent/pi.yaml` (install, `refused_domains`, launcher, `login.grants`), `PiContainerSetup`, the package size `build-pi-tree.sh` reports |
| `doc/decisions.md` | `PiContainerSetup`, `PiRoutingExtension`, `PiAskExtension`, `PiStatusExtension`, `PiFindings`, `pi.yaml`, `pom.xml` (`agent.cli.version` 1.5.0, `pin.*`, `porelo.release.*`), `settings.xml`, `.mvn/`, `buildtools/*.sh`, `.github/workflows/*.yml`, `.github/actions/*`, `dependabot.yml`, and the tests it names (`PinAgreementTest`, `UpdateRulesTest`, `UpstreamSnapshotsTest`, `ReleaseChannelTest`, `WaitingDeclarationTest`, `SessionIdDeclarationTest`, `BuiltTreeCheck`) |
| `build.md` | `pom.xml` (profiles `documents`, `native`, `dist`, `acceptance`; version 0.5.7-SNAPSHOT), `build-pi-tree.sh`, `follow-upstream.sh`, `update.yml`, `build.yml`, `artifactory-smoke.yml`, `src/acceptance` |
| `CONTRIBUTING.md` | `build.md` anchors, `pom.xml` profiles, `src/acceptance` features |

**Done in this analysis.** Every passage was read once against the code on main. What matched, briefly: CONTRIBUTING.md and README.md fully. In index.md, the commands, the launcher variables, `pi.dev` refused, the Copilot app id and the 400 for an unlisted model. In build.md, the package version 0.5.7, the podman skip, `PI_TREE_DIRECT`, the acceptance profile activation, the `@credential` skip, the update cron (Tuesday 05:17), branch `update/pi`, `PORELO_E2E_MODEL` in `build.yml`, the publish variables and the smoke workflow. In decisions.md, the snapshot profiles and `MAVEN_ARGS`, the Dependabot directories and cooldown, `check-actions`, the Maven wrapper digest, both scripts' regression matrices, `umask 022`, the four builder steps, `--ignore-scripts`, the ask dialog and its `in_last_lines: 7`, `at_rest`, the session declaration and the root-owned stores. The deviations are listed under findings, each with a judgement.

**The commit (the issue's third point), once approved.** One documentation-only commit at the gate, touching `doc/decisions.md` and `build.md`, and `doc/index.md` only if the size check below says so:
1. decisions.md:48-50: the container gets three files (`porelo-status.ts` and `porelo-ask.ts` always, `porelo-route.ts` when brokered and with a token), not one. Keep the three load-bearing points about the routing file.
2. decisions.md:110 and :258: drop "pinned" from the 0.85.0 and 1.4.0 measurements (the pin is 1.5.0), or say "then pinned".
3. decisions.md:236: the adapter also reads `settings.json`, for `defaultProvider`/`defaultModel`, as line 255 already implies.
4. decisions.md:375-378: curl, through `pinned.sh`'s `pinned_fetch`, also downloads Node, fd and ripgrep, each checked against its pin. shfmt is not the only one.
5. build.md:107-109: the update job does move Node, and fd and ripgrep, in the same run, one commit each, Node before the CLI. Restate it as "a Pi bump does not move Node; the job moves each pin as its own commit".
6. build.md:113: `--expect-moved` is given for every component that moved (`steps.follow.outputs.components`), not only `pi-coding-agent`.
7. build.md:91-92: the weekly job asks about Pi, Node, fd and ripgrep, not only npm's `latest`.
8. build.md:60-62: the manual relock runs `npm install --package-lock-only` with the host's npm. That contradicts lines 101-104 and CONTRIBUTING's "no Node toolchain is installed here". Point to the release tool's relock in the pinned image instead.
9. build.md:194-200: say where `target/cp.txt` comes from (`./mvnw -B -q -Pci-tools -s settings.xml dependency:build-classpath -Dmdep.outputFile=target/cp.txt`, as `build.yml` does).
10. build.md:167-174: "Two halves" names only install and tier 2. The suite also holds terminal, session, waiting and wake. Optional wording.

**What the commit must not do:** change code, `pi.yaml`, workflows, `issues/` or `stats/`. The deviations judged as code or Porelo-side, or that need a measurement, go into the report only.

**Checks for the commit:** `./mvnw -B -s settings.xml -Pdocuments test` (the documents tests read these files), and `mkdocs build --strict` for `doc/`. The `shared-rules` `check-doc-site` also runs on push.

## To be checked

- decisions.md:97-98 or :50: does Porelo's github-copilot provider still hand Pi COPILOT_GITHUB_TOKEN, or does only the routing file's apiKey carry the token? Its answer decides which sentence is rewritten.
- Tree and package size: re-measure with -Pdist (build-pi-tree.sh:250) to choose between 74 and 79 MB for build.md:21,34 and doc/index.md:20.
- README.md:15: is the documentation site really at porelo-ai.github.io/pi/, given that the repository is porelo-ai/porelo-pi?
- build.md:60-62: should the manual relock be dropped for the release tool's relock in the pinned image, or kept as a fallback with a warning?
- The stale 'measured on 0.85.0 / 1.4.0' statements (decisions.md:110,136,258): correct only the word 'pinned', or schedule fresh measurements on 1.5.0?
- Passages that depend on Porelo and cannot be checked here: index.md:71 (porelo agents --verbose), build.md:76 (porelo agents --supply-chain), build.md:85 (the update writes its changelog line), build.md:160 (amd64 is one core, 2 GB).
- Whether the doc-only commit goes now or waits for issues that move the same passages (for example the Copilot routing or the supply-chain flags).

## Findings

- doc/decisions.md:48-50 says the container gets 'one file'. PiContainerSetup.java:76-97 always writes porelo-status.ts and porelo-ask.ts, plus porelo-route.ts when brokered. Doc wrong.
- doc/decisions.md:50 says 'No environment variable carries either'. decisions.md:97-98 says Pi reads the task's token from COPILOT_GITHUB_TOKEN, while PiRoutingExtension.java:43 passes the token as apiKey and nothing in this repository sets that variable. The document contradicts itself, and which half is right is to be checked.
- doc/decisions.md:110 says 'Read in the pinned 0.85.0' and :258 says 'the pinned Pi 1.4.0'. pom.xml:50 pins agent.cli.version 1.5.0. Doc wrong (stale wording; the measurements stand).
- doc/decisions.md:236 says the adapter reads 'auth.json and models.json and nothing else'. PiFindings.java:38,96 also reads settings.json for the default provider. Doc wrong.
- doc/decisions.md:136 lists 'both extensions loaded (porelo-route.ts, porelo-status.ts)'. Today there are three (PiContainerSetup.java:79 adds porelo-ask.ts). It was measured on 0.85.0 before the ask tool, so it is historical. A note would do. Doc stale, low.
- doc/decisions.md:375-378 says 'the one tool curl downloads, shfmt'. build-pi-tree.sh:176-184 fetches Node, fd and ripgrep with curl through .github/actions/pinned-jdk/pinned.sh:70-77. All are digest-checked, so only the wording is wrong. Doc wrong.
- build.md:107-109 says the Node runtime 'is not touched' by the update. update.yml:1-3,14 and follow-upstream.sh move Node, fd and ripgrep in the same run, as decisions.md:418-431 says. Doc wrong.
- build.md:113 says '--expect-moved pi-coding-agent excuses only the package being updated'. update.yml:136-141 passes --expect-moved for every moved component. Doc wrong.
- build.md:91-92 says the weekly run 'asks the npm registry what latest points at'. update.yml:1-3 follows Pi and the three tree pins. Doc incomplete.
- build.md:60-62 tells you to relock with the host's npm (npm install --package-lock-only in src/main/npm). That contradicts build.md:101-104 (relock in the pinned Node image) and CONTRIBUTING.md:13-14 (no Node toolchain here). Doc wrong.
- build.md:194-200 runs java -cp "$(cat target/cp.txt)" but never says how cp.txt is made. build.yml makes it with -Pci-tools dependency:build-classpath, a profile pom.xml does not declare (it presumably comes from porelo-parent). Doc incomplete.
- build.md:167-174 describes 'Two halves' (install, tier 2). src/acceptance also holds terminal, session, waiting and wake features. Doc incomplete, low.
- build.md:21 gives the tree as ~74 MB and build.md:34 the package as about 79 MB. doc/index.md:20 calls the tree itself 'about 79 MB'. The two documents disagree, and the real figure needs a -Pdist build (build-pi-tree.sh:250 prints it).
- README.md:15 names the docs as porelo-ai.github.io/pi/, while pom.xml:22-34 names the repository porelo-ai/porelo-pi. Whether the site path is right cannot be told from this repository.
- Read without deviation: CONTRIBUTING.md (commands, profiles, anchors #acceptance and #bumping-the-pi-version), README.md links, and index.md's commands, launcher variables, pi.dev refusal, Copilot app id and providers.
- Read without deviation in decisions.md: the snapshot profiles and MAVEN_ARGS (build.yml:24), the rrf filter (org.fuin.porelo), Dependabot (directories, 3-day cooldown, grouped), the Maven wrapper sha256, and the regression matrices of build-pi-tree.sh and follow-upstream.sh.
- Read without deviation in decisions.md: umask 022, the four builder steps, the porelo.yml Node pin, --ignore-scripts on both npm ci, the ask dialog title with in_last_lines 7, at_rest lacks, the session declaration, and the root-owned stores at 1777/0444.
- No deviation was judged a code defect. Every one is in the documentation, or depends on Porelo or on a measurement this repository cannot make.
- The order asked for one sub-agent per issue. This session has no sub-agent tool, so PI68 was analysed directly in the main session.
