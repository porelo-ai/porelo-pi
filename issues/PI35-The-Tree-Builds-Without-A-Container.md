# PI35 — The Tree Builds Without A Container

**Status:** Review.
**Size:** S - `build-pi-tree.sh` runs its four builder steps either in the pinned image, as today, or
directly with the pinned Node; `porelo.yml` installs that Node; one refusal test. No change to what ships.

**What must be true.** The package's whole build, Pi's tree included, runs green inside a Porelo task, where no
nested container starts, and makes the same tree as the build that runs it in its pinned Node image.

From [PJ49](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj49), which names this as the
mending that comes before a tag: "pi's `npm ci` run with the pinned Node directly".

## Why

`buildtools/build-pi-tree.sh` builds Pi's tree with `npm ci`, the bill generator and the download of Node, fd and
ripgrep inside `podman run` of the pinned `node:22.23.3-slim` image. Measured on 2026-10-19 in an image built from this
repository's `porelo.yml`, as a task runs it (plain uid, default seccomp, `no-new-privileges`): unit, documents and the
shared checks are green, and `-Pnative,dist` stops there: "command required for rootless mode with multiple IDs:
exec: "newuidmap": executable file not found". A task has no subordinate ids for a container of its own.

## Measured

On 2026-10-19, pi at `7fc3d2b`, with the lockfile and the bill generator's lockfile as they stand:

- **(a)** the script's first step (`npm ci --omit=dev --ignore-scripts`, the generator's `npm ci`, `cyclonedx-npm`,
  the cleanup) run in `node:22.23.3-slim@sha256:c3de60bf…` with `--userns=keep-id`, as today;
- **(b)** the same lines run directly, under `env -i`, with Node `22.23.3` from `nodejs.org` checked against
  `pin.node.sha256`.

Both report Node `v22.23.3` and npm `10.9.9`. The trees:

- **15003 files identical** by SHA-256;
- **16442 entries** (files, directories and 8 symlinks with their targets and modes) **identical**, once (b) runs
  with `umask 022`. With the host's `002`, every mode differed (755/644 against 775/664) and nothing else. So the
  direct way sets `umask 022`, as the image's default is.
- **The bills** are equal but for `serialNumber` and `timestamp`, as between any two builds.

**Built, measured on the whole build, 2026-10-19:**

- **In the `porelo.yml` image as a task runs it** (plain uid, `no-new-privileges`, no podman in the image, the pinned
  Node from `nodejs.org`, `PI_TREE_DIRECT=1`): `-Pnative,dist verify` is green, and the tree is built directly.
- **The same commit on this machine through podman**, as the Build does: the tree from both is the same, **all
  14873 files byte for byte and all 16295 entries with their modes and links**.
- That last needed `umask 022` at the script's start: `pi/` and `node/` themselves were made with the caller's
  umask, so a host with `002` shipped them group-writable. The GitHub runner's is `022`, so the published package
  is unchanged.
- **`compare-bills`** of the direct build's package bill against the published one: "the same 126 components,
  unchanged licenses".
- `PinAgreementTest` gained three tests, seen red on the old script and `porelo.yml`: the task's image takes the
  pinned Node by version and digest, another one there is named, and `PI_TREE_DIRECT=1` with another `node` is
  refused before anything is built.

## Plan

- **`buildtools/build-pi-tree.sh`:** each of its four builder steps (the tree and its bill, Node, fd and ripgrep,
  the other platforms' binaries removed) becomes one `in_builder` call, its paths named by `$OUT` and not `/out`.
  `in_builder` runs it:
  - **in the pinned image**, as today, with `OUT=/out` and the tree mounted there, when podman is there and
    `PI_TREE_DIRECT` is not set;
  - **directly**, with `umask 022`, `OUT=$TARGET` and a fresh `HOME` under `target/`, when `PI_TREE_DIRECT=1`. It
    refuses then unless `node --version` is exactly `v$NODE_VERSION`, naming both. What the steps check (the
    lockfiles' integrity, Node's, fd's and ripgrep's digests) is checked the same way in both.
- **`porelo.yml`:** Node `22.23.3` from `nodejs.org` by `pin.node.sha256`, linked into `/usr/local/bin`; `env`
  `PI_TREE_DIRECT: "1"`. `podman` leaves the draft, since nothing needs it there then.
- **Red first:** `PI_TREE_DIRECT=1` with another `node` on `PATH` is refused before anything is built.
- **No changelog line:** the package is the same, byte for byte, as measured above.

## Acceptance

- In the `porelo.yml` image, as a task runs it (plain uid, `no-new-privileges`), `-Pnative,dist verify` is green,
  and its `pi-tree-sbom.cdx.json` gives "the same … components, unchanged licenses" by `compare-bills` against
  the published bill.
- On a machine with podman and without `PI_TREE_DIRECT`, the tree is built in the image as today.
- The whole VM suite green on the commit, then both legs.

## Measured after the push

- The Build of `ef8d0de` on `main` built the tree through podman as before, and both legs were green on it.
