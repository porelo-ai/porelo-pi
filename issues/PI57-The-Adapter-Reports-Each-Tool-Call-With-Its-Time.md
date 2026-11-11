# PI57 — The Adapter Reports Each Tool Call With Its Time

```json
{
  "$schema": "issue-1.json",
  "id": "PI57",
  "title": "The Adapter Reports Each Tool Call With Its Time",
  "state": "Review",
  "size": "S",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "the adapter reads each tool call from its agent's output and hands Porelo its tool, kind, start, end, a shell call's command line, whether it ran in the background, and a sub-agent's name and the calls made under it, in Porelo's common entry",
  "openQuestions": 0
}
```

**Why this size.** one reader over the two events the formatter already reads, a name table and its tests; no background jobs and no sub-agents. It builds against `ToolEvents` of Porelo's agent API, contract version 5.

**Estimate.** S: 27 min, the measured median of S over all repositories (n=167).

**What must be true.** For every tool call the agent makes in an unattended task, Porelo records one entry in its
common tool-call format: the task, the tool as the agent names it, its kind (shell, edit, read, search, web,
sub-agent, mcp or other), when it started and when it ended, for a shell call its command line, whether it ran in the
background, and for a sub-agent the name it was given and the calls made under it. Porelo joins those calls to the
issue whose id names the sub-agent. The adapter only maps its agent's own output; the format, the time a line without
its own time gets, the masking of a command line and where the entry is kept are Porelo's.

## Plan

- Source: Pi's JSON mode, measured with the pinned Pi (CC57 in porelo-claude-code). `tool_execution_start` (`toolCallId`, `toolName`,
  `args`) is START; `tool_execution_end` (`toolCallId`, `isError`, `durationMs`) is END. `PiJsonFormatter` reads the
  same events today.
- Neither line has a clock time, so `at` stays empty and Porelo stamps the line when it arrives. The END passes
  `durationMs` as Pi gives it; porelod dates the start from it when the line has no time.
- `PiAgent` overrides `Agent#toolEvents(String line)`; `PiToolEvents` maps one output line to its
  `ToolEvent(line, id, phase, tool, kind, command, background, jobId, at, name, parent, durationMs)`. It keeps no state between
  lines: Porelo joins a start to its end by `id`, a background job's by `jobId`, and a call to its sub-agent by
  `parent`.
- Kinds: `bash` -> shell (`command` = `args.command`), `edit`/`write` -> edit, `read` -> read, `grep`/`find`/`ls` -> search, any other (an extension's) -> other.
- A shell call's `command` is its whole command line as the stream gives it, unmasked; Porelo masks it before it is
  stored.
- Sub-agents: Pi has none built in; `name` and `parent` stay empty.
- Background: Pi's tools run in the foreground; a command started with `&` inside a shell call is not seen.
  Calls of one turn run side by side, so a start is joined to its end by `id` only.
- Red first: `PiToolEventsTest` feeds recorded lines (two calls in one turn ending in the other order, a shell call, an edit, an unknown tool, a failed call, and a line that is not JSON) and expects the events above; it fails while
  `toolEvents` still answers the API's default, nothing.
- Level: unit tests over recorded output only; the acceptance legs show the entries of a real run once Porelo writes
  them.

## Acceptance

- A recorded unattended run yields one entry per tool call with tool, kind, start, end and, for a shell call, its
  command line; a background job's start and end are joined by its id; a sub-agent carries its name.
- No entry holds a file's content or a tool's output; a command line reaches Porelo unmasked and is stored masked.
- A line the reader cannot read yields nothing and never stops the run.

## What makes it uncertain

- An extension may add a tool that starts sub-agents or jobs of its own; it is mapped as other.

## Decisions

- Only unattended tasks are measured. Attended (tmux) sessions write no JSON stream; whether their tool calls are
  recorded is decided later.
- A shell call keeps its whole command line, not only its program. Porelo masks it before it is stored; the adapter
  passes the line as the stream gives it.
- A task gives each issue a sub-agent of its own, named by the issue's id; an issue's time is its sub-agent's span.
- Pi has no sub-agents, so a Pi task's time is shared evenly among its issues.
