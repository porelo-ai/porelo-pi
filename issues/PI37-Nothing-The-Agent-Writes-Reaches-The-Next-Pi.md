# PI37 — Nothing The Agent Writes Reaches The Next Pi

```json
{
  "$schema": "issue-1.json",
  "id": "PI37",
  "title": "Nothing The Agent Writes Reaches The Next Pi",
  "state": "Analyzed",
  "size": "M",
  "priority": "Low",
  "rank": 6,
  "blockedBy": [],
  "covers": "nothing the agent writes in a task reaches a later Pi: its settings, models, MCP servers, extensions, `PATH`",
  "openQuestions": 1
}
```

**Why this size.** One module, but several files, a document and a measurement in a task: `pi.yaml` (the image's
directory and the start arguments), Porelo's three extensions moved from files placed in the task to files in the
image, `PiContainerSetup` and `PiRoutingExtension` reshaped so only data is placed, their tests, `doc/decisions.md`
and the changelog, and one start of Pi on the local VM with its directory read-only. It touches a trust boundary,
so its plan is read by another model before it is built. It is L if the `PATH` part stays here (see *Open
questions*): that part is Porelo's, not the adapter's.

**Estimate.** M: 53 min, the measured median of M over all repositories (n=113).

**What must be true.** Nothing the agent writes in a task reaches a later Pi in it: not Pi's settings, models,
MCP servers or extensions, and not a `pi` or a variable that comes before the root-owned wrapper. So code the
agent prepared cannot run inside the Pi a person later signs in with.

## Why

Found by the skill reading of the move of Pi's directory out of the home, each read in Pi 1.3.0's code:

- **The rest of Pi's directory is the agent's.** `/var/lib/pi/agent` is `1777`, and only `auth.json` and
  `mcp-auth.json` are root's. The agent can write `settings.json`, `models.json`, `mcp.json` and
  `extensions/*.ts`. Pi loads them at its next start as its user scope, which `--no-approve` does not refuse;
  only a workspace's `.pi/` is refused. An extension the agent wrote runs inside the next Pi and can copy a
  later `/login` out of it.
- **`PATH` and the environment are the agent's.** The image puts `.local/bin` in the agent's home first on `PATH`, and
  the agent owns it and its shell's start files. A `pi` of its own, or `PI_PACKAGE_DIR` pointing at a package
  whose `piConfig` renames the directory variable, comes before the wrapper's `PI_CODING_AGENT_DIR`.
- **Not this issue's:** a key a person types into `/login` is in the Pi process's memory before any store refuses
  it, and an extension cannot replace a built-in command. That is an accepted risk in `doc/decisions.md`.

**Read in today's code and in the pinned Pi 1.5.0** (the tree `build-pi-tree.sh` builds):

- **Porelo places an agent's files as the agent.** `TaskRunner.place` runs `mkdir -p`, `tee` and `chmod` through
  `podman exec` without `--user`, and the image ends with `USER agent` (porelo's `Containerfile`). So Porelo's own
  `porelo-route.ts`, `porelo-status.ts` and `porelo-ask.ts` are the agent's files: it can rewrite them, and the
  next Pi runs what it wrote. A root-owned `extensions/` alone would make `mkdir -p` and `tee` fail.
  `ContainerFile` has one switch, `ownerOnly` (`600` or `644`), and no owner.
- **Pi can be told to load only named extensions.** `--no-extensions` drops every discovered extension, Pi's
  directory and the workspace alike, and keeps those named with `-e` (`core/resource-loader.js`: `noExtensions ?
  cliEnabledExtensions : merge(...)`). `--no-mcp` turns off the built-in MCP extension, which reads `mcp.json`.
- **Pi's sessions can live apart from its directory.** `PI_CODING_AGENT_SESSION_DIR` moves them
  (`config.js`, `ENV_SESSION_DIR`); `models.json`, `settings.json`, `auth.json`, `tools/` and `bin/` stay under
  `PI_CODING_AGENT_DIR`.
- **The binary is found by name.** `pi.yaml` says `binary: pi`, and the image's `ENV PATH` puts `.local/bin` in
  the agent's home first, so a `pi` the agent drops there comes before `/usr/local/bin/pi`.

## Plan

- **Porelo's extensions come from the image, not the task.** The status and ask extensions carry no secret and do
  not change per task: the package ships them, and `as_root` copies them root-owned and `0644` to
  `/opt/porelo-pi/extensions/`. The routing extension becomes fixed code beside them that reads its provider,
  endpoint and token from a data file Porelo places owner-only, so only data is the task's.
  `PiStatusExtension`, `PiAskExtension` and `PiRoutingExtension` write the files at package time instead of
  answering `ContainerSetup.files`; `PiContainerSetup` places the routing data alone.
- **Pi loads only those.** `sandboxed.arguments` gain `--no-extensions`, `-e` for each of the three and `--no-mcp`,
  beside `--no-approve` and `--no-skills`.
- **Pi's directory is root's.** `as_root` makes `/var/lib/pi/agent` root-owned `0755` with empty, root-owned
  `settings.json`, `models.json` and `mcp.json`, and `sessions/` a sticky `1777` directory inside it, so
  `session_id.directory` stays where Porelo reads it.
- **The wrapper sets what it relies on.** `/usr/local/bin/pi` unsets `PI_PACKAGE_DIR` and every other variable
  that moves Pi's package or directory before it sets `PI_CODING_AGENT_DIR`.
- Changed with it: `PiContainerSetupTest`, `PiAgentTest` (the arguments), `BuiltTreeCheck` (the shipped
  extensions), `doc/decisions.md` ("Pi's directory is outside the home, its sign-in stores root's" and "The
  endpoint is set by a file, not a variable"), and `CHANGELOG.md` under `[Unreleased]`.
- **Red first, unit level:** a `PiAgentTest` case that the sandboxed arguments carry `--no-extensions` and `-e`
  only for paths under `/opt/porelo-pi/extensions/`, and a `PiContainerSetupTest` case that no placed file is
  under `extensions/` or ends in `.ts`; both fail on today's code. The task-level proof is the VM start under
  *Acceptance*.

## Acceptance

- In a task on the local VM, as the agent: writing `settings.json`, `models.json`, `mcp.json` or a file under
  `/var/lib/pi/agent/extensions/` fails, and an extension the agent writes anywhere is not loaded by the next
  attended Pi.
- Rewriting the routing data changes nothing Pi runs as code, and the shipped extensions are root-owned and
  unchanged.
- The attended and unattended Pi start, route through the broker, show the ask dialog, and continue a session,
  with the existing acceptance features green on the legs.

## What makes it uncertain

- Pi may write to its directory at start or on a model change (`settings.json`, `models-store.json`, `bin/`). If
  it stops on a read-only file instead of carrying on, those files need a place of their own, read from the
  measurement before the build.
- The routing data stays the agent's file: an endpoint it rewrites could point Pi's requests, and a key a person
  gives `/login` with them, at a listener of its own. Closing that needs the data placed as root and readable to
  the agent, a placement `ContainerFile` does not offer: a change to Porelo's agent API, which would make this L.

## Open questions

- **Does the `PATH` part stay in this issue?** The `pi` a person starts is found through the agent's own `PATH`,
  and a shell in the task reads the agent's own start files. No adapter can close that, and the same holds for
  every agent's binary. Recommended: it goes to Porelo as a requirement of its own, and this issue closes Pi's
  directory alone (M). Kept here, it waits on Porelo and is L.

## To be checked

- How Porelo starts the attended Pi: by `PATH` or by the definition's path, and through which shell.
- Whether Pi starts, keeps its sessions and changes its model with its directory read-only and only `sessions/`
  writable, measured on the local VM.
- Whether Porelo can place a file as root, readable to the agent, through the agent API today (its own files under
  `/porelo/files` are placed as root): if so, the routing data takes that and the second point under *What makes
  it uncertain* falls away.
- Whether Porelo gives Pi any MCP server through `mcp.json`; `--no-mcp` would drop it.
