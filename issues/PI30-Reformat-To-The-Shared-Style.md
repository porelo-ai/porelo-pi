# PI30 — Reformat To The Shared Style

**Status:** Backlog; blocked by porelo-project [PJ29](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj29)'s shared part.
**Size:** S - one commit of whitespace, line breaks and import order across 26 Java files, the plugin switched on in
`pom.xml`, the shared `.editorconfig` and `.git-blame-ignore-revs`, the YAML and `.feature` files at 4 spaces; no contract another
repository reads.

**What must be true.** The Pi adapter's code is in the shared style, the build refuses code that is not,
and `git blame` skips the one commit that reformatted it.

From [PJ29](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj29), one code style for the IDE and the build, whose shared part is `porelo-parent`'s Spotless with
`google-java-format` in its AOSP style.

## Plan

- **Needs first:** [PJ29](https://github.com/porelo-ai/porelo-project/blob/main/issues/README.md#pj29)'s shared part in `porelo-parent` `0.2.1-SNAPSHOT`, installed locally by Agent Core until it is pushed; this repository
  already builds on.
- **The shared `.editorconfig`** at the root, the same content as everywhere, compared by `check-shared`.
- **The other files, at the shared `.editorconfig`'s 4 spaces:** the 9 YAML files (the workflows,
  `dependabot.yml`, `mkdocs.yml`, the manifest `src/main/resources/agent/pi.yaml`) and the 6 `.feature` files go from 2 to 4
  spaces. Each YAML file is checked to load to the same data. `mvnw` and `mvnw.cmd` stay as the wrapper
  generates them.
- **`buildtools/build-pi-tree.sh` and `buildtools/follow-upstream.sh`** are as `shfmt` writes them, which
  the parent checks through Spotless.
- **`pom.xml`** switches on the parent's `spotless-maven-plugin`, so `spotless:check` runs at `validate`.
- **Red first:** `./mvnw validate` on today's tree fails, naming the files. The longer lines alone are
  181 lines over 100 columns.
- **One commit** from `spotless:apply`, with `google-java-format`'s defaults: it changes whitespace, line
  breaks (Javadoc rewrapped too) and the order of imports, nothing else. A second commit puts its hash into
  `.git-blame-ignore-revs`, since a commit cannot name its own hash.
- **Checked as any change to the adapter:** unit tests, the native build with its linkage check, the
  packages and the acceptance on the VM, on the exact commit.

## Acceptance

- `validate` fails on a file indented or wrapped against the style, naming it, and passes after
  `spotless:apply`.
- The reformatting commit changes only whitespace, line breaks (Javadoc rewrapped too) and the order of
  imports: each file is the same with all whitespace removed and its imports sorted. `git blame` with
  `.git-blame-ignore-revs` skips it.
- The whole workflow is green on the VM on the exact commit.
