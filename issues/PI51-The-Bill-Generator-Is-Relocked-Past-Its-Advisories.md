# PI51 — The Bill Generator Is Relocked Past Its Advisories

```json
{
  "$schema": "issue-1.json",
  "id": "PI51",
  "title": "The Bill Generator Is Relocked Past Its Advisories",
  "state": "Estimated",
  "size": "S",
  "priority": "Medium",
  "rank": 1,
  "blockedBy": [],
  "covers": "the bill generator's lock resolves no release with a published advisory; it never ships, so low in priority",
  "openQuestions": 0
}
```

**Why this size.** One module: `buildtools/sbom/package.json` and its lock, one flag in `buildtools/build-pi-tree.sh`,
a test beside the ones that already read that script, and a changelog line. No contract another repository reads. The
relock and the tree build need podman; npm runs only inside the builder image.

**Estimate.** S: 27 min, the measured median of S over all repositories (n=167).

**What must be true.** `buildtools/sbom/package-lock.json`, the bill generator's lock, resolves no release with a
published advisory: on 2026-11-11, by OSV over every package of the lock, fast-uri 3.1.8 (4 advisories, fixed in 3.1.9),
glob 10.5.0 (1, fixed only in 14.0.2) and minimatch 9.0.9 (1, fixed only in 11.0.1), and nothing else. The generator
never ships: it is installed with `--ignore-scripts`, runs once on the build machine against this repository's own
lockfile and is removed before anything is packaged. So this is low in priority. The bill keeps being validated
against its schema before it is written, as it is today.

## Plan

Read in today's lock (`@cyclonedx/cyclonedx-npm` 6.0.1, the only direct dependency, 176 packages):

- fast-uri 3.1.8 <- ajv 8.20.0 (`^3.0.1`) <- an optional dependency of cyclonedx-npm 6.0.1. A plain relock would take
  3.1.9.
- glob 10.5.0 <- cacache 19.0.1 (`^10.2.2`) <- make-fetch-happen 14 <- node-gyp 11.5.0 <- libxmljs2 0.37.0 <- an
  optional dependency of cyclonedx-npm 6.0.1; minimatch 9.0.9 <- that glob (`^9.0.4`). No relock fixes these: libxmljs2
  0.37.0 is its latest and needs node-gyp `^11.2.0`, and even the latest cacache wants glob `^13`. libxmljs2 serves
  only XML output and its native build never runs under `--ignore-scripts`; the build writes JSON.
- `@cyclonedx/cyclonedx-npm` 7.0.0 (2026-11-09) drops the optional dependencies ajv, ajv-formats,
  ajv-formats-draft2019 and libxmljs2; they stay optional peers of `@cyclonedx/cyclonedx-library` (`^10.4.0`), which
  npm does not install. Its other changes: `--spec-version` defaults to 1.7 (the build passes 1.6), and `--validate`
  now fails when no validator is installed, while the default still skips validation silently.

Steps:

1. `buildtools/sbom/package.json`: `@cyclonedx/cyclonedx-npm` 7.0.0, plus `ajv`, `ajv-formats` and
   `ajv-formats-draft2019` pinned exactly, so the JSON validation that 6.0.1 brought along stays. libxmljs2 is left
   out.
2. Relock inside the relock image, `porelo.release.npm.image` in `pom.xml` (the builder `build-pi-tree.sh` installs
   in, kept equal by `PinAgreementTest`): `podman run` with the directory mounted, `npm install --package-lock-only
   --ignore-scripts`. The earlier relock of this lock was done the same way, by hand; neither `update.yml` nor
   `porelo-release` touches this lock.
3. `buildtools/build-pi-tree.sh`: the `cyclonedx-npm` call gets `--validate`, so a validator missing from the lock
   fails the build instead of skipping the check. That script changed, so its regression matrix in
   `doc/decisions.md` is run and its result recorded in the commit.
4. `doc/decisions.md`, "The bill generator is installed from its own lockfile": one sentence that the generator's
   validator is a dependency of its own and the call enforces it.
5. `CHANGELOG.md` under `[Unreleased]`: the shipped bill is now written by cyclonedx-npm 7.0.0 (its `metadata.tools`
   names the generator).

Red first: in `PinAgreementTest`, a test that the `cyclonedx-npm` call in `build-pi-tree.sh` carries `--validate` and
that `buildtools/sbom/package.json` names `ajv` among its dependencies; it fails on today's script. Level: unit, over
the files; the tree build and the OSV check below are the acceptance.

## Acceptance

- An OSV query (`api.osv.dev/v1/querybatch`) over every package of `buildtools/sbom/package-lock.json` returns no
  advisory; the lock holds no glob, minimatch, node-gyp or libxmljs2, and fast-uri at 3.1.9 or later.
- `build-pi-tree.sh` builds the tree in podman, its log shows the bill validated ("BOM result appears valid"), and
  `merge-tree-bill`, `BuiltTreeCheck` and `PackageNoticesCheck` pass on it; the tree's bill names the same components
  as one written by 6.0.1 from the same lock.
- With `ajv` removed from the generator's lock, the build fails at the bill instead of writing an unvalidated one.
- The script's regression matrix still refuses each case with exit 2.

## What makes it uncertain

- 7.0.0 is two days old; a fix release may follow. Its bill may differ from 6.0.1's in more than the tool's version
  (field order, new 1.6 fields); `merge-tree-bill` or `compare-bills` in `porelo-release` may react to that, which
  would grow this towards M.
- If 7.0.0 turns out not to fit, the fallback is 6.0.1 with a relock for fast-uri and npm `overrides` forcing glob
  `^14.0.2` and minimatch `^11.0.1` under cacache 19, which declares glob `^10`; that is safe only because node-gyp
  never runs here, and it is an override across majors to explain in the decision.
- New advisories may appear in ajv's tree (ajv-formats-draft2019 pulls the unmaintained uri-js) between now and the
  build; the OSV check is repeated at the commit.

## To be checked

- That `merge-tree-bill` and `compare-bills` accept a 7.0.0 bill: diff a 6.0.1 and a 7.0.0 bill of the same tree.
- That the relock image's npm writes lockfile version 3 as today.
