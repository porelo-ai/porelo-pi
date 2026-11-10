# PI46 — A Task Has No Skills

```json
{
  "$schema": "issue-1.json",
  "id": "PI46",
  "title": "A Task Has No Skills",
  "state": "Review",
  "size": "XS",
  "priority": "Medium",
  "rank": 5,
  "blockedBy": [],
  "covers": "a task has no skills, on any start"
}
```

**Why this size.** one argument in the definition and its tests, no contract.

**What must be true.** Pi in a Porelo task lists no skill on any start, resumed or not.

## Plan

- `sandboxed.arguments` in `pi.yaml` gains `--no-skills`, behind `--no-approve`, which Porelo passes on every start.
- Red first: `PiAgentTest` finds the argument, and the headless and resumed commands name it.

## Acceptance

- Seen to fail first: the definition has no `--no-skills`, and the agent lists a skill in its directory.
- With the change, the same agent answers that none is listed.

## Measured

On the VM, Pi 1.4.0 from the packaged tree alone, not through Porelo, through OpenRouter with `z-ai/glm-5.3-flash`, one
run each, with a skill in Pi's directory, one in `.agents/skills` and one in `.pi/skills`: with `--no-approve` the agent
listed the one in its directory; with `--no-approve --no-skills` it answered that none is listed. No scenario covers it,
so a pin that renames the flag is not seen by the suite, unless Pi refuses the flag.

## A soft bound

The agent can still read a skill's file as any file, and start another Pi from its shell; no flag closes that. A hard
bound is Porelo's own, outside this repository.

In the same runs, without these flags, the agent had `read`, `bash`, `edit` and `write`, found no way to start a
subagent, and a definition it wrote under `.pi/agents` ran nothing. Read only, not measured: `pi --help` names no
workflows, and Pi's docs describe extensions and a `codemode` tool, which was not among the tools.
