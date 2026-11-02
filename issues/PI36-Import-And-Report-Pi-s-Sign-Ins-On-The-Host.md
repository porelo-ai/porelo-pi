# PI36 — Import And Report Pi's Sign-Ins On The Host

**Status:** Review; acceptance through the installed adapter on a VM, built against the published agent API.
**Size:** M - a new credential extractor and the findings in this module, the definition's `config_dir`, and an
acceptance step on the VM; it depends on a contract change in porelo that this issue does not make.

**What must be true.** `porelo vault import` takes a sign-in Pi already holds on the host, for the provider asked
for; and asked what it finds of itself, the Pi adapter answers which providers Pi is signed in to there, by kind and
with the path, and never a credential's value.

## Why

Porelo makes the agent already on the machine the default and offers `vault import` for what it finds. Today Pi's definition has no `config_dir` and no
extractor, so `vault import` answers that Pi does not say where it keeps its credentials.

## Plan

- **Where Pi keeps them:** `.pi/agent/auth.json` in the home, one entry per provider id, and an `apiKey` per provider in
  `models.json` beside it. `$PI_CODING_AGENT_DIR` moves the directory, which reaches the adapter only as the
  `configDirectory` Porelo passes.
- **One file, several providers.** `CredentialExtractor.extract(configDirectory)` returns one credential, and
  `CredentialImport` stores it under the agent's default provider. So Pi's import needs the provider passed in, an
  optional parameter of `ExtractCredential` under the agent API's rule for a compatible change; that is Porelo's change.
- **`PiFindings`** answers `Find`:

  | Entry | Finding |
  |---|---|
  | `auth.json`, `api_key` with a literal key | `SIGNED_IN`, importable, `API_KEY` |
  | `auth.json`, `oauth` for `anthropic` or `github-copilot` | `SIGNED_IN`, importable, `SUBSCRIPTION` |
  | `auth.json`, `oauth` for `openrouter` | `SIGNED_IN`, importable, `API_KEY`: OpenRouter's flow ends in a key |
  | `auth.json`, `oauth` for `github-copilot` without `refresh` | `SIGNED_IN`, not importable: `refresh` is the GitHub token Porelo keeps |
  | `auth.json`, `oauth` for `github-copilot` with `enterpriseUrl` | `SIGNED_IN`, not importable, no `provider`: another host than Porelo's Copilot |
  | a key `!command` | `SIGNED_IN`, not importable; never run |
  | a key with `$`, a missing key, a shape not understood | `CANNOT_SAY`: it depends on Pi's environment, or cannot be told |
  | a provider Porelo does not declare | `SIGNED_IN`, not importable, no `provider`; `kind` only for an API key |
  | `models.json`, an `apiKey` for a provider `auth.json` does not hold | as a key above, never importable; no `provider` with a `baseUrl` |
  | a file that is broken or cannot be read, or holds one entry Pi refuses | one `CANNOT_SAY` with its path: Pi then refuses the whole file |

  The contract has no free-text field, so a provider Porelo has no name for is not named. A literal key holding a `$`
  or the escapes `$$` and `$!`, and a reference the entry's own `env` answers, read as a reference: Pi is ready with
  them, and they stay "cannot say" rather than being resolved here. `models.json` may hold comments, which Pi takes
  and the adapter's parser does not: such a file reads as "cannot say".
- **The order:** the default Pi was given (`defaultProvider` with `defaultModel` in `settings.json`), then the
  providers Porelo declares in Pi's own order (anthropic, openai, google, github-copilot, openrouter), then the rest.
  Pi's `enabledModels` and a session's restored model are not followed.
- **`PiCredentialExtractor`**: the entry for the provider asked for, as `Find` reports it importable. Anthropic keeps
  `refresh` and `expires` as attributes with the same client as Claude's; OpenRouter stores `access` as an API key;
  Copilot stores `refresh`, the GitHub token, as `access-token`, the type `vault authorize` keeps it under. A file Pi
  would refuse fails the import, and the answer names its provider.
- **`config_dir`** naming `.pi/agent` in the home in `pi.yaml`.
- **Red first:** `PiFindingsTest` and `PiCredentialExtractorTest` over each entry above: the provider asked for is
  taken, the other is not, the command is not run; a guard that no finding contains a key.

## Acceptance

- On a VM, with Pi signed in to OpenRouter by API key, `porelo vault import pi --provider openrouter`
  stores it under `openrouter`, and the findings name OpenRouter, an API key, from `.pi/agent/auth.json` in the home.
- A command key is reported and never run, seen with a command that would write a marker file.

## What Pi does

Measured on the pinned Pi 1.4.0 on a VM, with made-up values, `pi auth check --provider <id> --no-refresh
--json`:

| In `auth.json` | Pi says |
|---|---|
| a literal key; `oauth`; `openrouter` as `oauth` with an empty `refresh`; `github-copilot` past `expires` | `ready` |
| a key `!command` | `ready`, and the command is not run by this check |
| a key `$UNSET`; no key; an empty key | `not_ready` |
| a key `$NAME` that the entry's `env` answers; a literal with `$$` | `ready` |
| a broken file; a list; one entry of an unknown type, or `oauth` without `refresh`, beside a good one | `invalid`, for every provider |
| a byte order mark | `ready` |

A key in `models.json` makes Pi `ready`, comments included; a keyless entry in `auth.json` wins over it, `not_ready`.
Read in the package and not measured: `enterpriseUrl` on Copilot, in `dist/auth/oauth/github-copilot.js`, which needs a
sign-in on such a host; and the order, in `dist/core/model-resolver.js`.

## Checked

- From the reading: a key beside a `models.json` that cannot be read, such as one holding a comment, is reported and not
  importable, since that file may move the provider. Declined: a file changed between `Find`'s read and the import's, an
  edit by the person in that instant, whose command text would be stored and not run; a key with a raw newline, invalid
  to Pi and accepted by the wire's parser, which harms nobody.
- On a VM, with made-up values, the installed adapter answered as above over its socket: `vault import pi --provider
  openrouter` stored the key, `--provider anthropic` refused, and a start took Pi, naming the provider and the file.
- A key `!touch <file>`: reported, not importable, not imported, and the file was not made.
- `login:` holding only its flow and grants reads as no login, held by a test.

## To be checked

- That a Copilot entry imported this way serves a task as the one `vault authorize` makes does, which also keeps the
  grant's settings: it needs a real Copilot sign-in, and has not run.
