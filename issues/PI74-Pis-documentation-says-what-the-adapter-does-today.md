# PI74 — Pi's documentation says what the adapter does today

```json
{
  "$schema": "issue-1.json",
  "id": "PI74",
  "title": "Pi's documentation says what the adapter does today",
  "state": "In analysis",
  "size": "-",
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
