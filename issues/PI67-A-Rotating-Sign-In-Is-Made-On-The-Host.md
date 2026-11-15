# PI67 — A Rotating Sign-In Is Made On The Host

```json
{
  "$schema": "issue-1.json",
  "id": "PI67",
  "title": "A Rotating Sign-In Is Made On The Host",
  "state": "Idea",
  "size": "M",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "which of Pi's sign-ins rotate their refresh token, measured on the pinned CLI and declared per provider and kind, and how Pi signs in on a headless host instead of having a rotating sign-in carried there",
  "openQuestions": 1
}
```

**Why this size.** M: one measurement per sign-in kind on the pinned CLI, and one contract change - the definition says per provider and kind whether its refresh token rotates, which Porelo reads; its tests and the docs move with it.

**Estimate.** M: 53 min, the measured median of M over all repositories (n=113).

**What must be true.** A person who runs Pi on a rented host gets each of its sign-ins there without carrying one that would sign their own machine out: GitHub Copilot by GitHub's device code, OpenRouter by its key, and Anthropic's subscription by a sign-in made on the host.

## Plan

- **The contract, in words.** Porelo no longer carries a sign-in whose refresh token is replaced at each renewal from the person's machine to a host; the host makes its own, and an API key is carried as before. So the definition says, for each provider and kind it signs in to, whether that refresh token rotates. The key's name and shape are agreed with Porelo's agent before it is written; until then this plan names it `rotates`.
- **How rotation is measured, never assumed.** First, read in the pinned CLI whether it stores a refresh token its renewal answers with. Then one real renewal per kind on a test sign-in, on a VM: the digest of the refresh token before and after, compared, never the token itself printed. A kind no account here can reach stays unmeasured and is said so, never declared from the CLI's code alone.
- **The kinds, as read in the code today (1.5.0):**
  - Anthropic subscription, `auth.json` `oauth`: a refresh token, renewed on the host like Claude Code's. Whether Anthropic replaces it at renewal is the measurement, shared with Claude Code's.
  - GitHub Copilot, `auth.json` `oauth`: its `refresh` is the GitHub token from the app the definition grants; whether that grant expires or rotates is the measurement.
  - OpenRouter key: a key; does not rotate.
- **The sign-in on a headless host:**
  - GitHub Copilot: `porelo vault authorize github-copilot` already takes GitHub's device flow on any machine (`login.flow: device-code`); on a host nothing changes.
  - Anthropic subscription: there is no host sign-in for Pi today - only an import. Either Pi's own Anthropic login runs in a throwaway container as Claude Code's does, read back through `PiCredentialExtractor`, or the sign-in `porelo vault login claude` makes serves Pi's `anthropic` provider too. Which one is measured first: whether Pi's login prints a link and a paste prompt without a browser.
- The findings and the facts stay; a kind declared rotating is still found and described, and Porelo, not the
  adapter, refuses to carry it.
- **Tests:** a definition test that each provider and kind carries its measured answer; for the Anthropic host sign-in, an extractor test over the file the chosen login writes, and the login itself against a VM.

## Acceptance

- Each kind above has its rotation answer in the definition, each from a measurement named in `doc/decisions.md`,
  or is said unmeasured.
- On a VM without a browser, the sign-in made on the host as above ends in the host's vault, and a task runs on it.
- A test fails when a measured kind's answer is taken out of the definition.

## Open questions

- **The accounts for the measurement.** A real renewal per kind needs a test sign-in to each provider; a renewal of a rotating one signs out every other place that holds it. Which accounts may be used, and that none of them is one the person also uses on their own machine, is the operator's call.

## To be checked

- **The key's name.** `rotates` and where it stands in the definition, agreed with Porelo's agent, which reads it.
- **Pi's Anthropic login without a browser.** Whether the pinned Pi's Anthropic login prints a link and takes a pasted code with no browser, measured on a VM; it decides between Pi's own login and Claude Code's.
