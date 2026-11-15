# PI68 — Review that the documentation matches the implementation

```json
{
  "$schema": "issue-1.json",
  "id": "PI68",
  "title": "Review that the documentation matches the implementation",
  "state": "In analysis",
  "size": "-",
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
