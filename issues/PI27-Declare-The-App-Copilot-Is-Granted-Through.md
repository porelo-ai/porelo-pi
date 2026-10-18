# PI27 — Declare the app Copilot is granted through

**Status:** Implementing.
**Size:** S - a `login.grants` block in `pi.yaml`, its test, a check in the weekly update and a paragraph in
`doc/`; Porelo's side is built.

**What must be true.** On a vault without a GitHub Copilot entry, `porelo vault authorize github-copilot` with
Pi installed signs in through the same OAuth app Pi uses without Porelo, and the agent's documentation says
whose app that is, or that its owner is not verified.

## Why

Porelo grants a provider with one command, `porelo vault authorize <provider>`, and takes the client id from
the installed agent's `login.grants.<provider>`: one id per host and the app's owner. `pi.yaml` declares
none, so the command refuses as for an agent that does not sign in.

**What Pi 0.85.0 signs in with,** read in the tree this package ships
(`@earendil-works/pi-ai`, `dist/auth/oauth/github-copilot.js`):
- **One client id on every host:** `Iv1.b507a08c87ecfe98`, kept base64-encoded in the source, with
  `read:user`.
- **Pi presents itself as an editor:** its requests carry `Editor-Version: vscode/…`,
  `Editor-Plugin-Version: copilot-chat/…` and `Copilot-Integration-Id: vscode-chat`.
- **Its source does not say whose app the id is.** It is widely cited as GitHub's own Copilot app for
  editors; not verified.

## Plan

- **`src/main/resources/agent/pi.yaml`:** a `login.grants.github-copilot` block, read by Porelo's
  `AgentDefinitionReader`, with `hosts` mapping `"*"` to `Iv1.b507a08c87ecfe98`, as Pi uses it on every host.
  Its `owner` says the source names none, and that it is cited as GitHub's Copilot app for editors, not
  verified.
- **Red first:** `PiAgentTest` reads the definition through the API and expects that grant; it fails on
  today's `pi.yaml`.
- **The pinned tree is held to that id** by `BuiltTreeCheck`, against the built tree's `github-copilot.js`, plain
  or base64. It runs in every Build and in the weekly update's "Native binary and packages", so a pin move to a
  Pi that signs in with another app stops there. That is in place of a step of its own in `update.yml`; seen red
  with the id in `pi.yaml` changed.
- **`doc/index.md`:** the GitHub Copilot sign-in is the one command. It says plainly that the grant goes
  through the app cited as GitHub's own for its editor plugin, as Pi does without Porelo, that Pi presents
  itself to GitHub as that editor, that the app's owner is not verified, and where a person revokes it
  (GitHub's authorized OAuth apps).
- **On the VM**, with the adapter installed per account: on an empty vault, `porelo vault authorize
  github-copilot` shows a device code, which proves the id reached GitHub, and is then left unfinished. A
  full grant needs an account with Copilot.

## Acceptance

- On an empty vault with Pi installed, `porelo vault authorize github-copilot` starts the device flow with Pi's
  client id; today it is refused.
- `doc/index.md` names the app, says Pi presents itself as that editor, and that the app's owner is not
  verified.

## Measured

- **On `ubuntu26.04`, 2026-10-18**, the adapter of this commit in `smith`'s agents directory, porelo 0.5.0, a
  vault of the run's own: `porelo vault authorize github-copilot` answered "entry github-copilot (oauth-device),
  with the app pi signs in with: Iv1.b507a08c87ecfe98, not named in Pi's source; …", then "open
  https://github.com/login/device" and a code. So GitHub's device flow takes that id from Porelo, which sends
  none of Pi's editor headers. The run was left unfinished and the vault removed; a full grant needs an account
  with Copilot.
