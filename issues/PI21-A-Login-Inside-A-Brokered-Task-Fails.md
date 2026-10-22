# PI21 — A login inside a brokered Pi task fails

**Status:** Implementing.
**Size:** L - Pi's directory moves out of the home: root steps in the image, the wrapper, both extension files,
`session.session_id.directory`, an acceptance scenario; and porelo [B247](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b247), a session directory outside the home.

**What must be true.** A `/login` in a brokered Pi task leaves no provider key in Pi's stores, and the agent
cannot write, remove or replace them.

**Not covered here, found by the skill reading:** a key a person types into `/login` is in the Pi process's
memory before the store refuses it, and Pi is the agent; the agent can write `settings.json`, `models.json`,
`mcp.json` and `extensions/` beside the stores, which the next Pi loads; and a `pi` or `PI_PACKAGE_DIR` of the
agent's own can come before the wrapper on `PATH`. The first is an accepted risk in `doc/decisions.md`; the
other two are [PI37](README.md#pi37).

## Why

Pi has no switch to turn `/login` off, and an entry in its `auth.json` wins over the provider key the
task is given. Egress blocks every subscription sign-in, because their auth hosts are not declared. One
path remains: a person, in an attended task, types `/login openrouter` and approves it in a browser; Pi
then stores a non-expiring OpenRouter key in the task, which the agent can read and use past the broker.
The model cannot do this alone.

**A root-owned file under the home does not hold.** Measured in the pinned Ubuntu 24.04 image as the
`agent` user, with `~/.pi` root's, `~/.pi/agent` root's and `1777`, and `auth.json` root's and `0444`:
writing, removing and renaming over `auth.json` are refused, and so is renaming `~/.pi/agent`. But
`mv ~/.pi ~/.pi-old` works, since the agent owns `/home/agent`, and a new `~/.pi/agent/auth.json` can
then be written where Pi reads it. So Pi's directory leaves the home.

## Plan

- **Pi 1.3.0 takes its directory from `PI_CODING_AGENT_DIR`**, and keeps two stores there: `auth.json`
  for providers and `mcp-auth.json` for MCP servers.
- **In the image, as root, in `pi.yaml`'s `install.as_root`:** `/var/lib/pi` root's and `0755`,
  `/var/lib/pi/agent` root's and `1777`, `auth.json` and `mcp-auth.json` in it root's, `0444`, holding
  `{}`. The wrapper `/usr/local/bin/pi`, root's, exports `PI_CODING_AGENT_DIR=/var/lib/pi/agent`. The
  agent still creates its sessions, settings and extensions there, and cannot write, remove or rename
  over root's files, nor move a directory it does not own out of one it does not own.
- **What moves with it:** `PiStatusExtension.FILE` and `PiRoutingExtension.FILE` to
  `/var/lib/pi/agent/extensions/`, and `session.session_id.directory` to
  `/var/lib/pi/agent/sessions/--workspace--`, which needs porelo [B247](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b247).
- **Red first:** an attended scenario in `src/acceptance` tries to chmod, write, replace and remove both
  stores as the agent, the way Pi's store writes, move Pi's directory and write in its parent. It expects
  every try refused, Pi to read `/var/lib/pi/agent` and both stores still to hold `{}`. It was seen to fail
  with Pi's directory under the home.
- **Then** the attended and unattended scenarios run unchanged, and `doc/decisions.md` says why Pi's
  directory is outside the home.

## Acceptance

- Pi's `auth.json` and `mcp-auth.json` cannot be written, removed or replaced in a brokered task, and no
  file the agent writes under its home is read by Pi as a sign-in.
- An attended scenario tries to chmod, write, replace and remove both stores, move Pi's directory and
  write in its parent; every try is refused, Pi reads `/var/lib/pi/agent` and both stores still hold `{}`.
  With the stores writable, the same scenario is seen to fail.
- The session scenarios, attended and unattended, are green with sessions kept in the new directory.

## Measured

- In the pinned Ubuntu 24.04 image as the `agent` user, Pi 1.3.0 with `PI_CODING_AGENT_DIR=/var/lib/pi/agent`, both
  stores root's and `0444`: `--version` answers, and a `--print` run starts its session, writes it to
  `/var/lib/pi/agent/sessions/--tmp--/`, says "No API key found for openrouter" and exits 1, leaving both stores `{}`
  and nothing under the home.
- Pi's store writes `auth.json` in place with `writeFileSync`, after a lock beside it, so a write as the agent is what
  `/login` would do.

- On the VM, porelo 307 per account in `smith`: the new scenario in `terminal.feature` failed against the adapter
  installed before ("pi reads /home/agent/.pi/agent", every write, replacement and removal of both stores done), and
  passed with `a187266`'s build. The whole suite with it: 24 of 25 green, credential and wake scenarios included. The
  one red is the attended resume, "has a session to continue", since Porelo looks for the session under the home
  until porelo [B247](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b247).

## To be checked

- How a refused `/login` reports itself, and that attended and resumed sessions hold, measured on the VM.
