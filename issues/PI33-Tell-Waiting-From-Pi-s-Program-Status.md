# PI33 — Tell Waiting From Pi's Program Status

**Status:** Idea.

**What must be true.** Porelo can say whether Pi waits for a person from what Pi reports of itself, and not
only "cannot say", and wakes it only where that report says it is at rest.

## Why

`pi.yaml` declares no `waiting`, on purpose: measured on 0.85.0, nothing on Pi's screen tells a question it
put from a turn that is done, so Porelo says "cannot say" and never wakes Pi.

**Pi 1 reports its state by OSC 7501**, the Program Status Protocol (`docs/terminal-setup.md`, "Program
status", in 1.3.0's package):
- `working`, a run or compaction in progress;
- `blocked`, an extension dialog or login waiting;
- `done`, a run finished;
- `error`, a run ended with an error not retried;
- `idle`, Pi started or the run was cancelled.

**What that does not answer, read in the same page:**
- **`blocked` is an extension dialog or a login only.** A question the model writes as plain text and then
  waits on, the case 0.85.0 measured, reports `done` like any finished turn.
- **tmux does not forward the reports,** and Pi sends them only after the terminal answers the protocol's
  support query, unless `PI_PROGRAM_STATUS=1`. A task's session is in tmux.
- **The status extension this package installs already sees the dialogs** (`ui_prompt_start`), and Porelo
  does not read an agent's own files.

So the idea holds only if Porelo can read the reports past tmux, and if `done` with a question open can be
told from `done` without one. Neither is shown. An Idea until someone measures both on Pi 1, after
[PI32](README.md#pi32).
