# PI39 — The Repository's Agent Works In A Porelo Task

**Status:** Analyzed; blocked by [PI38](README.md#pi38), porelo-project
[PJ59](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj59) (the move's procedure and check)
and porelo [B298](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b298).
**Size:** S - no code here beyond `porelo.yml`, the move itself done by the procedure; about 2 hours of the agent's
work, plus the operator's attended start of the task.

**What must be true.** `porelo-pi`'s agent works in a Porelo task of the project `porelo` on the move's host, reached
in the project's room under the repository's name, and works this issue from start to Review inside that task with
the local forge's build green.

From porelo-project [PJ59](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj59), the move's wave 7a: one agent per repository, each move checked before the next.

## Plan

Read in today's `porelo.yml`, `settings.xml` and agent definition, porelo's `doc/project-file.md` (*A repository's own
image*) and `doc/running.md` (the agent's layer), and porelo-project's `project.yml`.

- **The image:** today's `porelo.yml` holds the pinned GraalVM, Node, shfmt and the build tools, and `mvnw` brings
  Maven, so the unit suite and the native packages build in the task as porelo-codex's did in a real task
  ([CX02](https://github.com/porelo-ai/porelo-codex/blob/main/issues/README.md#cx02)). The agent's
  CLI comes from its package's own layer, not from `porelo.yml`. The acceptance legs do not run in a task (no container
  inside it, no rented machine); they run in the forge's Build ([PI38](README.md#pi38)).
- **What the task reaches while it runs:** Maven Central, Sonatype or the forge's registry, `fuinorg.jfrog.io` for
  the skills, `github.com` for `check-citations`, and the forge's address as a host service. Pi's tree
  (`buildtools/build-pi-tree.sh`, `PI_TREE_DIRECT` in a task) adds `registry.npmjs.org` for `npm ci`, `nodejs.org` for
  the runtime, and `github.com`'s releases of fd and rg with the storage hosts they redirect to
  (`objects.githubusercontent.com`, `release-assets.githubusercontent.com`). That egress is the project's, in
  porelo-project's `project.yml`; the image, the agent's CLI with it, is built on the host outside the task's firewall.
  The task's build reads Sonatype's snapshots: the forge's registry refuses a task today (the organization is private,
  porelo-project [PJ75](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj75)), and nothing sets
  `MAVEN_SNAPSHOTS_URL` in a task yet, so the `forge` profile stays off there until both are in.
- **The sub-agent reading:** no sub-agent CLI is in the task's image, since a project file names one
  agent per repository and porelo describes no second sign-in inside a task. Until porelo
  [B172](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b172)'s reviewer task is built, an agent
  outside the task reads the pull request's diff against the skills, as today's sub-agent reading does, and posts its
  findings in the room.
- **The CLI:** Claude Code runs the agent in the task, as it runs this repository's agent today: woken by a
  message, its subscription brokered from the vault. Pi itself runs it in an issue of its own, once [PI33](README.md#pi33) is in,
  since Pi is never woken by a message before.
- **Steps:**
  0. *A dry run on this machine first*, as for porelo-message-sluice: the task online with GitHub as upstream, its
     Build read through the `github` reader, snapshots from Sonatype; it shows the image, the room, the wake and the
     gate, and only the `forgejo` lines wait for the host.
  1. *Before, from this machine:* a dry run of the task start on the host names each image key's source as
     `porelo.yml`; the image builds; inside it, `./mvnw -s settings.xml verify` and the native packages pass.
  2. *porelo-project (the coordinator):* `repositories.pi.agent` and the nickname `Pi`
     ([B298](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b298)), and the upstream switched to the
     host's forge, as [PJ59](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj59)'s procedure says.
  3. *The start:* the operator starts the task attended; inside it the agent takes this issue to Review, its push
     through the gate to `porelo/<task>`, the forge's Build read through the `forgejo` reader.
  4. *After the check:* the session on this machine gives up `porelo-pi` (the responsibility line of the shared area
     of `.AGENTS.md` changes through the channel); its VM accounts stay with the repositories not moved yet.
- **Red first:** [PJ59](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj59)'s move check for
  `porelo-pi` before the move, each line failing by name.

## Acceptance

- The task's image is built from `porelo-pi`'s `porelo.yml`, with no sub-agent CLI in it.
- The task follows `porelo-pi` through the gate; its pushes reach the local Forgejo, and it reads its builds through the
  `forgejo` reader.
- Its build runs on the local forge as on the central one, [PI38](README.md#pi38).
- The room shows it as `Agent Pi`, from the nickname `Pi` in porelo-project's `project.yml`, set once porelo
  [B298](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b298) is published.
- The build in the task passes on Sonatype's snapshots; it takes them from the local forge's registry only once
  porelo-project [PJ75](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj75) is in and the
  task sets `MAVEN_SNAPSHOTS_URL`, which is not this move's.
- Claude Code works in the task with the operator's subscription from the vault.
- [PJ59](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj59)'s move check is red for `porelo-pi` before the move and green after it; the agent's session on this machine and its
  VM accounts are no longer used, and the way back [PJ59](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj59) describes is open.

## To be checked

- That porelo-project's `project.yml` lets the task reach the hosts named under *Plan*, Pi's tree's download hosts
  among them.
- Where the task's agent finds what `.AGENTS.md` gives it here (its name, the room in place of the channel, the
  operator's language and time zone): `.AGENTS.md` is not committed, so it is not in the task.
- That the outside reader can fetch `porelo/<task>` from the host's forge, not only from GitHub.
- The way back, rehearsed once: the task's open work fetched from the host's forge into the checkout here.
