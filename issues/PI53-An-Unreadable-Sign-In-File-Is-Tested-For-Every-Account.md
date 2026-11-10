# PI53 — An Unreadable Sign-In File Is Tested For Every Account

```json
{
  "$schema": "issue-1.json",
  "id": "PI53",
  "title": "An Unreadable Sign-In File Is Tested For Every Account",
  "state": "Review",
  "size": "S",
  "priority": "Medium",
  "rank": 1,
  "blockedBy": [],
  "covers": "the test that an unreadable `auth.json` cannot say runs and passes as root and as a user other than root, never skipped; the failure as uid 1001 reproduced first"
}
```

**Why this size.** one test and one seam in `PiFindings`; no contract.

**What must be true.** The test that an `auth.json` which cannot be read makes Porelo say "cannot say", rather than
pass over a sign-in that may be there, runs and passes for every account: root in the forge's Build and a user other
than root in a task. It is never skipped.

## Why

`PiFindingsTest.aFileThatCannotBeReadCannotSay` takes every permission off the file and skips itself when the account
still reads it. The forge's runner is root, so the Build always skips it; in a task, as uid 1001, it was reported to
fail. So the guard runs nowhere it is trusted, and where it runs it is red.

## Plan

Read in today's `PiFindings` (`JsonFile`, which turns an `IOException` from `Files.readString` into `UNREADABLE`) and
`PiFindingsTest`.

- **Reproduced first:** the test as uid 1001, in a container of the task image on a fresh clone, once with the
  clone's files owned by that account and once copied in as it: it ran and passed both times, 47 run and none
  skipped. The failure reported from the task does not happen; what holds is that root, which runs the forge's
  Build, skips it.
- **The test independent of the account:** `PiFindings` reads its files through a reader it is given, the file
  system's by default; the test gives one that refuses `auth.json` with `AccessDeniedException`, so root meets the
  refusal too. The mode, its restore and the `assumeFalse` go. A second test makes `auth.json` a directory, which
  every account fails to read as a file.
- **Red first:** with `UNREADABLE` mapped to a file that is absent, the directory test failed; with
  `AccessDeniedException` alone mapped so, the reader's test failed, which a directory alone would not show.

## Reading

Read by another model: a directory alone left the refusal of a mode untested, since it fails a read with another
exception; taken, by the reader the test gives.

## Acceptance

- The test runs, and passes, as root and as uid 1001; a test result lists it as run, not skipped.
- It was seen red with the unreadable state passed over.
