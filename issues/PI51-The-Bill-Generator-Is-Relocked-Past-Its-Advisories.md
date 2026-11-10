# PI51 — The Bill Generator Is Relocked Past Its Advisories

```json
{
  "$schema": "issue-1.json",
  "id": "PI51",
  "title": "The Bill Generator Is Relocked Past Its Advisories",
  "state": "Idea",
  "size": "-",
  "priority": "Medium",
  "rank": 1,
  "blockedBy": [],
  "covers": "the bill generator's lock resolves no release with a published advisory; it never ships, so low in priority"
}
```

**What must be true.** `buildtools/sbom/package-lock.json`, the bill generator's lock, resolves no release with a
published advisory: on 2026-11-08 fast-uri 3.1.8 (fixed in 3.1.9; 2 moderate, 2 low), glob 10.5.0 (1 moderate) and
minimatch 9.0.9 (1 moderate). The generator never ships: it is installed with `--ignore-scripts`, runs once on the
build machine against this repository's own lockfile and is removed before anything is packaged. So this is low in
priority, and is done by relocking to a `@cyclonedx/cyclonedx-npm` release whose tree no longer holds them, or by
overrides where none does.
