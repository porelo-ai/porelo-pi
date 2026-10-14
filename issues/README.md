# Issues

What is open in this repository, grouped by state in the order Review, Rework, Implementing, Backlog,
Analyzed, Accepted, Idea, Estimated, Finished and Rejected; a group with nothing in it is left out.
Within a group, the order is the priority. The number is identity, not sequence.

## Analyzed

| # | Size | Blocked by | What it covers | Open questions |
|---|---|---|---|---|
| <a id="pi28"></a>[PI28](PI28-Read-The-Code-Changed-Since-The-Last-Skills-Review.md) | M | — | the code changed since 2026-09-27 read against the seven skills | 0 |
| <a id="pi29"></a>[PI29](PI29-Rename-The-Adapter-Porelo.md) | S | sokar-project [PJ41](https://github.com/sokar-ai/sokar-project/blob/main/issues/README.md#pj41) | the adapter renamed Porelo, by script, with its exceptions | 0 |

## Estimated

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="pi21"></a>[PI21](PI21-A-Login-Inside-A-Brokered-Task-Fails.md) | M |  | a person cannot put a provider key into a task |
| <a id="pi27"></a>[PI27](PI27-Declare-The-App-Copilot-Is-Granted-Through.md) | S |  | the app `vault authorize github-copilot` signs in through for Pi |

## What lives here and what in Sokar

An agent's work lives in its own repository, so the requirements for Pi are issues here. The
reasoning that outlives them is in [`doc/decisions.md`](../doc/decisions.md).

The candidate agents without a repository wait in `sokar-project` until a repository builds them.

## Where the rest of the open work lives

Not everything open is an issue. Accepted risks - exposures that are known, deliberate and not
being removed - are in [`doc/decisions.md`](../doc/decisions.md) with what would change the
answer. A finding that has been answered is neither: it is in the code, with its reasoning.
