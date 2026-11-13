# PI44 — A Copilot Task Uses The Models The Account Has

```json
{
  "$schema": "issue-1.json",
  "id": "PI44",
  "title": "A Copilot Task Uses The Models The Account Has",
  "state": "Estimated",
  "size": "M",
  "priority": "Low",
  "rank": 4,
  "blockedBy": [],
  "covers": "a task on GitHub Copilot runs on a model the account has: no flagless failure on a default it lacks, and `--model` takes every model the account has",
  "openQuestions": 0
}
```

**Why this size.** One module, but more than one file and a document: the routing extension `PiRoutingExtension`
writes gains a Copilot part that asks the broker for the account's models when Pi starts, a check runs it in the
built tree's Pi against a stand-in `/models`, and `doc/index.md` and `doc/decisions.md` change what they say a
Copilot model must be. No contract with Porelo changes: the list is fetched inside the task, through the broker,
with the task's token, so an imported sign-in and a grant are served alike.

**Estimate.** M: 53 min, the measured median of M over all repositories (n=113).

**What must be true.** A task on GitHub Copilot runs on a model the person's account has: started without
`--model` it asks for none the account lacks, or stops saying which model is missing and what to do, and
`--model` takes every model the account has.

## Why

Measured on Pi 1.4.0 with a real Copilot sign-in, on an account whose model list lacks Pi's default:

- **Without `--model` the task fails at its first turn.** Pi's built-in default for Copilot is `gpt-6.1-sol`,
  and Copilot answers it with 400 `model_not_supported`. At a terminal, Pi's `/login` says the default is not
  available and points to `/model`; a task shows nothing of this.
- **`--model` refuses a model the account has.** `gpt-6.1-terra` is in the account's list and runs in Pi outside
  a task; in a task Pi stops with `Model "gpt-6.1-terra" not found`. A model in the catalogue of the shipped
  tree, such as `gpt-5.6-terra`, runs.

The sign-in on the host records the account's models, `availableModelIds` in Pi's `auth.json`, and Pi's model
store adds what the tree's catalogue lacks. A task holds neither, so its Pi knows only the catalogue and its default.

**Read in the pinned Pi 1.5.0** (the tree `build-pi-tree.sh` builds):

- The default is still `"github-copilot": "gpt-6.1-sol"` (`pi-coding-agent/dist/core/model-resolver.js`,
  `defaultModelPerProvider`). Without `--model`, `findInitialModel` takes a default from Pi's settings, else that
  default if it is among the available models, else the first available model.
- The catalogue has grown: `gpt-6.1-terra` is in 1.5.0's (`pi-ai/dist/providers/data/github-copilot.json`). The
  defect stays for every model an account has beyond the catalogue.
- Pi narrows Copilot's catalogue to `availableModelIds` only for an `oauth` credential
  (`pi-ai/dist/providers/github-copilot.js`, `filterModels`). In a task the credential is the key the routing
  extension registers, so nothing narrows it.
- The model store is `models-store.json` in Pi's directory: pi.dev's catalogue per provider, model data with no
  secret, fetched by `withRemoteCatalog`. A task reaches no pi.dev, and a print run does not refresh from the
  network at all (`ModelRuntime.create` without `allowModelNetwork`).
- `--model x` with no provider resolves only among known models and stops with `not found`; a provider prefix
  (`github-copilot/x`) takes an unknown id as a custom model built on the default's API, `openai-responses`,
  wrong for Copilot's Claude models.
- Pi lists an account's models itself: `GET <base>/models` with the Copilot token, keeping models whose
  `capabilities.supports.tool_calls` is not false and whose picker and policy allow them
  (`pi-ai/dist/auth/oauth/github-copilot.js`, `parseGitHubCopilotModelCatalog`). Porelo's Copilot provider serves
  every path at the root, `/models` named among them (porelo's `providers/github-copilot.yaml`).
- An extension's factory is awaited (`core/extensions/loader.js`), `registerProvider` takes `models`, which replace
  the provider's, and an extension may import `@earendil-works/pi-ai/providers/all`, whose `getBuiltinModels`
  gives the catalogue's full entries.

## Plan

- **Where the list comes from:** inside the task, through the broker, not from the host. The extension already
  holds the broker's base URL and the task's token; a grant, which has no list, is served the same way as an
  imported sign-in, and nothing new crosses from the host. Carrying `availableModelIds` from the host would need a
  new field from the vault to `SetupContext`, a contract change in porelo, and would still leave a grant without.
- `PiRoutingExtension.document(provider, endpoint, token)`: for `github-copilot` only, the factory becomes
  `async`, fetches `<endpoint>/models` with `Authorization: Bearer <token>` and Copilot's editor headers (the
  ones `github-copilot.js` sends), applies Pi's own selection (tool calls, picker, policy, with Pi's fallback to
  policy-enabled models), and registers the provider with `models`: the catalogue's entry for each id the
  account has (`getBuiltinModels("github-copilot")`), and for an id the catalogue lacks an entry built from the
  answer (`supported_endpoints` gives the API: `/v1/messages` to `anthropic-messages`, `/responses` to
  `openai-responses`, else `openai-completions`; limits from `capabilities.limits`). Other providers' file stays
  as it is.
- Without `--model`, when the account lacks Pi's default, the extension stops the start with one line naming the
  account's models and asking for `--model`; it never lets Pi's `findInitialModel` pick another. The red-first
  check gets a third case: a stand-in `/models` without Pi's default and no `--model` ends the start with that line.
- When `/models` fails or answers nothing usable, the extension registers as today (catalogue, Pi's default)
  and writes one line to stderr saying the account's models could not be read; the start is not refused.
- `PiContainerSetup` is unchanged: the file stays `secret`, holding the token as before.
- Docs: `doc/index.md` (Copilot: any model the account has; without `--model` what the answer sets),
  `doc/decisions.md` *What is proven about brokering this agent* (the line "a Copilot model must be one Pi
  lists" and its index row).
- **Red first:** a check in `BuiltTreeCheck` runs the tree's `pi --list-models github-copilot` with
  `PI_CODING_AGENT_DIR` set to a temporary directory holding the generated extension, its endpoint a local
  stand-in answering `/models` with one catalogue id, one id outside the catalogue and one with
  `tool_calls: false`. It expects exactly the first two; today it lists the whole catalogue. A second case answers
  500 and expects the catalogue and the stderr line.
- `PiContainerSetupTest`: the Copilot file fetches `/models` from the endpoint it was given; an OpenRouter file
  does not.
- Level: unit for the text, the tree check for the behaviour (it runs where the tree is built, as the status
  extension's check does). A real account is measured once by hand on the VM, as the decisions record Copilot
  today: a start without `--model` and a start with a model outside the catalogue.

## Acceptance

- In a Copilot task, `pi --list-models` shows the account's models and no other Copilot model.
- With a real Copilot account whose list lacks Pi's default: a start without `--model` behaves as the answer to
  the open question says, never a 400 `model_not_supported` at the first turn.
- `--model <id>` runs for an id the account has that the catalogue lacks; an id the account lacks stops at start
  with Pi's `not found`, not with a 400 from Copilot.
- A `/models` that fails leaves the task as it starts today, with one line saying so; no token appears in that
  line or in any log.
- An OpenRouter task's routing file is unchanged.

## What makes it uncertain

- The broker forwarding a `GET /models` with Copilot's editor headers is read in its provider file, not measured;
  if it refuses the path or drops the headers, porelo's broker changes first and the size grows to L.
- Copilot's `/models` answer is undocumented; Pi's parser is the only reading of it. An id outside the catalogue
  gets limits and API from that answer, and a wrong API shows only at its first turn.
- The fetch adds a request to every start, bounded by a short timeout as Pi's own (5 s).
- A new Pi pin may move `registerProvider`, `getBuiltinModels` or the alias for `providers/all`; the tree check
  catches that at build.


## Decisions

- The operator, 2026-11-11: a Copilot task without `--model`, on an account that lacks Pi's default, stops at the
  start. It names the account's models and asks for `--model`; it never picks a model by itself.

## To be checked

- That the broker forwards `GET /models` for `github-copilot` with the task's token and Copilot's headers, and
  what it answers for a grant and for an imported sign-in, measured on the VM.
- Whether Copilot's `/models` reached with the GitHub token (as the broker presents it) answers the same list as
  with the session token Pi uses at a terminal.
