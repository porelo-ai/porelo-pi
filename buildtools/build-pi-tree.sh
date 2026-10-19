#!/usr/bin/env bash
#
# Builds what the porelo-agent-pi package ships: the Pi coding agent with its dependencies, and
# the Node runtime it needs. Both end up inside the package, so a task image is built with no
# network access at all - see the 'packaged' section of pi.yaml.
#
# Everything runs in a pinned container rather than on the build machine, so no Node toolchain
# has to be installed to build Porelo, and the toolchain itself is a stated version rather than
# whatever the machine happens to have.
#
# Skipped, with a message, when there is no container runtime: the rest of Porelo still builds.
#
# Every container maps the build user in, so what it writes belongs to the build user. Without
# that, the tree is written as root and the next build cannot even delete it.
set -euo pipefail
# The modes the tree ships with are the build's, not those of whoever runs it: a host's 002 made pi/ and
# node/ group-writable in the package, measured on 2026-10-19.
umask 022

MODULE="$(cd "$(dirname "$0")/.." && pwd)"
# Porelo's release tool, on the classpath Maven passes as the only argument; it records Node below.
RELEASE_CLASSPATH="${1:-}"
TARGET="$MODULE/target/tree"
# The pins - the Node runtime, the builder image it is built in by digest, fd and ripgrep - come from
# pom.xml, which Maven passes in; this script keeps no default of its own, so nothing here can drift
# from what the release tool moves. Run it from Maven.
for pin in NODE_VERSION NODE_SHA256 NODE_IMAGE_DIGEST FD_VERSION FD_SHA256 RG_VERSION RG_SHA256; do
    [ -n "${!pin:-}" ] || {
        echo "build-pi-tree: $pin is not set - run this from Maven, which passes the pins from pom.xml" >&2
        exit 2
    }
done
BUILDER="${PI_BUILDER_IMAGE:-docker.io/library/node:${NODE_VERSION}-slim@${NODE_IMAGE_DIGEST}}"

# Each of these can be overridden from the environment, and every one of them ends up in a command that
# runs inside a build container with the shipped tree mounted writable. Checked against a strict
# shape here, so a value carrying shell syntax is refused rather than executed - and a mistyped
# version fails now rather than as a confusing error inside the container.
refuse() {
    echo "build-pi-tree: $1" >&2
    exit 2
}
[[ "$NODE_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] ||
    refuse "NODE_VERSION='$NODE_VERSION' is not a version"
[[ "$NODE_SHA256" =~ ^[0-9a-f]{64}$ ]] ||
    refuse "NODE_SHA256 is not a 64-character lowercase digest"
[[ "$NODE_IMAGE_DIGEST" =~ ^sha256:[0-9a-f]{64}$ ]] ||
    refuse "NODE_IMAGE_DIGEST is not sha256: and a 64-character lowercase digest"
for v in FD_VERSION RG_VERSION; do
    [[ "${!v}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || refuse "$v='${!v}' is not a version"
done
for d in FD_SHA256 RG_SHA256; do
    [[ "${!d}" =~ ^[0-9a-f]{64}$ ]] || refuse "$d is not a 64-character lowercase digest"
done
# Maven passes the JVM it runs on; anything else from the environment must still be one binary.
JAVA_CMD="${JAVA_CMD:-$(command -v java || true)}"
[[ "$JAVA_CMD" == /* && -x "$JAVA_CMD" ]] ||
    refuse "JAVA_CMD='$JAVA_CMD' is not an absolute path to an executable"
# A digest, not a tag alone: npm ci and every checksum run inside this image, and a tag can be repointed.
[[ "$BUILDER" =~ ^[A-Za-z0-9][A-Za-z0-9._/-]*(:[A-Za-z0-9._-]+)?@sha256:[0-9a-f]{64}$ ]] ||
    refuse "PI_BUILDER_IMAGE must be pinned by an @sha256: digest - '$BUILDER' is not"

if [ "${PI_TREE_DIRECT:-}" != 1 ] && ! command -v podman >/dev/null 2>&1; then
    echo "build-pi-tree: no podman, so the Pi tree is not built; porelo-agent-pi will be incomplete"
    exit 0
fi

# Each builder step runs in the pinned image, as it always has, or - with PI_TREE_DIRECT=1, where no
# container can start, as in a Porelo task - directly, with the very Node that image holds. Measured on
# 2026-10-19: both ways make the same tree, all 15003 files byte for byte and every mode and link,
# once the direct way takes the image's umask. A step names its paths by $OUT and $WORK and gets what
# it needs as named values, never as text pasted into it.
if [ "${PI_TREE_DIRECT:-}" = 1 ]; then
    [ "$(node --version 2>/dev/null)" = "v$NODE_VERSION" ] ||
        refuse "PI_TREE_DIRECT=1 builds with the pinned Node v$NODE_VERSION, but 'node' here is '$(node --version 2>/dev/null || echo none)'"
fi
in_builder() {
    local values=()
    while [ "$1" != -- ]; do
        values+=("$1")
        shift
    done
    shift
    if [ "${PI_TREE_DIRECT:-}" = 1 ]; then
        rm -rf "$TARGET/.home" "$TARGET/.work"
        mkdir -p "$TARGET/.home" "$TARGET/.work"
        (env -i PATH="$PATH" HOME="$TARGET/.home" OUT="$TARGET" WORK="$TARGET/.work" \
            "${values[@]}" sh -c "$1")
        rm -rf "$TARGET/.home" "$TARGET/.work"
    else
        local envs=()
        for value in "${values[@]}"; do
            envs+=(--env "$value")
        done
        # HOME too: the build user is the runner's uid, which the image does not know, so podman would
        # give it / and npm could not write its cache. /tmp is outside the tree, as the direct way's is.
        podman run --rm --userns=keep-id --env HOME=/tmp --env OUT=/out --env WORK=/tmp "${envs[@]}" \
            -v "$TARGET:/out:z" "$BUILDER" sh -c "$1"
    fi
}

rm -rf "$TARGET"
mkdir -p "$TARGET/pi" "$TARGET/node"
cp "$MODULE/src/main/npm/package.json" "$MODULE/src/main/npm/package-lock.json" "$TARGET/pi/"

# The bill generator, carried in with its own lockfile rather than resolved from the registry while
# the tree that is about to be shipped sits mounted beside it. Removed again below, so it is a
# build tool and not part of the package.
mkdir -p "$TARGET/sbom-tool"
cp "$MODULE/buildtools/sbom/package.json" "$MODULE/buildtools/sbom/package-lock.json" \
    "$TARGET/sbom-tool/"

# npm ci, not npm install: it installs exactly the lockfile, refuses to update it, and checks
# every package against the integrity hash recorded there. That is what makes this reproducible
# rather than "whatever the registry served today".
# --ignore-scripts: lifecycle scripts run after the integrity check, with the shipped tree writable, and
# Pi needs none. Of the three packages that declare one, @google/genai's preinstall is a no-op,
# protobufjs's postinstall prints a warning, and esbuild's postinstall copies a binary its JS API
# finds without it.
in_builder -- "
    set -eu
    cd \"\$OUT/pi\"
    npm ci --omit=dev --ignore-scripts --no-audit --no-fund

    # The bill, generated here and nowhere else: this is the only moment the lockfile and the
    # installed tree exist together, and the next line deletes the lockfile. Scanning the
    # result later is not equivalent - measured, a scan of the unpacked tree finds 141 of the
    # 167 packages and no integrity hashes at all.
    # Installed from the lockfile beside it, so every byte of the generator is checked against an
    # integrity hash, and with scripts disabled: it runs with the shipped tree mounted writable,
    # and an install script is the shortest path from a registry account to that tree.
    (cd \"\$OUT/sbom-tool\" && npm ci --ignore-scripts --no-audit --no-fund)
    \"\$OUT/sbom-tool/node_modules/.bin/cyclonedx-npm\" \
        --omit dev --spec-version 1.6 --output-format JSON \
        --output-file \"\$OUT/pi/sbom.cdx.json\"

    # npm's cache is in HOME, outside the tree; once it fell back to the working directory, which
    # shipped 40 MB of downloaded tarballs to every installation. Never again.
    rm -rf .npm

    rm -f package.json package-lock.json
"

# Out again before anything is packaged: it was needed for one command and belongs to the build.
rm -rf "$TARGET/sbom-tool"

# The published runtime, checked against the digest nodejs.org publishes beside it. Fetched with
# node's own fetch rather than curl: the container runs as the build user, so it cannot install
# packages, and the builder image already has the one tool needed.
in_builder "NODE_VERSION=$NODE_VERSION" "NODE_SHA256=$NODE_SHA256" -- "
    set -eu
    node -e \"
        const { writeFileSync } = require('fs');
        const version = process.env.NODE_VERSION;
        fetch('https://nodejs.org/dist/v' + version + '/node-v' + version + '-linux-x64.tar.gz')
            .then(r => { if (!r.ok) { throw new Error('HTTP ' + r.status); } return r.arrayBuffer(); })
            .then(b => writeFileSync(process.env.WORK + '/node.tar.gz', Buffer.from(b)));
    \"
    echo \"\$NODE_SHA256  \$WORK/node.tar.gz\" | sha256sum -c -
    tar -xzf \"\$WORK/node.tar.gz\" -C \"\$OUT/node\" --strip-components=1
    # Only the runtime is used: Pi is started as 'node <cli.js>'. npm, npx and the headers are
    # build-time tools that would otherwise ride along in every installation.
    rm -rf \"\$OUT/node/include\" \"\$OUT/node/share\" \"\$OUT/node/lib/node_modules\"
    rm -f \"\$OUT/node/bin/npm\" \"\$OUT/node/bin/npx\" \"\$OUT/node/bin/corepack\"
"

# fd and rg, checked against the pinned digests, the binary and its licenses kept and nothing else.
# Fetched with node's fetch like the runtime above; it follows GitHub's redirect to its storage.
in_builder "FD_VERSION=$FD_VERSION" "FD_SHA256=$FD_SHA256" "RG_VERSION=$RG_VERSION" "RG_SHA256=$RG_SHA256" -- "
    set -eu
    node -e \"
        const { writeFileSync } = require('fs');
        const e = process.env;
        const get = (url, file) => fetch(url)
            .then(r => { if (!r.ok) { throw new Error(url + ': HTTP ' + r.status); } return r.arrayBuffer(); })
            .then(b => writeFileSync(file, Buffer.from(b)));
        Promise.all([
            get('https://github.com/sharkdp/fd/releases/download/v' + e.FD_VERSION + '/fd-v' + e.FD_VERSION
                + '-x86_64-unknown-linux-musl.tar.gz', e.WORK + '/fd.tar.gz'),
            get('https://github.com/BurntSushi/ripgrep/releases/download/' + e.RG_VERSION + '/ripgrep-'
                + e.RG_VERSION + '-x86_64-unknown-linux-musl.tar.gz', e.WORK + '/rg.tar.gz'),
        ]).catch(err => { console.error(err.message); process.exit(1); });
    \"
    echo \"\$FD_SHA256  \$WORK/fd.tar.gz\" | sha256sum -c -
    echo \"\$RG_SHA256  \$WORK/rg.tar.gz\" | sha256sum -c -
    cd \"\$WORK\"
    mkdir -p \"\$OUT/tools/bin\" \"\$OUT/tools/licenses/fd\" \"\$OUT/tools/licenses/ripgrep\" fd rg
    tar -xzf fd.tar.gz -C fd --strip-components=1
    tar -xzf rg.tar.gz -C rg --strip-components=1
    cp fd/fd rg/rg \"\$OUT/tools/bin/\"
    cp fd/LICENSE-APACHE fd/LICENSE-MIT \"\$OUT/tools/licenses/fd/\"
    cp rg/LICENSE-MIT rg/UNLICENSE rg/COPYING \"\$OUT/tools/licenses/ripgrep/\"
"

# Prebuilt binaries for platforms a Linux container will never run. Dead weight in a package
# that is already large, and confusing to anyone auditing what is shipped.
# shellcheck disable=SC2016 # $OUT is the step's own, expanded where it runs.
in_builder -- '
    set -eu
    find "$OUT/pi" -type d \( -name darwin -o -name win32 -o -name "darwin-*" -o -name "win32-*" \) \
        -prune -exec rm -rf {} + 2>/dev/null || true
    find "$OUT/pi" -type d -name examples -prune -exec rm -rf {} + 2>/dev/null || true
'

# Node is in the tree and is not an npm package, so nothing above can have listed it. Added
# with the digest this script already verifies, rather than a digest computed after the fact -
# what is recorded is what was checked.
[ -n "$RELEASE_CLASSPATH" ] ||
    refuse "no classpath for Porelo's release tool - run this from Maven, which passes it"
"$JAVA_CMD" -cp "$RELEASE_CLASSPATH" org.fuin.porelo.release.Main add-component \
    "$TARGET/pi/sbom.cdx.json" --name node --version "$NODE_VERSION" \
    --url "https://nodejs.org/dist/v$NODE_VERSION/node-v$NODE_VERSION-linux-x64.tar.gz" \
    --sha256 "$NODE_SHA256" --license MIT
"$JAVA_CMD" -cp "$RELEASE_CLASSPATH" org.fuin.porelo.release.Main add-component \
    "$TARGET/pi/sbom.cdx.json" --name fd --version "$FD_VERSION" \
    --url "https://github.com/sharkdp/fd/releases/download/v$FD_VERSION/fd-v$FD_VERSION-x86_64-unknown-linux-musl.tar.gz" \
    --sha256 "$FD_SHA256" --license "MIT OR Apache-2.0"
"$JAVA_CMD" -cp "$RELEASE_CLASSPATH" org.fuin.porelo.release.Main add-component \
    "$TARGET/pi/sbom.cdx.json" --name ripgrep --version "$RG_VERSION" \
    --url "https://github.com/BurntSushi/ripgrep/releases/download/$RG_VERSION/ripgrep-$RG_VERSION-x86_64-unknown-linux-musl.tar.gz" \
    --sha256 "$RG_SHA256" --license "Unlicense OR MIT"

cp "$TARGET/pi/sbom.cdx.json" "$MODULE/target/pi-tree-sbom.cdx.json"

# One tarball rather than three directories: one file for the packagers to carry, one file for an
# operator to check, and one unpack instead of copying tens of thousands of files per task.
tar -czf "$MODULE/target/pi-tree.tar.gz" -C "$TARGET" pi node tools

echo "build-pi-tree: pi $(du -sh "$TARGET/pi" | cut -f1), node $(du -sh "$TARGET/node" | cut -f1)," \
    "tools $(du -sh "$TARGET/tools" | cut -f1)," \
    "packaged $(du -h "$MODULE/target/pi-tree.tar.gz" | cut -f1)"
