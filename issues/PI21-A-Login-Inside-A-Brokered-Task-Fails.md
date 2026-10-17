# PI21 — A login inside a brokered Pi task fails

**Status:** Estimated.
**Size:** M - a few root steps in the image, an acceptance scenario, and a measurement that Pi still
starts and keeps its sessions; no change to Porelo.

**What must be true.** A person in an attended, brokered Pi task cannot put a provider key into the
task with `/login`, so nothing in the task can use a provider past the broker.

## Why

Pi has no switch to turn `/login` off, and an entry in `~/.pi/agent/auth.json` wins over the provider
key the task is given. Egress blocks every subscription sign-in, because their auth hosts are not
declared. One path remains: a person, in an attended task, types `/login openrouter` and approves it in
a browser; Pi then stores a non-expiring OpenRouter key in the task, which the agent can read and use
past the broker. The model cannot do this alone.

## Plan

- **In the image, as root:** `pi.yaml`'s `install.as_root` gets steps that make `/home/agent/.pi/agent`
  root's with mode `1777`, and `auth.json` in it root's, read-only, holding `{}`. Porelo's image recipe
  (`Containerfile`) creates the `agent` user before these steps and switches to it after, and the home
  directory is part of the image, not a volume.
- **Why both:** a root-owned file alone does not hold, because the agent owns its directory and could
  remove or replace it. With the directory sticky and root's, the agent still creates its sessions and
  settings there, but cannot write, remove or rename over root's `auth.json`.
- **What the task still writes there:** `PiContainerSetup` places the routing extension and the
  settings; each is checked to land as before.
- **Red first:** an attended scenario in `src/acceptance` writes to `~/.pi/agent/auth.json` as the
  agent, the way `/login` stores a key, and expects it refused; on today's image it succeeds.
- **Then** the attended and unattended scenarios run unchanged, and `doc/decisions.md` says why the
  directory is root's.

## Acceptance

- `auth.json` cannot be written in a brokered task.
- An attended acceptance scenario shows `/login` fails and no `auth.json` appears; with the file
  writable, the same scenario is seen to fail.

## To be checked

- Whether Pi 0.85.0 starts, keeps its sessions and resumes with `~/.pi/agent` root's and sticky, and how
  it reports the refused write at `/login` - measured on the VM.
- Whether the scenario can show a refused `/login` without a browser approval, or writes the file the
  way Pi's store does.
