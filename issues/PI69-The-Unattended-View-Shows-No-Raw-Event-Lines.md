# PI69 — The Unattended View Shows No Raw Event Lines

```json
{
  "$schema": "issue-1.json",
  "id": "PI69",
  "title": "The Unattended View Shows No Raw Event Lines",
  "state": "Implementing",
  "size": "S",
  "priority": "Medium",
  "rank": 1,
  "blockedBy": [],
  "covers": "the unattended view of a Pi task shows what the agent says and does, never a raw JSON event line: an unknown event dropped unless it names an error, read in the code beside the same finding for Claude Code"
}
```

**Why this size.** One class, the formatter, and its tests; no contract, since its output is read only by a person.

**Estimate.** S: 27 min, the measured median of S over all repositories (n=168).

**What must be true.** A person reading an unattended Pi task in the window sees the agent's messages, its tool calls and its end; no line of raw JSON.

## Plan

- **What is wrong today, read in the code, not yet seen.** `PiJsonFormatter` silences a fixed set of lifecycle events and words a message's end, a tool's start and failure and the agent's end; any other event is returned as raw JSON cut at 300 code points, by the class's own rule that a new event is what nobody has seen before.
- **The new rule.** An unknown event is dropped unless it carries an error (an `error` field, `isError`, a provider's status), which shows as `[error] <message>`; an event the person can use is worded.
- **What still shows, by design.** A line that says something went wrong - an error or a warning, a failed run, a refused request - is never dropped: it shows in words, with its message cut at 300 code points as today. A line that is not JSON at all still shows as it is, without control characters.
- The class comment's rule that an unknown event passes through is replaced by the one above, with its reason.
- **Tests.** The harm guarded is an error hidden from the person: the existing tests that an error, a failed run and a non-JSON line still show stay, and one per new rule shows an unknown event dropped while an unknown event that names an error still shows, each seen red first. The words of a worded event are not held verbatim by a test.

## Acceptance

- A recorded unattended run of the pinned CLI rendered through the formatter holds no line that begins with `{`.
- An error event of an unknown type still shows, and its test fails when the error rule is taken out.

## To be checked

- **The events 1.5.0 writes.** Which event types an unattended run of the pinned CLI writes beyond the handled ones, read from a run on a VM, values replaced.
