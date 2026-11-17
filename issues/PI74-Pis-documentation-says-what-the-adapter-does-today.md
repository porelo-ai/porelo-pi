# PI74 — Pi's documentation says what the adapter does today

```json
{
  "$schema": "issue-1.json",
  "id": "PI74",
  "title": "Pi's documentation says what the adapter does today",
  "state": "Estimated",
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

**Size:** S - Documentation only in three files (index.md section, four decisions.md rewordings, size figures); the one slow step is an optional native package build to measure the size.

**Estimate:** 120 min

## Plan

### Estimate

The plan stands as analyzed. I re-read the facts it relies on in today's tree, and they hold:

- `pom.xml` pins `agent.cli.version` 1.5.0, Node 22.23.4, fd 10.5.2 and rg 15.2.0.
- `doc/decisions.md` line 110 reads "Read in the pinned 0.85.0", line 207 reads "the two extension files", and line 258 reads "Measured on the pinned Pi 1.4.0".
- `doc/index.md` line 20 and `build.md` lines 21 and 34 give the undated ~74 MB and about 79 MB figures.
- `pi.yaml` carries `--no-approve`, `--no-skills` and `effort` with `--thinking {effort}`. The efforts are noted as read in `pi --help` on 1.4.0.
- `PiAgent`'s Javadoc says "Four overrides", but the class has six `@Override` methods.

### Time, by step

- index.md: the new section "Working with Pi in a task", vault import and the unattended log shapes. About 45 min of writing against the Java and yaml.
- decisions.md: the four rewordings, plus a `--no-skills` entry and its table row. About 20 min.
- The package size: remove the figures in about 5 min, or measure them with a `-Pnative,dist verify` build (native image plus podman) in about 40 to 60 min of mostly waiting.
- mkdocs `--strict`, check-doc-site, check-citations, CHANGELOG line and the push. About 15 min.

Total: about 90 min if the figures are removed, about 140 min if the package is measured. The estimate takes 120 min, which leans toward measuring.

## To be checked

- Package size: measure with a -Pnative,dist build at the current pins (adds about 50 min, needs podman and GraalVM) or remove the figures? This is the largest swing in the estimate.
- Is the Porelo command that shows what the adapter found of Pi on the host (porelo agents or the vault import listing) to be named in index.md? It cannot be read in this repository.
- [PI72](README.md#pi72) and [PI73](README.md#pi73) are still in Review: if either changes the log line shapes or the effort declaration before this issue is implemented, the index.md text must follow it.

## Findings

- No measured velocity is recorded under stats/ (no wall_min values for any issue yet), so the minutes are a judgment for an S documentation issue, not derived from measured history.
- Confirmed in today's tree: decisions.md lines 110, 207 and 258 and the 79 MB and 74 MB figures stand as the plan says; pom.xml pins Pi 1.5.0.
- Confirmed: the PiAgent Javadoc says Four overrides while the class has six @Override methods; this is a code comment for its own issue, not this one.
