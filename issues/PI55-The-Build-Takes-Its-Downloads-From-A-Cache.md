# PI55 — The Build Takes Its Downloads From A Cache

```json
{
  "$schema": "issue-1.json",
  "id": "PI55",
  "title": "The Build Takes Its Downloads From A Cache",
  "state": "Review",
  "size": "S",
  "priority": "Medium",
  "rank": 1,
  "blockedBy": [],
  "covers": "shfmt, Node, fd, ripgrep, the Node builder image and the npm packages of Pi's tree come from the Actions cache, keyed by digest or lockfile, so an outage of their hosts no longer fails a Build that already ran once; an entry nobody reads for 7 days is removed by GitHub"
}
```

**Why this size.** one script, two composite actions and three workflows of this repository; no contract another
repository reads.

**What must be true.** A Build, an update run or the shared-rules check that runs on a pin it has seen before takes
every outside download it needs from the Actions cache, not from github.com, nodejs.org, docker.io or the npm
registry: an outage of one of those hosts then fails only the first run on a new pin. Each cached file is still checked
against its pin before it is used, and an entry of an old pin goes by itself, so the cache never fills up.

## Plan

- `.github/actions/pinned-shfmt` puts the pinned shfmt on `PATH` for the four jobs that need it (`build.yml` twice,
  `shared-rules.yml`, `update.yml`), kept under `pinned-shfmt-<sha256>`.
- `.github/actions/pi-tree-downloads`, called before the native build in `build.yml` and `update.yml`, reads the tree's
  pins from `pom.xml` and keeps:
  - Node, fd and ripgrep in `target/pi-downloads`, where `build-pi-tree.sh` keeps a file that matches its pin, under
    `pi-downloads-<node sha256>-<fd sha256>-<rg sha256>`; the direct build without podman unpacks Node from there.
  - The builder image, pulled once by its index digest and saved as an OCI archive by its image id, with that id
    beside it, under `pi-builder-<index digest>`. A saved image no longer carries the index digest, so a restored one
    is loaded, used only when podman finds it under the recorded id, and handed to the script as
    `PI_BUILDER_IMAGE_ID`, which runs it with `--pull=never`.
  - npm's cache under `pi-npm-<hash of both lockfiles>`, set as `PI_NPM_CACHE`; `build-pi-tree.sh` mounts it as
    npm's cache in the builder, or names it to npm in a direct build. The workflow saves it after the native build,
    which is what fills it. `npm ci` still checks every package against the lockfile's integrity hash.
- Every file is checked against its pin when it is taken from the cache, as when it is fetched.
- Retention: GitHub removes a cache entry nobody read for 7 days (a period the repository's Actions settings can
  change), and every key carries the digest or the lockfiles' hash, so an old pin's entries go by themselves while the
  current ones are read on every Build. No step deletes entries.
- `PinAgreementTest` holds the script: a kept npm cache is mounted outside the tree, a value that could change the
  mount is refused before podman starts, and a loaded builder runs by its id without a pull.

## Acceptance

- The second Build on GitHub on unchanged pins shows a cache hit for each of shfmt, Node, fd, ripgrep, the builder image
  and npm, and its log shows no request to github.com's release downloads, nodejs.org, docker.io or the npm registry.
- A cache entry changed on purpose (one byte of the fd archive) is refused by its digest and fetched again.
- After a pin moves, the Build writes an entry under the new digest, and the old one is gone once nobody read it for
  7 days.
- The packages a cached Build produces equal those of an uncached one, byte for byte.

## Measured

- The builder step on an empty store: pulled by digest, saved, 12 s, an 80 MB archive; on a fresh store from that
  archive: loaded and found by its id in 3 s with no pull; an id file changed to another id stops the step.
- `build-pi-tree.sh` run outside Maven with the loaded image and a kept npm cache, twice: both runs installed the tree
  and stopped where the bill merge needs Maven's classpath; the cache held 32 MB, and no npm cache was in the tree.

## To be checked

- The first Build on GitHub writes the entries, the second shows a hit for each.
