# Issues

What is open in this repository, grouped by state in the order Review, Rework, Implementing, Backlog,
Analyzed, Accepted, Idea, Estimated, Finished and Rejected; a group with nothing in it is left out.
Within a group, the order is the priority. The number is identity, not sequence.

## Review

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="pi32"></a>[PI32](PI32-Move-To-Pi-1.md) | M | — | the package ships Pi 1, without the advisories Pi 0.85.0's tree carries |
| <a id="pi27"></a>[PI27](PI27-Declare-The-App-Copilot-Is-Granted-Through.md) | S |  | the app `vault authorize github-copilot` signs in through for Pi |

## Implementing

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="pi31"></a>[PI31](PI31-Take-The-Legs-From-A-Run-That-Proved-The-Same-Tree.md) | S | — | a Build on `main` takes its legs from a run that proved the same tree within a day |
| <a id="pi34"></a>[PI34](PI34-Publish-Without-A-Downloaded-Client.md) | S | — | the Build publishes with curl and porelo's script, with no client downloaded beside the token |
| <a id="pi35"></a>[PI35](PI35-The-Tree-Builds-Without-A-Container.md) | S | — | Pi's tree builds inside a Porelo task, with no nested container, the same as in its pinned Node image |

## Idea

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="pi33"></a>[PI33](PI33-Tell-Waiting-From-Pi-s-Program-Status.md) |  | [PI32](#pi32) | Porelo tells a waiting Pi from one at rest by what Pi 1 reports of itself |

## Estimated

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="pi21"></a>[PI21](PI21-A-Login-Inside-A-Brokered-Task-Fails.md) | M |  | a person cannot put a provider key into a task |

## Finished

| # | Title | What was built |
|---|---|---|
| <a id="pi30"></a>[PI30](README.md#pi30) | Reformat To The Shared Style | The code is in the shared style, google-java-format in its AOSP variant with YAML and Gherkin at four spaces; `validate` refuses code that is not, and `git blame` with `.git-blame-ignore-revs` skips the reformatting. |
| <a id="pi28"></a>[PI28](README.md#pi28) | Read The Code Changed Since The Last Skills Review | The code changed since 2026-09-27 read against seven skills, each finding fixed, with a test where it could do harm: no terminal control character reaches a log, and no checkout keeps the job's token. |
| <a id="pi29"></a>[PI29](README.md#pi29) | Rename The Adapter Porelo | The repository, its artifact, its packages, its Java packages and its documents say Porelo; a documents test keeps the old name to the released part of `CHANGELOG.md`. |

## What lives here and what in Porelo

An agent's work lives in its own repository, so the requirements for Pi are issues here. The
reasoning that outlives them is in [`doc/decisions.md`](../doc/decisions.md).

The candidate agents without a repository wait in `porelo-project` until a repository builds them.

## Where the rest of the open work lives

Not everything open is an issue. Accepted risks - exposures that are known, deliberate and not
being removed - are in [`doc/decisions.md`](../doc/decisions.md) with what would change the
answer. A finding that has been answered is neither: it is in the code, with its reasoning.
