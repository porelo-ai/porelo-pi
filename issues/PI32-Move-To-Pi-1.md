# PI32 — Move To Pi 1

**Status:** Review.
**Size:** M - the pin and its lockfile, the definition's measured claims checked again on 1.3.0, both
extensions run against Pi 1's API, and the VM suite; a major version the weekly update stops on by design.

**What must be true.** The package ships Pi 1.3.0 or newer, with no advisory OSV lists against the shipped tree
that Pi 0.85.0's tree had, and the VM suite is green on both legs with it.

## Why

pi pins Pi **0.85.0**, the newest is **1.3.0** (2026-10-15). The weekly update stops on a major version for
a person to decide (`update.yml`, "What upstream said that a person decides"), so the pin stays where it is
by itself.

**What 0.85.0's shipped tree carries,** looked up in OSV on 2026-10-18, every package of
`src/main/npm/package-lock.json` at its version: 13 advisories, all inside Pi's own dependencies:
- **`undici` 8.9.0, ten advisories, fixed in 8.10.2.** The high ones are GHSA-w293-vg96-wgc3, TLS
  certificate validation bypassed through dropped connect options in BalancedPool, and GHSA-rfgv-xxqx-mfg5,
  a DoS through an unrequested WebSocket subprotocol.
- **`brace-expansion` 5.0.9, three advisories, fixed in 5.0.12:** DoS by stack exhaustion or quadratic time.

**Every Pi since 1.0.4** (2026-10-05) depends on `undici` 8.10.2, and 1.3.0 on `brace-expansion` 5.0.12
directly. The operator chose this move over `overrides` in `package.json`, which would ship a tree Pi's
release never tested.

## What Porelo relies on, read in 1.3.0's package

Read in the published packages of `@earendil-works/pi-coding-agent` and `@earendil-works/pi-ai` at 0.85.0 and
1.3.0, not run:
- **The same:**
  - the entry point `dist/bundle/cli.js`, which the image's `pi` wrapper runs;
  - Node `>=22.19.0`;
  - the flags `--no-approve`, `--append-system-prompt`, `--print`, `--mode`, `--session` and `--model`;
  - the ready marker "Pi can explain its own features" and the footer's "(auto)";
  - `PI_SKIP_VERSION_CHECK` and `PI_TELEMETRY`;
  - the Copilot client id, still base64 `SXYxLmI1MDdhMDhjODdlY2ZlOTg=` in `dist/auth/oauth/github-copilot.js`,
    which `BuiltTreeCheck` reads;
  - the extension events the status extension listens to (`session_start`, `agent_start`, `agent_settled`,
    `ui_prompt_start`, `ui_prompt_end`, `session_shutdown`) and `pi.registerProvider` with `baseUrl` and
    `apiKey`, which the routing extension calls.
- **Changed, to check:**
  - **Breaking changes in the extension API:** `before_agent_start`, `TurnEndEvent`, `agent_settled`'s
    handlers deferring runs, and pi-ai's provider stream input. Porelo's extensions use none of these by name.
    Whether they still load and route is for the VM to show.
  - **New:**
    - built-in MCP, read from `~/.pi/agent/mcp.json` and from `.pi/mcp.json` after project trust, which
      `--no-approve` withholds;
    - codemode on `quickjs-wasi`;
    - `@earendil-works/pi-mcp` and `@earendil-works/pi-codemode` among the dependencies, while
      `@earendil-works/chord` is gone. The bill changes, and `compare-bills` says by how much.
  - **New, maybe of use:** OSC 7501 program status (`PI_PROGRAM_STATUS`): Pi reports whether it is working,
    blocked on a dialog, done or failed. That could give Pi the `waiting` declaration 0.85.0 could not have.
    That is [PI33](README.md#pi33), not this issue.
  - **The screen:** the "Working ─" line `at_rest` lacks, and the session file layout (`session_id`), are
    measured claims; both are measured again at a terminal.
  - **No `npm-shrinkwrap.json`** in 1.3.0's package (0.85.0 had one). The lockfile here pins by integrity
    either way.

## Plan

- **The pin:** `org.fuin.porelo.release.Main update 1.3.0` (or the newest 1.x on the day), which rewrites
  `agent.cli.version` and the lockfile. Then the bill compared with `compare-bills`, its new components named
  in the changelog.
- **The definition:** each comment in `pi.yaml` that says "measured on 0.85.0" is measured on the new
  version, and says so: the flags, `ready_marker`, `at_rest`, `session_id` and `refused_domains`. A claim that
  no longer holds is fixed with its test seen red first.
- **The extensions:** `PiRoutingExtension` and `PiStatusExtension` are run against Pi 1 on the VM: a brokered
  answer, and the status file going working, then idle.
- **[PI27](README.md#pi27)'s check:** `BuiltTreeCheck` passes on the new tree unchanged, since the id is the same. Seen red
  with the id changed, as before.
- **OSV again** on the new lockfile, its result in this issue.
- **The VM suite**, whole, on `ubuntu26.04`, then the Build's two legs.

## Acceptance

- `agent.cli.version` is 1.3.0 or newer, and the package's `pi --version` says so on the VM.
- The VM suite and both legs are green on it.
- OSV lists none of the 13 advisories above against the new shipped tree.
- Every "measured on" in `pi.yaml` names the version it was measured on.

## Measured

- **OSV, 2026-10-18,** on the new `src/main/npm/package-lock.json` (118 packages, from 167): no advisory, so
  none of the 13 above.
- **The tree's bill** needed porelo-buildtools [BT19](https://github.com/porelo-ai/porelo-buildtools/blob/main/issues/README.md#bt19):
  Pi 1.3.0 brings `@types/node` through `protobufjs`, and `add-component` had taken it for the Node runtime the
  script adds. With that fix's snapshot both stand in the bill, as two components.
- **On `ubuntu26.04`, account `smith`,** porelo `0.5.1~snapshot.307`, the adapter and the tree of `3bc78b2`
  in `smith`'s agents directory: **23 of 24 scenarios green.** Among them:
  - the brokered answers, so the routing extension still routes;
  - "reaches work without being asked anything", so the ready marker still shows;
  - both session scenarios, so `session_id` still finds the session;
  - Pi shown as one Porelo cannot say is waiting, and a message reaching it at rest with nothing typed.
- **The one red is the machine's, not the tree's:** "the adapter ships the Pi its bill names" reads the
  machine-wide package's bill, `/usr/share/porelo/sbom/`, which on that VM is still the 0.85.0 package, while
  the agent it checks is the account's own and said 1.3.0. The legs install the package itself, so they
  answer it: **Build 39152826235 on `bf6d763` is green on both legs**, that scenario included.
- **Not measured again on 1.3.0:** the screen claims behind `at_rest` and the missing `waiting` (Pi is never
  woken, so no scenario types into it). `pi.yaml` says which version each claim was measured on.
