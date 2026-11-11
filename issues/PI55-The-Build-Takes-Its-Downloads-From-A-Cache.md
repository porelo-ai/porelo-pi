# PI55 — The Build Takes Its Downloads From A Cache

```json
{
  "$schema": "issue-1.json",
  "id": "PI55",
  "title": "The Build Takes Its Downloads From A Cache",
  "state": "Analyzed",
  "size": "S",
  "priority": "Medium",
  "rank": 1,
  "blockedBy": [],
  "covers": "shfmt, Node, fd, ripgrep, the Node builder image and the npm packages of Pi's tree come from the Actions cache, keyed by digest or lockfile, so an outage of their hosts no longer fails a Build that already ran once; only the newest entry of each is kept",
  "openQuestions": 0
}
```

**Why this size.** one script and three workflows of this repository, each cache restored and saved beside the step
that fetches today; no contract another repository reads.

**What must be true.** A Build, an update run or the shared-rules check that runs on a pin it has seen before takes
every outside download it needs from the Actions cache, not from github.com, nodejs.org, docker.io or the npm
registry: an outage of one of those hosts then fails only the first run on a new pin. Each cached file is still checked
against its pin before it is used, and the cache holds only the newest entry of each download, so it never fills up.

## Plan

What each job fetches today, outside the Maven repository, the wrapper's Maven and the GraalVM, which the `pinned-jdk`
action already keeps:

- `shfmt` from github.com: `build.yml` (the build job and the job that reads the packages), `shared-rules.yml`,
  `update.yml`, each by `pinned_fetch` into `$RUNNER_TEMP/bin`.
- Node, fd and ripgrep, fetched by `buildtools/build-pi-tree.sh` into `target/pi-downloads` through `pinned_fetch`, in
  `build.yml` and `update.yml` (fd and ripgrep also by the workflow's own step, with the job's token).
- The builder image `docker.io/library/node:<version>-slim@<digest>`, pulled by `podman run` in `build-pi-tree.sh`.
- The npm packages of Pi and of the bill generator, fetched by the two `npm ci` runs in the builder, whose cache lies in
  the container's `/tmp` and is gone with it.

Steps:

1. **Pinned files.** One `actions/cache` per file, restored before and saved right after the fetch on a miss, as the
   `pinned-jdk` action keeps the GraalVM: a directory per file beside the GraalVM's, key `pinned-<name>-<sha256>`.
   `pinned_fetch` already keeps a file whose digest matches, so the fetch steps copy from that directory or fetch into
   it; a cached file that does not match its pin is fetched again, never used.
2. **The builder image.** Restored from `actions/cache` (key `pi-builder-<image digest>`) as an archive and loaded
   with `podman load` before the native build; on a miss `podman pull` by digest, then `podman save` and a save of the
   entry. The script keeps naming the image by digest, so podman refuses a loaded image whose digest differs.
3. **npm.** `build-pi-tree.sh` mounts a cache directory the workflow names (`PI_NPM_CACHE`) as npm's cache in the
   builder, and the workflow keeps it in `actions/cache` keyed by the hash of both lockfiles. `npm ci` still checks every
   package against the lockfile's integrity hash. Unset, the script keeps today's throwaway cache, so a local build is
   unchanged.
4. **Only the newest entry.** Every key carries the digest or the lockfile's hash, so a new pin writes a new entry. A
   last step on `main` deletes the entries of the same prefix whose key is not the one just used, with the job's token
   given `actions: write` for that step's job alone, unless the repository's Actions settings can expire entries after a
   set time (see *To be checked*).
5. The test that goes red first: `PinAgreementTest` runs `build-pi-tree.sh` with `PI_NPM_CACHE` set and a stubbed
   podman, and asserts the cache directory is mounted as npm's cache and stays outside the shipped tree; without the
   change, the mount is missing.

## Acceptance

- The second Build on GitHub on unchanged pins shows a cache hit for each of shfmt, Node, fd, ripgrep, the builder image
  and npm, and its log shows no request to github.com's release downloads, nodejs.org, docker.io or the npm registry.
- A cache entry changed on purpose (one byte of the fd archive) is refused by its digest and fetched again.
- After a pin moves, the cache holds one entry for that download, the new one.
- The packages a cached Build produces equal those of an uncached one, byte for byte.

## To be checked

- Whether the repository's Actions settings can delete cache entries after a set time, and how GitHub evicts entries
  that are not read; the result decides whether step 4 is needed.
- The size of the builder image archive and the npm cache against the repository's cache limit.
- Whether `podman load` of a saved image keeps the repository digest the script names, or needs the image ID checked.
