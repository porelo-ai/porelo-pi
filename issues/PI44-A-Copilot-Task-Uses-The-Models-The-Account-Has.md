# PI44 — A Copilot Task Uses The Models The Account Has

```json
{
  "$schema": "issue-1.json",
  "id": "PI44",
  "title": "A Copilot Task Uses The Models The Account Has",
  "state": "Accepted",
  "size": "-",
  "priority": "Medium",
  "rank": 4,
  "blockedBy": [],
  "covers": "a task on GitHub Copilot runs on a model the account has: no flagless failure on a default it lacks, and `--model` takes every model the account has"
}
```

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

## To be checked

- Whether the model list and the store's entries can reach a task with no secret among them, from a sign-in
  imported on the host, and what happens when the vault's entry came from a grant, which has no such list.
- Which model a task should take without `--model` once the list is known, and whether that is the adapter's
  choice or Pi's.
