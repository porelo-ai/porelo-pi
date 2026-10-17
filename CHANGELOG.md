# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Headings name this package's version; Pi's version is what the package ships and appears as an
entry rather than a heading. One sentence per change - `git log` has the detail.

## [Unreleased]

### Security

- The log formatter drops terminal control characters but newline and tab from what it shows and from how a run ended, so an escape sequence in Pi's output, a provider's error or a tool's name no longer reaches the terminal.
- No checkout leaves the job's token in `.git/config`, where the build, the bill check and both acceptance legs could read it; the update job's push alone gets the token, through git's environment in that one step.

### Changed

- The log formatter cuts a long line after 300 characters counted as code points, so an emoji at the cut is no longer split in half.
- The update job neither calls its pull request green nor merges it when a step before ended in anything but success, a step killed by its limit or a cancel included.
- A release upload stops when the Debian index cannot be read or Artifactory cannot say whether the `.rpm` is there, and the `.rpm` is checked as well as the `.deb`.
- A publish is believed only once the Debian index lists the exact version built, not merely the package's name.
- Every job that validates installs shfmt 3.12.0, checked against its release's digest, since Spotless checks the shell scripts with it.
- The update job fails when deleting its rented machines fails, instead of going on as if they were gone.
- Built on the snapshot of `porelo-parent` (0.2.1-SNAPSHOT, with the release tooling 0.5.1-SNAPSHOT), against Porelo 0.5.0.

## [0.5.0] - 2026-10-17

### Changed

- The adapter is renamed with the product it serves: it is `org.fuin.porelo:porelo-agent-pi`, its packages `porelo-agent-pi` depend on `porelo`, provide `porelo-agent` and install into `/usr/libexec/porelo/agents`, and its repository is `porelo-ai/porelo-pi`.
- Every workflow's token reads only at its top; only the update job may push the pin's branch and open its pull request, and only the job that deletes old runs may delete them.
- A tag's build takes nothing from Central's snapshots and asks for nothing again, and a tag on a Porelo snapshot is seen refused by the channel step itself, run in a test.
- The wake scenario reads the model's answer rather than the typed line, waits for Pi at rest and for the message and file to arrive instead of fixed pauses, and watches 15 seconds rather than 60 for a wake line.
- Built against Porelo 0.5.0 and on `porelo-parent` 0.2.0, which brings the release tooling 0.5.0.
- Every workflow run is titled by its workflow, the branch or tag and the commit; the shared rules run as `Shared rules check`, the weekly pin move as `Agent version update`.

## [0.4.1] - 2026-10-08

### Added

- The build reads the version from the `.deb` and `.rpm` it made and fails unless it is the project's, as the packages map it.

### Changed

- Built against Sokar 0.4.1 and on `sokar-parent` 0.1.2, which brings the release tooling 0.4.3.
- NullAway's compiler configuration and the plugin versions come from `sokar-parent`; `exec-maven-plugin` moves to 3.6.3.
- The build takes `org.fuin.sokar:sokar-parent` as its parent instead of `org.fuin:pom`; the packages are unchanged.
- The `shared-rules` workflow also runs `sokar-release check-readmes`.
- A release tag is refused while anything the build resolves is a snapshot, checked by `sokar-release` 0.4.2.
- A push that changes only documents - Markdown, `mkdocs.yml`, `doc/` or `issues/` - no longer starts the build.
- The release tooling is `sokar-release` 0.4.1, whose shared checks of issue citations and the documentation chapter replace this repository's own tests.

## [0.4.0] - 2026-10-05

- Initial public version.
