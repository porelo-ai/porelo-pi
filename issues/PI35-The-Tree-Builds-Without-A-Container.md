# PI35 — The Tree Builds Without A Container

**Status:** Idea.

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

## What is to be found out

- **Running the script's container steps directly** when the pinned Node (`pin.node.version`, checked against
  `pin.node.sha256`) is the `node` on `PATH`, and through podman otherwise, so a developer's build and the Build stay
  as they are. `porelo.yml` would install that Node from `nodejs.org` by the same pin.
- **Whether the two ways make the same tree**: the tree's files and `pi-tree-sbom.cdx.json` compared byte for byte,
  and `compare-bills` against the published bill.
- **The cleanup steps** that run as root in the container today (removing other platforms' prebuilt binaries), as the
  task's own user.
- What is left that still needs a container carries `needs-nested-container`, as [PJ49](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj49) shapes it.
