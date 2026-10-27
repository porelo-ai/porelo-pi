# PI37 — Nothing The Agent Writes Reaches The Next Pi

**Status:** Idea.

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
- **`PATH` and the environment are the agent's.** The image puts `/home/agent/.local/bin` first on `PATH`, and
  the agent owns it and its shell's start files. A `pi` of its own, or `PI_PACKAGE_DIR` pointing at a package
  whose `piConfig` renames the directory variable, comes before the wrapper's `PI_CODING_AGENT_DIR`.
- **Not this issue's:** a key a person types into `/login` is in the Pi process's memory before any store refuses
  it, and an extension cannot replace a built-in command. That is an accepted risk in `doc/decisions.md`.

## To be checked

- Whether Porelo places the two extension files as root, so `extensions/` can be root's and read-only.
  `porelo-route.ts` is written owner-only today, through `ContainerFile.secret` in `PiContainerSetup`;
  root's and read-only must mean root-owned and readable to the agent, not owned by it.
- Whether Pi starts, keeps its sessions and its settings with everything in its directory but `sessions/`
  root's and read-only.
- How Porelo starts the attended Pi: by `PATH` or by the definition's path, and through which shell.
