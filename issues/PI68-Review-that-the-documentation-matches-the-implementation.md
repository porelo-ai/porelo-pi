# PI68 — Review that the documentation matches the implementation

```json
{
  "$schema": "issue-1.json",
  "id": "PI68",
  "title": "Review that the documentation matches the implementation",
  "state": "Estimated",
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

**Size:** S - The review is already done and its findings hold on main; what is left is one documentation-only commit of about ten passage edits in doc/decisions.md and build.md, plus the documents tests and mkdocs strict build.

**Estimate:** 75 min

## Plan

Write the report from the findings in the plan, each with document, passage, contradicting file and line, and judgement (all judged documentation; none judged code). Then make one commit touching only doc/decisions.md and build.md, and doc/index.md only if the size is re-measured: decisions.md 48-50 (three extension files, not one), 110 and 258 (drop 'pinned' from the 0.85.0 and 1.4.0 measurements), 236 (settings.json is read too), 375-378 (curl also fetches Node, fd and ripgrep through pinned_fetch); build.md 91-92, 107-109 and 113 (the weekly update follows Pi, Node, fd and ripgrep, one commit each, with --expect-moved for every moved component), 60-62 (relock in the pinned image, not with the host's npm), 194-200 (how target/cp.txt is made), and optionally 167-174 (the suite has more than two halves). Check with ./mvnw -B -s settings.xml -Pdocuments test and mkdocs build --strict. Push for review at the gate.

## To be checked

- Which half of decisions.md is right on the Copilot token: COPILOT_GITHUB_TOKEN (97-98) or only the routing file's apiKey (50). This decides which sentence is rewritten and may need an answer from the Porelo side.
- Tree and package size, 74 or 79 MB (build.md:21,34, doc/index.md:20). A -Pdist build is needed to settle it, and that build is the largest cost risk.
- Whether README.md:15's site path porelo-ai.github.io/pi/ is right, given the repository is porelo-ai/porelo-pi.
- Whether build.md's manual relock is dropped or kept as a fallback with a warning.
- Whether the commit goes now or waits for issues that move the same passages.

## Findings

- Spot-checked on main: decisions.md:48-50 still says 'one file', build.md:107-109 still says Node 'is not touched', build.md:113 still names only pi-coding-agent, and pom.xml:50 pins agent.cli.version at 1.5.0. The plan's findings still hold.
- No measured velocity is recorded for this repository. stats/PI68.jsonl has only state moves, with no wall or estimate minutes, so the minutes are judged from the edit count and one documents test run plus one mkdocs build.
- Uncertainty: the open questions above can change two or three passages, but not the size. If a -Pdist build is run to settle the MB figure, add about 20 to 30 minutes.
- The order asked for one sub-agent per issue. This session has no sub-agent tool, so PI68 was estimated directly in the main session.
