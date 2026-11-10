# PI49 — The Tree Build Retries A Failed Download

```json
{
  "$schema": "issue-1.json",
  "id": "PI49",
  "title": "The Tree Build Retries A Failed Download",
  "state": "Review",
  "size": "S",
  "priority": "Medium",
  "rank": 3,
  "blockedBy": [],
  "covers": "the tree build fetches Node, fd and ripgrep again on a 5xx or a broken connection, a few times, before it fails"
}
```

**Why this size.** three fetches moved onto porelo-buildtools' `pinned.sh`, with the workflows' own copies of the action.

**What must be true.** Pi's tree build (`buildtools/build-pi-tree.sh`) fetches the pinned Node runtime, fd and ripgrep
again when a server answers with a 5xx or the connection breaks, a few times with a growing wait, before it fails and
says what it asked and what it got. A short outage of a download host then no longer fails a Build that was otherwise
green, while a wrong digest or a 4xx still fails at once.

## Plan

- `build-pi-tree.sh` sources `.github/actions/pinned-jdk/pinned.sh`, copied unchanged from porelo-buildtools,
  and fetches the three files on the build machine with `pinned_fetch <url> <sha256> <file>` into
  `target/pi-downloads`, where a file that matches its pin is kept.
  - The rule is porelo-buildtools': pauses of 15, 30, 60, 120, 240, 300 and 135 s, tried again only on a 5xx or a
    connection that failed, timed out or broke off, HTTPS only.
- The builder no longer fetches anything. It checks each file against its pin again, from `$OUT/.downloads`, and
  unpacks it. `.downloads` is out of the tree before anything is packaged.
- The workflows' shfmt and Node fetches take `pinned_fetch` too, and the `pinned-jdk` action takes porelo-buildtools'
  steps: the GraalVM kept by its digest in `actions/cache`, and the wrapper's Maven run again by `pinned_retry`. Every
  job that uses the action has 15 minutes more, so the rule can finish its pauses.

## Acceptance

- `PinAgreementTest` builds the tree with a curl that answers 404, and the build stops at the first download, after
  the first podman call, without reaching the network.
- A native build fetches the three files, and the tree it packages holds no `.downloads`.

## Measured

- The regression matrix of `doc/decisions.md`, run on the change with podman and curl stubbed: each of its inputs and
  each unset pin exits 2 with no podman call, no curl call and no tree begun. The control run with the pom's pins
  reaches podman, then stops at the first download, whose stubbed 404 fails at once.
- `-Pnative,dist verify` with fd and ripgrep already in `target/pi-downloads`: both were taken from there, checked
  against their pins, Node was fetched; green, and neither package holds `.downloads`.
- Not built in: shfmt and the tree's three files are not kept in `actions/cache`, as the GraalVM is. The rule is what a
  short outage needed; a cache for them can follow on its own.

## To be checked

- The first Build on GitHub after the move: the GraalVM cache misses, the second Build hits.
