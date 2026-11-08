# PI39 — The Repository's Agent Works In A Porelo Task

**Status:** Review; blocked by [PI38](README.md#pi38), in Review.
**Size:** S - no code here beyond `porelo.yml`, the move itself done by the procedure; about 2 hours of the agent's
work, plus the operator's attended start of the task.

**What must be true.** `porelo-pi`'s agent works in a Porelo task of the project `porelo` on the move's host, reached
in the project's room under the repository's name, and works this issue from start to Review inside that task with
the local forge's build green.

One agent per repository, each move checked before the next, as the project's procedure for an agent's move lays out.

## Plan

Read in today's `porelo.yml`, `settings.xml` and agent definition, porelo's `doc/project-file.md` (*A repository's own
image*) and `doc/running.md` (the agent's layer), and porelo-project's `project.yml`.

- **The image:** today's `porelo.yml` holds the pinned GraalVM, Node, shfmt and the build tools, and `mvnw` brings
  Maven, so the unit suite and the native packages build in the task as porelo-codex's did in a real task. The agent's
  CLI comes from its package's own layer, not from `porelo.yml`. The acceptance legs do not run in a task (no container
  inside it, no rented machine); they run in the forge's Build ([PI38](README.md#pi38)).
- **What the task reaches while it runs:** Maven Central, Sonatype or the forge's registry, `fuinorg.jfrog.io` for
  the skills, `raw.githubusercontent.com` for `check-citations`, and the forge's address as a host service. The task runs `verify`
  only: Pi's tree (`buildtools/build-pi-tree.sh`) would add `registry.npmjs.org`, `nodejs.org` and GitHub's release
  downloads, which the project's egress does not name, so the forge's Build builds the packages. That egress is the project's, in
  porelo-project's `project.yml`; the image, the agent's CLI with it, is built on the host outside the task's firewall.
  The task's build reads Sonatype's snapshots: the forge's registry refuses a task today (the organization is private), and nothing sets
  `MAVEN_SNAPSHOTS_URL` in a task yet, so the `forge` profile stays off there until both are in.
- **The sub-agent reading:** no sub-agent CLI is in the task's image, since a project file names one
  agent per repository and porelo describes no second sign-in inside a task. Until Porelo has a task that reviews, an agent outside the task reads the pull request's diff against the skills, as today's sub-agent reading does, and posts its
  findings in the room.
- **The CLI:** Claude Code runs the agent in the task, as it runs this repository's agent today: woken by a
  message, its subscription brokered from the vault. Pi itself runs it in an issue of its own, once [PI33](README.md#pi33) is in,
  since Pi is never woken by a message before.
- **Steps:**
  0. *A dry run on this machine first*, only the part that needs nobody: the task's image built from
     `porelo.yml` with podman, and inside it, as a user other than root on an empty local Maven repository,
     `./mvnw -B -s settings.xml verify` and `./mvnw -B -s settings.xml -Pnative,dist verify` on a fresh clone.
     No task starts here and nothing goes through a gate: the room, the wake and the gate are shown on the
     rented host directly. Passed: the image built in 2 min; `verify` ran 197 tests,
     none failed or skipped, in 39 s; the native packages, `.deb` and `.rpm`, with Pi's tree built directly
     (`PI_TREE_DIRECT`, fetching Node, the npm packages, fd and rg), built and checked in 101 s.
  1. *Before, from this machine:* a dry run of the task start on the host names each image key's source as
     `porelo.yml`; the image builds; inside it, `./mvnw -s settings.xml verify` passes.
  2. *porelo-project (the coordinator):* `repositories.pi.agent` in `project.yml`, no nickname, since the key
     `pi` gives the room's name, and the upstream switched to the host's forge, as the project's procedure for an agent's move lays out.
  3. *The start:* the operator starts the task attended; inside it the agent takes this issue to Review, its push
     through the gate to `porelo/<task>`, the forge's Build read through the `forgejo` reader.
  4. *After the check:* the session on this machine gives up `porelo-pi`;
     its test accounts stay with the repositories not moved yet.
- **Red first:** the move's check from the project's procedure for an agent's move, for
  `porelo-pi` before the move, each line failing by name.

## Acceptance

- The task's image is built from `porelo-pi`'s `porelo.yml`, with no sub-agent CLI in it.
- The task follows `porelo-pi` through the gate; its pushes reach the local Forgejo, and it reads its builds through the
  `forgejo` reader.
- Its build runs on the local forge as on the central one, [PI38](README.md#pi38).
- The room shows it as `Agent Pi`, from the capitalized key `pi` in porelo-project's `project.yml`.
- The build in the task passes on Sonatype's snapshots; it takes them from the local forge's registry only once that registry lets a task read it and the task sets `MAVEN_SNAPSHOTS_URL`, which is not this move's.
- Claude Code works in the task with the operator's subscription from the vault.
- The move's check is red for `porelo-pi` before the move and green after it; the agent's session on this machine and its
  test accounts are no longer used, and the way back the project's procedure for an agent's move describes is open.

## Findings from the trial

- **What ran:** the seven skills fetched from the skills repository, each checked against its sha256 before it was
  unpacked; the tests tagged `documents` (5 tests) and `verify` (201 tests, none failed or skipped) passed in the task,
  each its own step with exit code 0, the packages left to the forge's Build.
- **Missing, egress:** `raw.githubusercontent.com` does not resolve from the task, so `check-citations` ran without
  reading other repositories' indexes (it says so, and exits 0) and that part is left to the forge's *Shared rules check*.
- **Missing, context:** the task has no remote `origin`, only the gate's, so `check-commits` reads `porelo/main` where the
  brief names `origin/main`; no `forgejo` reader was in the task, so Build results come by relay; the mailbox card names
  no room, so the room is answered as `people`.
- **Held:** the settings deny `Skill`, `Agent` and `Workflow`, so skills are read as files; the agent's name, the room in
  place of the channel and the operator's language and time zone came with the brief, not from a file in the task; the
  task's start found a copy of the repository on the host's forge and a harness skills folder that did not exist yet.

## To be checked

- That the forge's Build and *Shared rules check* pass on this issue's commits, once the forge reads the Porelo
  snapshots its registry does not hold.
- That the task reads its builds through the `forgejo` reader; in the trial none was in the task.
- That the project's egress is widened to `raw.githubusercontent.com` or `check-citations` is moved to where it
  resolves: in the trial the task could not read the other repositories' indexes.
- That the outside reader can fetch `porelo/<task>` from the host's forge, not only from GitHub.
- The way back, rehearsed once: the task's open work fetched from the host's forge into the checkout here.
- That a revoked sign-in is renewed with `porelo vault login claude --force` and a stop and start of the task: a
  running task keeps the credential it started with.
- That the outside reader may join the room from its harness; otherwise it posts its findings in the channel and the
  operator relays them.
