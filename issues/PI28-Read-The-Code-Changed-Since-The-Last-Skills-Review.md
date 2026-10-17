# PI28 — Read The Code Changed Since The Last Skills Review

**Status:** Backlog.
**Size:** M - 34 files read against seven skills, each finding fixed with its own test; no
contract another repository reads.

**What must be true.** The code changed since this repository was last read against its skills, on
2026-09-27, has been read against them as well, and the versions read are on record.

## Why

The reading of 2026-09-27 went through the whole repository against the seven skills `AGENTS.md` names
and was closed in `fd8a2ec` (on `pre-public`, since the history was squashed into `main`). Everything
written after that was never read against them, and that reading did not record the version and SHA-256
of the skills it read, although its acceptance asked for both. The shared rule now says a repository's
skills are applied, not only named.

## Plan

- **Fetch the seven skills** - `graal`, `java-code-review`, `test-quality`, `security-audit`,
  `concurrency-review`, `clean-code`, `solid-principles` - in their latest version from
  `skills.json`, as *Skills* in `AGENTS.md` says, each checked against the SHA-256 its storage
  reports, and load them in the harness (asked of the harness, not read from a directory).
- **Fetched on 2026-10-17**, each SHA-256 matching what `agent-skills` reports:
  - `graal` 2026.910.3, SHA-256 `b24ec6272fb13490e32977c21d4543869a7e85ed474fdc0ff34022fbddd88bf6`
  - `java-code-review` 2026.906.3, SHA-256 `802f86339442ed9fa9d5bca14b6d16e1f6420ae92a047b18bba11239e32c698b`
  - `test-quality` 2026.906.3, SHA-256 `9c5d23ad7a6d9c6f5c3679f3cc44a7fcc2fb3cc1c0f0d0ba4a2c0f7812428bad`
  - `security-audit` 2026.906.3, SHA-256 `a46fd1be07bd539b955291537dea0f24f65a2f8b15d40d50a6a9098bbd992747`
  - `concurrency-review` 2026.906.3, SHA-256 `3a0aab612f25483a3fe777eb18fbcc434e936b19298708bf88beff8b22d79e56`
  - `clean-code` 2026.906.3, SHA-256 `5cf777a24630298b4b801c934f599722978c21aeac53d61683243908fc041c7b`
  - `solid-principles` 2026.906.3, SHA-256 `bd47cdab9126dde76cf1703472167a1c88c85306a4e0263a8f2db4c462740cf5`
- **`shellcheck` 0.11.0**, the host's, on `buildtools/build-pi-tree.sh` and `buildtools/follow-upstream.sh`:
  each finding fixed in the cleanup commit, with a test seen red first where one can be, or kept with
  `# shellcheck disable=<code>` and its reason on the line above. Run on 2026-10-17 on `a741713`: no finding.
- **No interface another repository reads changes:** the manifest's fields, the packages' names and paths,
  the service file, the guide written into a task. A finding that would change one is named in the channel,
  not built.
- **`persist-credentials: false` on every `actions/checkout`**, as decided for every repository on
  2026-10-17. Its red check is the shared `check-actions` from Agent Core's tools snapshot, run on today's
  workflows. The checkouts in `build.yml` and `shared-rules.yml` push nothing.
- **`update.yml` pushes its branch with git** (`git push --force origin "$BRANCH"`, :198), today through
  the write token the checkout leaves in `.git/config` for the whole job, the build and both acceptance legs
  included. The operator allowed the change on 2026-10-17: its checkout gets `persist-credentials: false`
  as well, and only the push step gives the token, never on a command line. In that one step the shell
  builds `x-access-token:<token>` in base64 with `printf` (a builtin) into `base64` on stdin, masks it with
  `::add-mask::`, and exports it as `GIT_CONFIG_COUNT=1`, `GIT_CONFIG_KEY_0=http.https://github.com/.extraheader`
  and `GIT_CONFIG_VALUE_0=AUTHORIZATION: basic <value>` for the `git push` after it; nothing goes to
  `$GITHUB_ENV`, so no later step holds it. `gh pr create` keeps `GH_TOKEN`. **Still to be seen:** only
  the next real update run pushing its branch proves it.
- **Scope:** `git diff --name-only fd8a2ec main -- src pom.xml buildtools`, taken again on the day the
  reading starts. Today that is 34 files (34 files changed, 2284 insertions(+), 561 deletions(-)):
  - production and build: `buildtools/build-pi-tree.sh`, `buildtools/follow-upstream.sh`, `pom.xml`, `PiAgent.java`, `PiJsonFormatter.java`, `PiStatusExtension.java`, `src/main/resources/agent/pi.yaml`;
  - 21 unit test files and 4 acceptance files under `src/test` and
    `src/acceptance`.

  Files unchanged since `fd8a2ec` were read on 2026-09-27 and stay out.
- **Read each file against each skill.** A finding is a guess until a test reproduces it: it is fixed
  with a test seen to fail on the unfixed code, or declined with the reason written here, or opened
  as its own issue when it is larger than this one or belongs to another repository.
- **A fix to what ships** gets its changelog entry in the same commit; the native build and the
  acceptance suite run as for any change to the adapter.
- **Red first:** each fix's test, run against the code before the fix.

## Acceptance

- The seven skills listed here with the version read and its verified SHA-256.
- Every file in scope read; each finding fixed with a test seen red first, declined here with its
  reason, or opened as an issue named here.
- Unit tests, the native build and the acceptance suite green on the commit handed over.

## To be checked

- Whether the workflows under `.github/workflows` changed since `fd8a2ec` are read as well, against
  `security-audit`: the reading of 2026-09-27 left them out, and they hold the release and update jobs.

## Findings, read on 2026-10-17

Read on `e1b6b9f` with the skills above, `SKILL.md` read from outside the repository. **The scope** is 31 files whose
content changed beyond the rename to Porelo since `fd8a2ec`: both `buildtools/*.sh`, `pom.xml`, `pi.yaml`, `PiAgent`,
`PiJsonFormatter`, `src/deb/copyright/copyright`, `src/deb/control/control`, 18 unit and check sources and the four
features `credential`, `session`, `waiting`, `wake`. The workflows were read against `security-audit` too.

Each finding is a guess until its test is seen red; none is fixed before [PI30](README.md#pi30).

**What ships:**
- **A1, `PiJsonFormatter`: terminal control characters pass through.** `Json.parse` turns `\u001b` escapes back into
  live bytes, so ESC, BEL or OSC sequences in the assistant's text, a provider's `errorMessage` or a tool's name
  reach the person's terminal; an OSC 52 writes the clipboard. Red: `"a\u001b[2Jb"` formats to text with no ISO
  control character, through `format` and through `ended().error()`.
- **A2, the same class: the cut at 300 characters can split a surrogate pair.** Red: `"a".repeat(299) + "😀"`.

**Tests that cannot fail, or prove less than they say:**
- **B1, `ReleaseChannelTest` (and `NoOldNameTest`):** output read to its end before `waitFor(30 s)`; a hanging step
  hangs the suite. Red: `sleep 120` fails within about 35 s.
- **B2, `PinAgreementTest.refusesABuilderImageNamedByTagAlone`** removes the digest from the pom but never runs the
  script's refusal. Red: the script with a tag-only `PI_BUILDER_IMAGE` and a `podman` stub exits 2, podman not called.
- **B3, `PinAgreementTest.scriptDefaults`** sees only `NAME="${NAME:-v}"`; unquoted and `:=` forms pass. Red: both
  forms as fixtures.
- **B4, `PinAgreementTest.installsRunningScripts`** matches only `npm ci`. Red: `npm install --omit=dev`.
- **B5, `WorkflowRulesTest`:** the package-build scans assert no build found; the empty-key guard matches text.
  Red: `-P native,dist … -DskipTests` green today; the steps run with an empty key.
- **B6, `WorkflowTimeLimitTest`:** lower-case job ids only, `jobs:` exact, no set of jobs asserted. Red: `Smoke:`.
- **B7, `PackageVersionCheck.aSnapshotSortsBelowItsReleaseAndAboveTheRunBefore`** checks its own helper. Fix:
  `dpkg --compare-versions` and `rpm` vercmp.
- **B8, `NoOldNameTest` runs git with the machine's global configuration.** Fix: `GIT_CONFIG_GLOBAL=/dev/null`.

**Workflows, against `security-audit`:**
- **W1, `update.yml`: a killed step opens a "Green" pull request.** Every gate reports by `|| echo STOP=...`; a step
  ended by its `timeout-minutes`, the job's limit or a cancel writes none. "Open the pull request" and "Merge, or say
  why not" run under `always()` and decide on STOP alone, so the body says **Green** and, with
  `PORELO_UPDATE_AUTO_MERGE=true`, an unverified pin is merged. Fix: both steps treat anything but the job's success
  as STOP. Red: the two steps' scripts run with STOP unset, a failed status given and a `gh` stub on `PATH`; no
  `gh pr merge`, no "Green".
- **W2, `build.yml`: "A published release is never replaced" fails open.** `curl -sfL … | grep -qF` sits in an `if`
  under `set -eux` without `pipefail`, so a failed fetch reads as "not published" and `jf rt upload` overwrites the
  release. Fix: fetch to a file, stop on curl's failure, then look. Red: the step's script with `CHANNEL=releases`, a
  `curl` stub exiting 22 and a `jf` stub recording its calls; non-zero exit, no upload.
- **W3, `build.yml`: "The package is indexed" greps the package name without its version**, so after the first
  publish it cannot fail, against the shared rule that the index shows the exact version. Red: a stub index holding
  only an older version makes the step exit 1.
- **W5, `update.yml`: `sweep --mine || true`** hides a failed cleanup while rented machines keep running, against the
  shared rule that a run exits non-zero naming what it left. Fix: drop `|| true`.
- **W6, `doc/decisions.md`, "Actions run from a commit":** "the only `curl` is `jf rt curl`" is no longer true
  (`build.yml` uses plain `curl` for the index). The text is corrected.
- **W7, `persist-credentials`** and the push through the environment, as planned above. In this job it weighs more:
  `BuiltTreeCheck` runs the freshly moved `fd` and `rg` during the dist build, with the write token on disk.

**Declined:** missing `.as(...)` reasons (a sweep of its own, not a defect); the formatter's two parse preambles
(taste); the mirror tests `doc/decisions.md` accepts; `build-pi-tree.sh` exiting 0 without podman (documented).
`follow-upstream.sh` against `security-audit`: inputs validated, values quoted, nothing reaches `GITHUB_OUTPUT`
unchecked. `graal` and `concurrency-review` found nothing.

**For Porelo, named in the channel, not built here:** `porelo task` prints every agent's formatted log to the terminal
as it comes, so A1 is fixed for Pi here, and in general only in Porelo.

**Size:** about 11 fixes and 16 tests; M holds.
