# PI43 — Mutation Testing With PIT

**Status:** Review.
**Size:** S - one profile in `pom.xml` and a report in this issue, no contract.

**What must be true.** A person can run mutation testing over this repository by hand and read which changes to the
code no test notices, sorted into real gaps and harmless ones.

- A `pit` profile, run by hand: not in CI and with no threshold.
- PIT and its JUnit 5 plugin are pinned.
- It runs on a VM where tests call real tools.
- A report sorts the survivors into real gaps and harmless ones.

## Plan

- `pom.xml`: a profile `pit` with `org.pitest:pitest-maven` 1.30.2 and, as its plugin dependency,
  `org.pitest:pitest-junit5-plugin` 1.2.3. Configuration: `targetClasses` from a property `pit.classes`, by default
  `org.fuin.porelo.agent.impl.pi.*`, so one class can be named by `-Dpit.classes=`; `targetTests`
  `org.fuin.porelo.agent.impl.pi.*Test` (the 25 unit test classes; the `*Check` classes need a built package and
  stay out); `excludedClasses` the package's `package-info`; `excludedMethods` `main`; `threads` 4;
  `timeoutConstant` 10000; `timestampedReports` false; output HTML and XML into `target/pit-reports`. No execution is
  bound to a phase, so `verify` and CI never run it.
- Run by hand: `./mvnw -B -s settings.xml -Ppit test-compile org.pitest:pitest-maven:mutationCoverage`.
- A section `## Survivors` in this issue: each surviving mutant by class and line, sorted into real gaps (a change
  that can do harm and no test sees) and harmless ones (with one line why). Gaps are recorded, not fixed here; a
  grave one becomes an issue of its own.

## Acceptance

- The command above ends with exit 0 and writes `target/pit-reports/mutations.xml`; `./mvnw -B -s settings.xml
  verify` runs no PIT goal.
- `## Survivors` names every survivor the report lists, each sorted.

**The model.** The `pit` profile in the messaging repository's `pom.xml`.

## Survivors

The run by hand over the seven classes of `org.fuin.porelo.agent.impl.pi`, with the 25 unit test classes: 171
mutants, 165 killed, 3 survived, 3 without coverage; ended with exit 0 in under a minute. It ran in a task
container, not on a VM; the tests that start real processes ran there, and `verify` there skipped none of its
201 tests.

**Real gaps** - a change that can do harm and no test sees:

- `PiFindings.isValid`, line 157, the `env` of an `api_key` entry taken as a map: negated, survived. Made to read
  every `env` map as invalid by hand, `PiFindingsTest` and `PiCredentialExtractorTest` still pass. No test holds
  an entry whose `env` is a map of strings beside a good entry, so a file Pi accepts could read as one it refuses:
  every provider would read as "cannot say" and the import would stop. It fails closed, nothing leaks; recorded
  here, not grave.

**Harmless ones:**

- `PiCredentialExtractor.extract`, line 61, the filter on the finding's file replaced by `true`: survived, and
  cannot be killed today. Only `auth.json` makes a finding importable (a key in `models.json` is found with
  `stored` false), so the filter on importable already drops every other file; this one is a second guard.
- `PiJsonFormatter.cut`, line 238, `> MAX` as `>= MAX`: survived. A line of exactly 300 code points would show
  the cut mark after all of it; only the output reads differently.
- `PiJsonFormatter.shape`, line 79, `message`, lines 105 and 116, each return replaced by `""`: no coverage. A
  line that parses to JSON but not an object, a `message_end` without a message object, and an assistant message
  without a content list would show an empty line instead of the line marked unreadable; what is shown still
  passes the control-character filter, so only the output reads differently.
