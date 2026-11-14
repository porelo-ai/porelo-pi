# PI65 — The Local Sign-In Goes To A Host

```json
{
  "$schema": "issue-1.json",
  "id": "PI65",
  "title": "The Local Sign-In Goes To A Host",
  "state": "Review",
  "size": "M",
  "priority": "High",
  "rank": 1,
  "blockedBy": [],
  "covers": "on the person's machine the adapter lists each Pi sign-in it finds - provider, kind, account where known, age and expiry where known, and whether it can be carried, never its value - and hands a chosen one to Porelo, which carries it into a rented host's vault",
  "openQuestions": 1
}
```

**Why this size.** `PiFindings` and `PiCredentialExtractor` exist; this extends them. Each route below is read, told apart and tested against a fixture file; no new contract of
the adapter's own, the fields come from Porelo's half.

**Estimate.** M: 53 min, the measured median of M over all repositories (n=113).

**What must be true.** A person with Pi signed in on their own machine carries that sign-in into a rented host's
vault from the window and is not asked to sign in a second time there. The adapter's half: for each route below it
says provider, kind, the account where the file names one, age and expiry where the file holds them, and whether it
can be carried, with no value in what it says; and taking one hands its value to Porelo through the extractor alone,
never through a file, a command line or a log.

Filed 2026-11-14 on the operator's order, under ST362 "Credentials carried to a host". Agent Core files Porelo's half,
the fields and the carrying by stdin or environment; Agent Frontend files the window's dialog.

## Plan

- `auth.json` in the definition's `config_dir`, one entry per provider: Anthropic's OAuth sign-in with its expiry, OpenRouter's key, GitHub Copilot's GitHub token; each carriable.
- A key given as a command or a variable, in `auth.json` or `models.json`: found and named, never run, and not carriable.
- The finder fills the fields Porelo's half adds to a finding; the extractor is the one an import already uses.
- Tests: one fixture per route, each finding's fields asserted, and a test that no finding's text holds any value
  of its fixture. The definition's `config_dir` stays the place read.

## Acceptance

- Each route above is found and said as listed, with its carriable answer, by a test.
- No finding holds a value of its fixture.
- With Porelo's half in, a sign-in found on the person's machine is in the host's vault and a task there runs on it.

## Open questions

- **Two machines on one sign-in.** An OAuth sign-in whose refresh token is replaced at each renewal works on one
  machine at a time: the first to renew signs the other out. Whether a carried sign-in is said to sign the person's
  machine out, or is carried only as a kind that does not rotate, is the operator's call.

## To be checked

- **The facts on the wire.** Porelo's listing of what can be carried names agent, provider and kind, asked of this
  adapter's extractor per provider; it carries no account, expiry or last change, so the facts stay in the adapter
  until a listing takes them, and the third acceptance point is Porelo's walk.

## What was built

- `PiSignInFacts` says, for each sign-in `PiFindings` finds in `auth.json` that the import can take, keyed by the
  finding's provider: the file's last change, and the entry's `expires` (epoch milliseconds) for Anthropic's
  subscription only. OpenRouter's key never expires, and Copilot's `expires` is its session token's, not the
  carried GitHub token's, so both say none.
- No account: the entries pi-ai writes for Anthropic, OpenRouter and GitHub Copilot name none, and a token is never
  decoded for one.
- Carriable is the finding's `importable`, as before. A key given as a command or a variable, a key in
  `models.json`, and a sign-in without a provider of Porelo's (or moved to another address) have no facts.
- The value goes to Porelo only through `PiCredentialExtractor`, the one an import already uses.
- Tests: `PiSignInFactsTest`, 11, each route, no account or expiry guessed, and no fixture value in any fact.
- Mapped onto Porelo's listing of what can be carried, which asks the extractor per provider: each sign-in with facts
  is one the extractor answers for its provider, named, by a test.
- The reading by another model found the issue naming another repository's issue, and two tests that passed with
  the facts read from another provider's entry or with no `auth.json` at all; all fixed. Noted, not fixed: the facts
  read `auth.json` again after the findings, so a file changed in between is described as it is then.
