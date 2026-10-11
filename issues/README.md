# Issues

What is open in this repository, grouped by state in the order Review, Rework, Implementing, Backlog,
Analyzed, Accepted, Idea, Estimated, Finished and Rejected; a group with nothing in it is left out.
Within a group, the order is the priority. The number is identity, not sequence.

## Analyzed

| # | Status | Size | Blocked by | What it covers | Open questions |
|---|---|---|---|---|---|
| [PI21](PI21-A-Login-Inside-A-Brokered-Task-Fails.md) | Analyzed | M |  | a person cannot put a provider key into a task | 2 |

## What lives here and what in Sokar

An agent's work lives in its own repository, so the requirements for Pi are issues here. The
reasoning that outlives them is in [`doc/decisions.md`](../doc/decisions.md).

The candidate agents without a repository wait in `sokar-project` until a repository builds them.

## Where the rest of the open work lives

Not everything open is an issue. Accepted risks - exposures that are known, deliberate and not
being removed - are in [`doc/decisions.md`](../doc/decisions.md) with what would change the
answer. A finding that has been answered is neither: it is in the code, with its reasoning.
