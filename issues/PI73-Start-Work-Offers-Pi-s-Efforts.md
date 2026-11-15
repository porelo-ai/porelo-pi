# PI73 — Start Work Offers Pi's Efforts

```json
{
  "$schema": "issue-1.json",
  "id": "PI73",
  "title": "Start Work Offers Pi's Efforts",
  "state": "Review",
  "size": "XS",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "the definition declares the reasoning efforts Pi takes and how one is passed (`--thinking {effort}`), so Start work offers them to pick instead of typing and an absent effort passes nothing"
}
```

**Why this size.** One section of the agent definition and a test that reads it; no code.

**Estimate.** XS: 9 min, the measured median of XS over all repositories.

**What must be true.** A person starting work with Pi picks the effort from the levels the pinned CLI accepts,
and no effort picked passes nothing.

## Why

Part of ST369, the Start work redesign. The operator wants to pick the model and the reasoning effort from what the
chosen agent offers, not type them. Porelo's contract (B795, handed over locally as porelo 5328c5df4) adds an optional
top-level `effort` section to an agent's definition: `effort: { arguments: [..., "{effort}"], values: [...] }`, the
values in the agent's own words and order. Today's definition reader skips an unknown top-level section, so the
declaration does not break an older Porelo.

## Plan

- Declare `effort` in the definition: `--thinking {effort}`, with the levels the pinned Pi 1.5.0 takes, read from its own help on
  the pin where a VM slot allows, else from the laptop's version and said so.
- A test that reads the section from the definition itself (the API record does not hold it yet), seen red first.
- **Level:** unit tests of the definition.

## Acceptance

- The definition declares the effort levels and how they are passed, in the contract's shape, and today's reader
  still reads the definition.

## What was built

- **The definition** (`pi.yaml`) declares `effort` with `arguments: ["--thinking", "{effort}"]` and `values: ["off", minimal, low, medium, high, xhigh, max]`, in the
  contract's shape; an older Porelo's reader skips the section.
- **Where the levels come from:** `pi --help` on 1.4.0, read offline on the test VM; the pinned 1.5.0 was not there, so the levels are unconfirmed on the pin. "off" is quoted, since YAML reads a bare off as false; the test saw `false` until it was.
- **Test** `EffortDeclarationTest` reads the section from the definition itself: the arguments and the levels, in
  their order. Both seen red before the section stood; dropping the last level turned `offersTheLevelsTheCliTakes`
  red, restored identical.
- **The reading by another model** found no fault: the shape, the reader of porelo 5328c5df4, the substitution and
  the placement hold. It confirmed the quoted `"off"` is needed: the reader takes each value as a string of what YAML read. Left: the test reads the YAML with a plain loader, so a duplicated `effort` key would pass it and fail the reader.
- **Level:** unit tests; suite 253 run, 0 failures, 0 skipped. Whether the pinned CLI accepts each level in a
  run is the operator's test.
