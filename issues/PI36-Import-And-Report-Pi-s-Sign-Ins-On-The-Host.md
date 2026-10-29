# PI36 — Import And Report Pi's Sign-Ins On The Host

**Status:** Analyzed; waits for the agent API's findings method and for the provider asked for reaching
an agent's `ExtractCredential`.
**Size:** M - a new credential extractor and the findings in this module, the definition's `config_dir`, and an
acceptance step on the VM; it depends on a contract change in porelo that this issue does not make.

**What must be true.** `porelo vault import` takes a sign-in Pi already holds on the host, for the provider asked
for; and asked what it finds of itself, the Pi adapter answers which providers Pi is signed in to there, by kind and
with the path, and never a credential's value.

## Why

Porelo makes the agent already on the machine the default and offers `vault import` for what it finds. Today Pi's definition has no `config_dir` and no
extractor, so `vault import` answers that Pi does not say where it keeps its credentials.

## Plan

- **Where Pi keeps them:** `~/.pi/agent/auth.json` (`$PI_CODING_AGENT_DIR` moves the directory), one entry per
  provider id: `{"type": "api_key", "key": …}` or `{"type": "oauth", "access", "refresh", "expires"}`. Read in Pi's
  source, `coding-agent/src/core/auth-storage.ts`.
- **One file, several providers.** `CredentialExtractor.extract(configDirectory)` returns one credential, and
  `CredentialImport` stores it under the agent's default provider. So Pi's import needs the provider passed in, an
  optional parameter of `ExtractCredential` under the agent API's rule for a
  compatible change; that is Porelo's change.
- **`PiCredentialExtractor`**: the entry for the provider asked for, Pi's provider id mapped to Porelo's provider name
  (Porelo declares `openrouter`, `anthropic` and `github-copilot`); an OAuth entry keeps `refresh` and `expires` as
  attributes, as Claude's does.
- **A `key` that is a command (`!…`) or an environment reference is never run or resolved**: it is reported as
  "signed in through a command" and not imported.
- **`config_dir: "~/.pi/agent"`** in `pi.yaml`.
- **The findings**: every entry by provider and kind, with the path; an entry Porelo has no provider for is named as
  such.
- **Red first:** `PiCredentialExtractorTest` over an `auth.json` with two providers, an API key, an OAuth entry and a
  command key: the provider asked for is taken, the other is not, the command is not run; a guard that no finding
  contains a key.

## Acceptance

- On the VM's `smith`, with Pi signed in to OpenRouter by API key, `porelo vault import --agent pi` stores it under
  `openrouter`, and the findings name OpenRouter, an API key, from `~/.pi/agent/auth.json`.
- A command key is reported and never run, seen with a command that would write a marker file.

## To be checked

- `auth.json`'s shape in the pinned Pi 1.3.0, not only at the reference checkout's head.
- Pi's provider ids for the providers Porelo declares, read in the pinned tree.
