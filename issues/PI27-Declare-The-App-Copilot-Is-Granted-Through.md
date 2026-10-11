# PI27 — Declare the app Copilot is granted through

**Status:** Idea.

**What must be true.** On a vault without a GitHub Copilot entry, `sokar vault authorize github-copilot` with
Pi installed signs in through the same OAuth app Pi uses without Sokar, and the agent's documentation says
whose app that is, or that its owner is not verified.

## Why

Sokar grants a provider with one command
([B132](https://github.com/sokar-ai/sokar/blob/main/issues/base/README.md#b132)), and takes the client id from
the installed agent's `login.grants.<provider>`: one id per host and the app's owner. `pi.yaml` declares
none, so the command refuses as for an agent that does not sign in.
