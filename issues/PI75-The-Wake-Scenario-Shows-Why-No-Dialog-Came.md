# PI75 — The Wake Scenario Shows Why No Dialog Came

```json
{
  "$schema": "issue-1.json",
  "id": "PI75",
  "title": "The Wake Scenario Shows Why No Dialog Came",
  "state": "Implementing",
  "size": "S",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "the wake scenario's wait for Pi's ask dialog shows the screen when it fails, and its cause is fixed rather than run again"
}
```

**Why this size.** One scenario's wait rewritten as `waiting.feature`'s already is, the cause read from the screen it
then shows, and one fix in the scenario; no contract.

**Estimate.** S: 28 min, the measured median of S over all repositories.

**What must be true.** The scenario "a message that arrives while Pi asks types nothing into the dialog, and wakes Pi
once it is answered" passes on every leg without a second attempt, and when it fails, its log shows what Pi showed.

## Why

On the fedora leg of a Build on 2026-11-15, its first attempt failed and its second passed on the same commit: after
Pi was asked to use `ask_person`, `Pi asks you` did not show in the last 12 lines of the pane within 120 seconds. The
wait's script ends in `grep -qF`, so its failure printed nothing, and the cause is not measured. Inferred: the model
under `PORELO_E2E_MODEL` answered without calling the tool, or called it later than 120 seconds. `waiting.feature`
drives the same dialog with the same words and already prints the screen when the dialog does not come.

## Plan

- **Red first:** the wake scenario's wait takes `waiting.feature`'s form: the pane read once, `Pi asks you` looked
  for in its last 12 lines, and on failure its last 25 lines that are not blank printed. Seen to fail with the
  dialog's title changed, the screen in the failure message.
- **Measure:** the scenario run on the build box until it fails or passes ten times, reading each screen.
- **The cause decides the fix**, never a retry or a longer bound alone: the model answering in text calls for a
  prompt that leaves no choice or a model pinned for tool calls in both scenarios; a bound below the measured time
  of the slowest pass calls for a bound from that measure; a Porelo or Pi fault is filed where it belongs.

## Acceptance

- Ten runs of the scenario on the build box pass, each with its time recorded; and the wait, with the title
  changed, fails showing the screen.
