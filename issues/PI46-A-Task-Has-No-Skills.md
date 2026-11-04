# PI46 — A Task Has No Skills

**Status:** Implementing.
**Size:** XS - one argument in the definition and its tests, no contract.

**What must be true.** Pi in a Porelo task lists no skill on any start, resumed or not.

## Plan

- `sandboxed.arguments` in `pi.yaml` gains `--no-skills`, behind `--no-approve`, which Porelo passes on every start.
- Red first: `PiAgentTest` finds the argument, and the headless and resumed commands name it.

## Acceptance

- Seen to fail first: the definition has no `--no-skills`, and the agent lists a skill in its directory.
- With the change, the same agent answers that none is listed.
