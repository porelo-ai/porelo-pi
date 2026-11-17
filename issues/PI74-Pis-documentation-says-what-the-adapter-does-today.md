# PI74 — Pi's documentation says what the adapter does today

```json
{
  "$schema": "issue-1.json",
  "id": "PI74",
  "title": "Pi's documentation says what the adapter does today",
  "state": "Analyzed",
  "size": "S",
  "priority": "Medium",
  "rank": 9,
  "blockedBy": [],
  "covers": "Pi's documentation says what the adapter does today"
}
```

**What must be true.** - doc/index.md describes, for a person using Pi in a task, what the code and src/main/resources/agent/pi.yaml do today, among them:
  - the ask_person tool: its dialog "Pi asks you" reads as waiting for a person, a question written as plain text does not, and Pi at rest at its prompt is woken for messages, handed-in files and build verdicts;
  - the reasoning effort a start can name (--thinking with off, minimal, low, medium, high, xhigh, max), and that none named keeps Pi's default;
  - that --no-approve and --no-skills are passed: a repository's own .pi extensions and skills do not load;
  - porelo vault import from Pi's auth.json on the host, and what the adapter reports of Pi found on the host (never a value);
  - what an unattended run shows in its log (tool calls with their key input, results, errors) and what it answers as its last message;
  - that a task that comes back continues its session;
  - that a /login inside a task stores nothing, Pi's directory being /var/lib/pi/agent with read-only sign-in stores.
- No document in the repository (README.md, doc/index.md, doc/decisions.md, build.md, AGENTS.md) states something the implementation or the pinned versions in pom.xml contradict: statements tied to a Pi version say which version they were measured or read on, and a statement given as about the pinned Pi (such as "Read in the pinned 0.85.0" in decisions.md) names the version pinned now, 1.5.0, or is re-measured or reworded as a past measurement.
- The package size given in doc/index.md and build.md ("about 79 MB") is the measured size of the package built with the current pins, or the figure is removed.
- The documentation changes nothing in the code; where a gap between documentation and code is found that is a bug in the code, it is reported as its own issue rather than fixed here.
- mkdocs build --strict succeeds on doc/.

**Size:** S - Documentation only: one new section in index.md, four rewordings in decisions.md, size figures in build.md. The one slow step is a -Pnative,dist package build with podman to measure the size, unless the figure is removed.

## Plan

### What today's code does and the documents do not say

`doc/index.md` (85 lines) covers install, sign-in, start, reach and `vault.log`. It does not mention anything below. Each item is read in today's code:

- **ask_person.** `PiContainerSetup` always writes `PiAskExtension` (`/var/lib/pi/agent/extensions/porelo-ask.ts`). It registers `ask_person`. Attended, it opens Pi's input dialog titled `Pi asks you`. Unattended, where there is no UI, it answers at once: "no person is attending; decide yourself". `pi.yaml` `session.waiting.screen` reads that title in the last 7 non-empty lines as waiting. A question written as text reads as not waiting (the operator's decision of 2026-11-11, in decisions.md). `session.at_rest` (`(auto)` shown, lacking `Working ─` and `escape/ctrl+c cancel`) is what lets Porelo wake Pi at its prompt for messages, handed-in files and build verdicts (CHANGELOG, Unreleased).
- **Effort.** `pi.yaml` `effort`: `--thinking {effort}` with off, minimal, low, medium, high, xhigh, max. With none named, nothing is passed and Pi keeps its default. This was read in `pi --help` on 1.4.0 and not on the pinned 1.5.0 (the yaml comment says so).
- **Sandbox arguments.** `sandboxed.arguments: [--no-approve, --no-skills]`: a repository's own `.pi` extensions and `.agents/skills` do not load, and no skill in Pi's directory is listed. The guide file comes after them through `--append-system-prompt`.
- **Host side.** `config_dir: ~/.pi/agent`. `PiFindings` reads only `auth.json` and `models.json`, and reports provider, kind and path, never a value. `PiCredentialExtractor` serves `porelo vault import pi --provider <id>`. The table in decisions.md says which entries can be imported.
- **Unattended log.** `PiJsonFormatter`:
  - The assistant text comes from `message_end`, without its thinking.
  - `[tool] <key input>` comes from `args` (command, pattern, path, file_path, filePath, url, query, description, else intent).
  - Results show as `[tool done] <first line | N lines | N parts>` or `[tool failed] <first 3 lines>`.
  - A failed assistant message shows as `-- error: …`.
  - Unknown events show only as `[error]` / `[warning]`.
  - Lines are cut at 300 code points and control characters are removed.
  - `ended()` reports a provider refusal with its HTTP status. `waiting.last_message` makes the run's answer the text parts of its last assistant `message_end`.
- **Continuation.** `session.supports_resume` and `--session`, with the id read from the first `{"type":"session"}` record, or from the newest file in `/var/lib/pi/agent/sessions/--workspace--`.
- **/login.** The launcher exports `PI_CODING_AGENT_DIR=/var/lib/pi/agent` (root's, sticky 1777). `auth.json` and `mcp-auth.json` hold `{}`, mode 0444. A `/login` stores nothing, but the key it was typed with is in Pi's memory: this is the accepted risk in decisions.md.

### Steps

1. **doc/index.md.** Add a section "Working with Pi in a task", written for the person using it, with one short paragraph or list item per point above. Link each to its decision in `decisions.md` instead of repeating the reasoning.
   - Add `porelo vault import pi --provider <id>` under "Sign in", and say that the adapter reports a sign-in found on the host by provider, kind and path, never a value.
   - Add a short "What an unattended run shows" part with the line shapes listed above.
   - Name the Pi version each statement was measured or read on, where it is version-bound (efforts: read on 1.4.0; the ask dialog: measured on 1.5.0).
2. **doc/decisions.md, version statements.**
   - Line 110: "Read in the pinned 0.85.0, not run". Either re-read it in the 1.5.0 tree (`dist/` of the shipped package), or reword it as "Read in 0.85.0, when it was pinned".
   - Line 258: "Measured on the pinned Pi 1.4.0". Reword as "Measured on Pi 1.4.0", or re-measure on 1.5.0.
   - Line 207: "the two extension files". There are three now (route, status, ask). Name them.
   - Line 135: the 0.85.0 measurement lists two extensions. Keep it as a past measurement and add that `porelo-ask.ts` came later.
   - The rest of the "measured on 0.85.0" lines (90, 103, 132, 166, 223) already name their version and stay.
   - Add a short decision for `--no-skills`, which is only in `pi.yaml` today, and add it to the table at the top. Also add one row for effort if a decision is worth writing; otherwise index.md alone covers it.
3. **Package size.** Either:
   - run `./mvnw -s settings.xml -Pnative,dist verify` with the current pins (Pi 1.5.0, Node 22.23.4, fd 10.5.2, rg 15.2.0), then put the measured `.deb` size and `pi-tree.tar.gz` size into index.md:20 and build.md:21 and 34, with the date and pins; or
   - remove the figures.

   build.md already gives two numbers (tree ~74 MB, package ~79 MB). Neither is dated.
4. **README.md, build.md, AGENTS.md.** I read them and found no other contradiction with the code or `pom.xml`. The package file names in build.md (0.5.7~snapshot.0) match `0.5.7-SNAPSHOT`.
5. **Check.** Run `mkdocs build --strict -d <scratch>` on doc/, then `porelo-release check-doc-site doc mkdocs.yml` and `check-citations` (both run by shared-rules). No document test reads index.md or decisions.md, so no test changes.

### What it touches

Only `doc/index.md`, `doc/decisions.md`, `build.md` and possibly a CHANGELOG line. No code. Mismatches in code comments (see the findings) are reported, not fixed.

### How it is judged done

- Each bullet of the issue can be found in index.md.
- A grep for "pinned" next to a version in the five documents shows only 1.5.0, or a past-tense wording.
- The size figure is either gone or matches a build log of the current pins.
- `mkdocs build --strict` passes.

## To be checked

- Package size: measure the .deb built with the current pins (needs podman and GraalVM), or remove the about 79 MB figure and the ~74 MB tree figure. Which one?
- decisions.md line 110 (update check) and line 258 (auth check table): re-read or re-measure on the pinned 1.5.0, or reword them as measured on 0.85.0 and 1.4.0? Rewording is cheaper and enough for the issue.
- Effort values were read in pi --help on 1.4.0 and never on the pinned 1.5.0. Is stating 'read on 1.4.0' in index.md enough, or should 1.5.0 help be read first?
- Which Porelo command shows a person what the adapter found of Pi on the host (porelo agents, vault import listing)? index.md should name it, and it is Porelo's wording, not read in this repository.
- Should --no-skills get its own entry in decisions.md? Today its reasoning is only in pi.yaml.
- Should index.md state the accepted risk that a key typed into /login inside a task reaches the agent, or only that nothing is stored?

## Findings

- index.md says nothing about ask_person, effort, --no-approve/--no-skills, vault import pi, the unattended log format, the last message, session continuation or the /login lockdown. All of these are in pi.yaml and the Java today.
- decisions.md line 110 says 'Read in the pinned 0.85.0', and line 258 says 'Measured on the pinned Pi 1.4.0'. pom.xml pins agent.cli.version 1.5.0.
- decisions.md line 207 says 'the two extension files' moved to /var/lib/pi/agent. PiContainerSetup writes three: porelo-route.ts, porelo-status.ts and porelo-ask.ts.
- decisions.md line 135 lists two extensions loaded, measured on 0.85.0. This is correct as a past measurement, but it predates the ask extension.
- build.md gives two size figures, pi-tree.tar.gz ~74 MB (line 21) and package about 79 MB (line 34), and index.md line 20 repeats 79 MB. None is dated or tied to pins.
- A code comment, not a document named by the issue: PiAgent's Javadoc says 'Four overrides', but the class overrides six (finder, credentialExtractor, containerSetup, logFormatter, ended, toolEvents). Worth its own small issue, not fixed here.
- No test reads doc/index.md or doc/decisions.md, so the change needs no test change. CI checks the doc site with check-doc-site and check-citations, and mkdocs --strict per mkdocs.yml.
- The unattended log and last message described here depend on [PI72](README.md#pi72) and [PI73](README.md#pi73) (both in Review). If either changes before [PI74](README.md#pi74), the text must follow it.
