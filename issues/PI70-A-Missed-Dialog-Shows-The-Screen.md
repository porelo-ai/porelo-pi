# PI70 — A Missed Dialog Shows The Screen

```json
{
  "$schema": "issue-1.json",
  "id": "PI70",
  "title": "A Missed Dialog Shows The Screen",
  "state": "Review",
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

## Measured

- **Red:** with the title changed to one Pi never shows, the wait failed after 120 seconds and printed the pane's
  last lines: Pi had called `ask_person` and its dialog `Pi asks you` was up.
- **Ten runs** on fedora with Porelo 0.5.1~snapshot.561 and `z-ai/glm-5.3-flash`, the scenario alone, each from a
  removed task and project: all ten passed at the first attempt, failsafe reruns nothing. Wall time per run, Maven
  included: 137, 144, 229, 147, 141, 139, 143, 141, 145 and 138 seconds; where the third spent its extra 85 seconds
  was not kept.
- **Not reproduced:** the failure seen once on the fedora leg did not come back in ten runs, so its cause is still
  inferred (the model answering without calling `ask_person`, or calling it after more than 120 seconds), and no
  bound or prompt was changed for it. A failure from now on prints what Pi showed, and its cause is filed from that
  screen.
