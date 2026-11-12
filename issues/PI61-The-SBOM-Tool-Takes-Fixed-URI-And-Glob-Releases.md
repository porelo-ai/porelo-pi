# PI61 — The SBOM Tool Takes Fixed URI And Glob Releases

```json
{
  "$schema": "issue-1.json",
  "id": "PI61",
  "title": "The SBOM Tool Takes Fixed URI And Glob Releases",
  "state": "Review",
  "size": "XS",
  "priority": "Low",
  "rank": 1,
  "blockedBy": [],
  "covers": "buildtools/sbom's lock takes fast-uri 3.1.9, closing four advisories, and glob and minimatch either move to their fixed majors or stay as an accepted risk written down, so GitHub's Dependabot alerts on main are answered"
}
```

**Why this size.** One lock file refreshed and, for glob and minimatch, either an `overrides` entry or a decision
written down; nothing that ships changes.

**What must be true.** GitHub reports five Dependabot alerts on `main` (3 moderate, 2 low; 2026-11-12). Every one is
in `buildtools/sbom/package-lock.json`, the build-time SBOM generator `@cyclonedx/cyclonedx-npm` 6.0.1 and what it
pulls in as development dependencies; none is in `src/main/npm`, which the package ships (checked against OSV the same
day, no advisory there). Each alert is closed by a fixed release or answered by an accepted risk.

## Plan

- Read on 2026-11-12 against OSV, the lock as on `main`:
  - `fast-uri` 3.1.8, under `ajv`'s `^3.0.1`: GHSA-333x-w4r6-6crp and GHSA-xhq7-xmqj-769h (low), GHSA-w74x-v6f9-j3xg and
    GHSA-wjr9-j795-jxcr (moderate), all fixed in 3.1.9, inside the range: a refreshed lock closes them.
  - `glob` 10.5.0, under `cacache`'s `^10.2.2`: GHSA-3g3m-2fjr-6pw4 (moderate), fixed only in 14.0.2.
  - `minimatch` 9.0.9, under `glob`'s `^9.0.4`: GHSA-g3hv-75wg-m249 (moderate), fixed only in 11.0.1.
  - GitHub counts five where OSV lists six; which pair it counts as one is to be read in the alerts.
- **`buildtools/sbom/package-lock.json`**: `fast-uri` to 3.1.9 by refreshing the lock with the npm the build uses.
- **glob and minimatch**: try `overrides` to their fixed majors and run the SBOM step; if cacache or the tool breaks,
  keep them and write the accepted risk in `doc/decisions.md`: a build-time tool reading only this repository's own
  lock files, with no untrusted glob pattern and no cancellation in its path.
- **Level:** the SBOM step of the Build green with the new lock and the bill unchanged in its shipped components;
  the alerts closed on GitHub after the push.

## Acceptance

- No Dependabot alert on `main` stays open without a fix or a written accepted risk.
- The shipped bill of materials is unchanged.

## What was measured

- Against [PI51](README.md#pi51): its relock to `@cyclonedx/cyclonedx-npm` 7 drops `libxmljs2`, so these overrides go
  with it; this is the fallback that issue names, applied early.

- In the pinned builder image (Node 22.23.4, npm 10.9.9): `npm update fast-uri --package-lock-only` took 3.1.9;
  `overrides` `glob` 14.0.2 and `minimatch` 11.0.1 then installed with `npm ci --ignore-scripts`, and the lock dropped
  what only glob 10 had pulled in (jackspeak, cliui and the like) and gained two of glob 14's. Both reach the tree
  only under `libxmljs2`'s `node-gyp`, which `--ignore-scripts` never runs.
- `./mvnw -Pdist exec:exec@build-pi-tree` with the old lock and with the new one: both exit 0, and the two tree
  bills hold the same 125 components with the same hashes and the same dependency graph.

## To be checked

- Which advisory pair GitHub's alerts count as one, read in the alerts by someone signed in.
