# PI49 — The Tree Build Retries A Failed Download

**Status:** Idea.

**What must be true.** Pi's tree build (`buildtools/build-pi-tree.sh`) fetches the pinned Node runtime, fd and ripgrep
again when a server answers with a 5xx or the connection breaks, a few times with a growing wait, before it fails and
says what it asked and what it got. A short outage of a download host then no longer fails a Build that was otherwise
green, while a wrong digest or a 4xx still fails at once.
