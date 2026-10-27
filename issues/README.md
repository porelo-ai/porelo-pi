# Issues

What is open in this repository, grouped by state in the order Review, Rework, Implementing, Backlog,
Analyzed, Accepted, Idea, Estimated, Finished and Rejected; a group with nothing in it is left out.
Within a group, the order is the priority. The number is identity, not sequence.

## Review

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="pi21"></a>[PI21](PI21-A-Login-Inside-A-Brokered-Task-Fails.md) | L | — | a `/login` leaves no key in Pi's stores: Pi's directory outside the home |

## Analyzed

| # | Size | Blocked by | What it covers | Open questions |
|---|---|---|---|---|
| <a id="pi36"></a>[PI36](PI36-Import-And-Report-Pi-s-Sign-Ins-On-The-Host.md) | M | porelo [B224](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b224), [B225](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b225), [B226](https://github.com/porelo-ai/porelo/blob/main/issues/base/README.md#b226) | `vault import` takes Pi's sign-in for the provider asked for, and Pi's sign-ins on the host are reported by kind, provider and path, for [PJ53](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj53) | 0 |

## Idea

| # | Size | Blocked by | What it covers |
|---|---|---|---|
| <a id="pi37"></a>[PI37](PI37-Nothing-The-Agent-Writes-Reaches-The-Next-Pi.md) |  | [PI21](#pi21) | nothing the agent writes in a task reaches a later Pi: its settings, models, MCP servers, extensions, `PATH` |
| <a id="pi33"></a>[PI33](PI33-Tell-Waiting-From-Pi-s-Program-Status.md) |  | — | Porelo tells a waiting Pi from one at rest by what Pi 1 reports of itself |

## Finished

| # | Title | What was built |
|---|---|---|
| <a id="pi31"></a>[PI31](README.md#pi31) | Take The Legs From A Run That Proved The Same Tree | A Build on `main` asks `porelo-release proven-legs` whether a Build or an update run already proved the pushed tree on both legs within a day and takes those legs instead of renting again, while a tag always runs its own. |
| <a id="pi34"></a>[PI34](README.md#pi34) | Publish Without A Downloaded Client | Both Publish steps and the Artifactory smoke workflow talk to Artifactory through `.github/scripts/artifactory.sh` with `curl`, with no JFrog CLI or action left under `.github/`. |
| <a id="pi35"></a>[PI35](README.md#pi35) | The Tree Builds Without A Container | `build-pi-tree.sh` builds Pi's tree either in the pinned Node container or, with `PI_TREE_DIRECT=1` in `porelo.yml`, directly with the pinned Node under `umask 022`, producing the same tree byte for byte where no nested container can start. |
| <a id="pi32"></a>[PI32](README.md#pi32) | Move To Pi 1 | The package ships Pi 1.3.0, dropping the 13 advisories in Pi 0.85.0's dependencies, with each measured claim in `pi.yaml` naming the version it was checked on. |
| <a id="pi27"></a>[PI27](README.md#pi27) | Declare The App Copilot Is Granted Through | `pi.yaml` declares the GitHub App `Iv1.b507a08c87ecfe98` that Pi signs in with for `porelo vault authorize github-copilot`, `BuiltTreeCheck` holds the shipped tree to that id, and `doc/index.md` states whose app it is cited as and where a grant is withdrawn. |
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
