# PI33 — Tell Waiting From Pi's Program Status

```json
{
  "$schema": "issue-1.json",
  "id": "PI33",
  "title": "Tell Waiting From Pi's Program Status",
  "state": "Review",
  "size": "M",
  "priority": "Medium",
  "rank": 7,
  "blockedBy": [],
  "covers": "Porelo tells a waiting Pi from one at rest by what Pi 1 reports of itself",
  "openQuestions": 0
}
```

**Why this size.** Pi's program status (OSC 7501) cannot answer the question by itself: a question the model
writes as text reports `done`, and tmux drops the reports. What can answer it stays in this repository. Porelo's
extension gives Pi a tool to ask a person, the tool opens a dialog with a fixed title, and `pi.yaml` declares that
title as `waiting`. The work is one new extension placed like the status extension, the declaration and its two
guarding tests, the decision rewritten, and the two attended acceptance scenarios on the VM. Porelo core does not
change.

**Estimate.** M: 53 min, the measured median of M over all repositories (n=113).

**What must be true.** Porelo can say whether an attached Pi waits for a person from what Pi itself shows and
reports, not only "cannot say", and types a wake line into Pi only where that says it is at rest.

## Why

`pi.yaml` declares no `waiting`, on purpose. Measured on 0.85.0, nothing on Pi's screen tells a question it put
from a turn that is done, so Porelo says "cannot say" and never wakes Pi. Porelo core wakes an agent only when it
declares both `waiting` and `at_rest`.

**What Pi reports of itself, read in the pinned 1.5.0 package** (`docs/terminal-setup.md`, "Program status", and
`ProgramStatusReporter`). Pi reports one of five states by OSC 7501:
- `working`: a run or compaction is in progress.
- `blocked`: an extension dialog (`select`, `confirm`, `input`, `editor`, `custom`) or a login is open, and its
  title is the message.
- `done`: a run finished. The state comes only from the assistant message's stop reason.
- `error`: a run ended with an error that is not retried.
- `idle`: Pi started, or the run was cancelled.

**Why the report alone does not answer it:**
- **A question in plain text is `done`.** Pi has no built-in tool to ask a person (its tools are bash, read,
  edit, write, grep, find and ls). A question the model writes and then waits on ends the run like any other.
  From the report alone, `done` with a question open looks exactly like `done` without one.
- **The report does not reach Porelo.** Pi writes OSC 7501 only after the terminal answers the support query, or
  when `PI_PROGRAM_STATUS=1` is set. tmux does not forward it. Porelo reads the screen with `tmux capture-pane`,
  and the agent API declares only literal screen rules (`Waiting`, `AtRest`). Reading the raw stream would need
  `tmux pipe-pane` and a new declaration in Porelo's agent API. That crosses repositories and still leaves the
  plain-text question as `done`.

**What makes it answerable.** Pi reports `blocked` only for its own dialogs, and a dialog draws its title on the
screen. So a question asked through a dialog is the one case Pi itself tells apart, and the screen shows it too.
Porelo's extension can give the model a tool that asks through a dialog. Its fixed title then becomes the `waiting`
rule that Porelo already reads.

## Plan

- **`PiAskExtension`** (new, beside `PiStatusExtension`): a TypeScript extension at
  `/var/lib/pi/agent/extensions/porelo-ask.ts`. It registers one tool, `ask_person(question)`, with a
  `promptSnippet` and `promptGuidelines` saying "to ask the person anything, use this tool and never end your turn
  with a question in text".
  - With a UI, the tool calls `ctx.ui.input(<fixed title>, …)`, for example `Pi asks you`. It returns the answer,
    or "no answer" when the dialog is cancelled.
  - Without a UI (`ctx.hasUI` false: an unattended `--print` run), it returns at once with "no person is
    attending; decide yourself and say what you decided".
  - The dialog fires `ui_prompt_start`, which the status extension already records, and Pi reports `blocked`.
- **`PiContainerSetup`** places the new file with the other two. `--no-approve` does not drop it, because it lies in
  Pi's own extensions directory, as the status extension does.
- **`pi.yaml`**:
  - `session.waiting.screen`: one rule, `contains: "Pi asks you"`, with `in_last_lines` covering the dialog and
    `for: "an answer to its question"`.
  - `at_rest.lacks` gains the same title.
  - The long comment that explains why there is no `waiting` is replaced by the measurement, stating the version it
    was taken on.
- **`doc/decisions.md`**, "Waiting for a person is declared as unknown": rewritten to say what is declared now and
  what is still not seen (a question in plain text). The index row changes with it.
- **Tests, red first:**
  - `WaitingDeclarationTest`'s `declaresNothingToReadWaitingBy` is turned around: the test asserts the rule and
    reads a recorded screen of the open dialog as WAITING. It also reads a recorded resting screen, and the screen
    of a question in plain text, as NOT_WAITING. These recorded screens are fixtures captured from the real Pi. It
    goes red while `pi.yaml` declares nothing.
  - `PiContainerSetupTest` expects the ask extension among the placed files.
  - `BuiltTreeCheck` loads the extension against the shipped tree, as it does the status extension.
  - Level: unit tests over recorded screens. Only the attended scenarios need the VM.
- **Acceptance** (VM, attended):
  - `waiting.feature` drives Pi to ask through the tool and expects the kit's "waiting for a person". After the
    answer, it expects "not waiting".
  - `wake.feature` gains a scenario where a message arrives while the dialog is open. No line is typed until the
    dialog is answered, and Pi is woken afterwards.

## Acceptance

- An attached Pi asked to put a question shows the dialog. `porelo task status` reads "waiting" with what Pi waits
  for. Once the dialog is answered, it reads "not waiting".
- A message handed to a Pi task while the dialog is open types nothing into the dialog. When Pi then rests at its
  prompt, the message wakes it.
- An unattended run whose model calls the tool gets the "no person" answer and finishes. The run does not hang.
- What must be seen to fail: with the rule removed from `pi.yaml`, `WaitingDeclarationTest` fails, and
  `waiting.feature` fails on "cannot say".

## What makes it uncertain

- **The model may still ask in plain text.** Pi then reports `done`, and Porelo says "not waiting" and may type a
  wake line into a question nobody sees. The decision kept "cannot say" to avoid exactly this. See the open
  question.
- **Waking starts for Pi.** Porelo core wakes an agent only when it declares both `waiting` and `at_rest`. Declaring
  `waiting` therefore turns waking on for Pi, for messages, handed-in files and build verdicts alike. The wake
  scenario has to show this on Pi's screen.
- **The dialog's drawing is not measured yet.** If the title is cut or wrapped at a narrow width, the rule needs a
  shorter text or a larger region.
- A later Pi may add its own tool for asking a person, or change how `input` draws. `WaitingDeclarationTest` and the
  fixtures are pinned to the measured release, and the update job's acceptance run would catch a change.


## Decisions

- The operator, 2026-11-11: Porelo gives Pi its own "ask the person" tool, and that tool's dialog is declared as
  waiting. Pi becomes wakeable; the decision "waiting is declared as unknown" is reversed for Pi. A question the
  model still writes as plain text is read as not waiting.

## To be checked

- Measured on 2026-11-12 with the pinned Pi 1.5.0 run outside a task at an 80 by 24 terminal, the ask extension
  loaded and its dialog opened by a command: the title `Pi asks you` stands alone on the ninth line from the bottom,
  under it the answer line and `enter submit  escape/ctrl+c cancel`, then the working-directory line and the footer
  with `(auto)`, which stays. The question itself is not drawn in the dialog (`input`'s second argument is a
  placeholder Pi does not show); it stands in the tool call above. Answered or cancelled, the dialog is gone. In
  the shipped bundle `ui.input` runs inside `withUIPrompt("input", …)`, which fires `ui_prompt_start`.
- On the VM `smith` (ubuntu 26.04, porelo main c43062468, the adapter installed in the account), 2026-11-12:
  `waiting.feature` and `wake.feature` passed three times, first as written, then after the review's changes,
  then with the final declaration. `z-ai/glm-5.3-flash` called `ask_person` in every one of the six turns rather
  than asking in text. Nothing was typed into the open dialog in 15 s of the daemon's passes, and the message was
  announced once Pi rested after the answer.
- In a task Pi draws rules around the dialog (captured at 100 by 39): the title is the sixth non-empty line from
  the bottom, not the fifth as outside a task. `in_last_lines` is 7, the rule above the title; `WaitingDeclarationTest`
  reads that captured screen.
- Changed from the plan after the review by a second model: `at_rest.lacks` names the dialog's keys line
  `escape/ctrl+c cancel` instead of the title. A wake reads the last 20 lines, so the title quoted in the
  conversation would have held every wake back; the keys line also covers Pi's other dialogs. The ask tool passes
  the turn's abort signal to the dialog. Left as is: the review's wish that the wake scenario also check the
  model's answer to the message, which tests the model, not the wake.
