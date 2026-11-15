# PI72 — The Running View Says What Each Tool Did

```json
{
  "$schema": "issue-1.json",
  "id": "PI72",
  "title": "The Running View Says What Each Tool Did",
  "state": "Implementing",
  "size": "S",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "the running view of an unattended Pi task says each tool call with its key input from `args` and each result by whether it failed and its first line or its size, instead of only the tool's name"
}
```

**Why this size.** One class, the formatter, and its tests; the recorded run already holds tool calls and results with their `args` and `result`.

**Estimate.** S: 27 min, the measured median of S over all repositories (n=168).

**What must be true.** A person reading an unattended Pi task sees for each tool call what it was called on, and for each result whether it failed and how it began or how long it was; never the whole output.

## Why

Part of ST369, *Findings of the second live test on a rented host* (High). In the operator's live test (porelo
snapshot.553, porelo-agent-claude snapshot.186, the build with the raw-lines fix) the running view of an unattended Claude Code task
showed almost nothing of the work: its lines were only `[Bash]`, `> [tool_result]`, `[thinking]` and once `[git] push`,
over and over. The operator asked whether it is normal to see nothing of the tools. It is not: a reader must see what
each tool did, in short.

Checked in this repository for the same gap, and it has it: `tool_execution_start` is shown as `[<toolName>]` alone,
though the event carries `args` (`bash` its `command`, read and edit tools a `path`), and `tool_execution_end` is
shown only when it failed, as `[<tool> failed]`, without its message; a result that worked is not shown at all.

## Plan

- **A tool call** on one line with its key input from `args`, folded to one line and cut: `bash` its command's first
  line, a read, write or edit tool its path, a search its pattern; any other its one most telling string field, or its
  name alone.
- **A result** by `tool_execution_end`: failed or not (`isError`), with the first non-blank line of `result.content`'s
  text, cut, or its size (`12 lines`); never the whole output.
- **Line breaks** in a tool's input folded here, which [PI71](README.md#pi71) names for a tool name.
- **Tests:** each rule seen red first against the recorded run and single lines; no test holds a worded line verbatim.
- **Level:** unit tests of the formatter.

## Acceptance

- Rendered through the formatter, the recorded run shows each tool call with its key input and each result with its
  failure and its first line or size.
- A result that failed always says so with its first line, and its test fails when the rule is taken out.
