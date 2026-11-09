# PI53 — An Unreadable Sign-In File Is Tested For Every Account

**Status:** Implementing.
**Size:** S - one test and, if the reproduction needs it, one seam in `PiFindings`; no contract.

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

- **Reproduce first, at the lowest level that shows it:** the unit test run as uid 1001 in a container of the task's
  image on this repository's clone, and the failure read: what was asked and what came back.
- **Then the test independent of the account:** where the failure is the account's (a mode root reads through, a
  file system that ignores modes), the unreadable file is made by a seam in `PiFindings` that reads the file, which
  the test makes throw `AccessDeniedException`; the mode-based setup and its `assumeFalse` go. Where the failure is
  the code's, it is fixed against the reproducing test.
- **Red first:** the reproduction above, and the new test against `PiFindings` with `UNREADABLE` mapped to a sign-in
  that is passed over.

## Acceptance

- The test runs, and passes, as root and as uid 1001; a test result lists it as run, not skipped.
- It was seen red with the unreadable state passed over.

## To be checked

- What the failure as uid 1001 said, read from the reproduction rather than from the report.
