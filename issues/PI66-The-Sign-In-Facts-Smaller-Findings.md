# PI66 — The Sign-In Facts' Smaller Findings

```json
{ "$schema": "issue-1.json", "id": "PI66", "title": "The Sign-In Facts' Smaller Findings", "state": "Idea",
  "size": "-", "priority": "Medium", "rank": 1, "blockedBy": [], "covers": "the smaller gaps left in how the adapter finds and describes a local Pi sign-in for carrying to a host, each fixed or measured" }
```

**What must be true.** What the adapter says of a Pi sign-in on the person's machine, before it is carried to a
host, holds in the cases below too, and each guard there has a test that shows it gone.

## To be checked

- **The facts read `auth.json` a second time.** The facts read the file again after the findings, so a file changed in between is described as it is then, not as the findings saw it.
- **The facts on the wire.** Porelo's listing of what can be carried names agent, provider and kind only; the account, expiry and last change stay in the adapter until a listing takes them, and are then handed over.
