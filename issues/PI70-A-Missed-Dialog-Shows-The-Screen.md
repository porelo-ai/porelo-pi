# PI70 — A Missed Dialog Shows The Screen

```json
{
  "$schema": "issue-1.json",
  "id": "PI70",
  "title": "A Missed Dialog Shows The Screen",
  "state": "Backlog",
  "size": "XS",
  "priority": "Low",
  "rank": 2,
  "blockedBy": [],
  "covers": "wake.feature's wait for Pi's question prints the pane's last lines when it fails, as waiting.feature's does, so a missed dialog says what Pi showed instead"
}
```

**Why this size.** One script in `src/acceptance/.../wake.feature`, taken from `waiting.feature` in the same
directory; no code.

**Estimate.** XS: 9 min, the measured median of XS over all repositories. Expect more for the acceptance run that
proves it.

**What must be true.** When the scenario *a message that arrives while Pi asks types nothing into the dialog, and
wakes Pi once it is answered* does not see "Pi asks you" in time, its log shows the pane's last lines. Then a reader
can tell a model that did not call `ask_person` from a dialog that did not draw.

## Why

Found when porelo-pi's `main` went red on 2026-11-15 (Build 43704817244, *Acceptance - fedora*, on dbdf708). The
check at `wake.feature:29` waited 120 s for "Pi asks you" and failed. One minute earlier in the same run,
`waiting.feature` typed the same prompt and saw the dialog within 2 s. The failure was taken as a model flake, and
the job is run again. That could not be shown from the log, because this check prints nothing when it fails, while
`waiting.feature`'s prints the screen's last 25 non-blank lines. Filed on the Coordinator's word.

## Plan

- `wake.feature:29`: read the pane into `screen`, exit zero when its last 12 lines hold "Pi asks you", otherwise
  print "screen, last lines:" and its last 25 non-blank lines and exit 1, the same as `waiting.feature:29-32`.
- **Level:** the acceptance leg on Ubuntu and Fedora, through the Build. Test scripts get no tests of their own.

## Acceptance

- The check prints the pane's last lines when it fails, and both legs pass with it.
