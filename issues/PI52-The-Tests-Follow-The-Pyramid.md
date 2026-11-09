# PI52 — The Tests Follow The Pyramid

**Status:** Accepted.
**Size:** M - two scenario files, the status extension's tests moved into the unit phase with a JavaScript engine
of their own, and one test class; no contract.

**What must be true.** Every flow this adapter owns is proven by unit tests that run in seconds, the status
extension's account of an open prompt included. The end-to-end legs show only what the real Pi and the installed
package alone can show, and no test waits longer than the thing it checks needs.

## The suites by level

Measured with a clean `./mvnw -o -B -s settings.xml clean test` on the host under a load of about 40 on 16 cores:
34 s in all, compilation included, 201 tests green and none skipped. The end-to-end times are those of the last
green Build on GitHub (run 42481960914). A class that starts a process counts as integration as a whole.

| Level | Tests | Run time | Share by count | What it holds |
|---|---|---|---|---|
| Unit | 134 | 2.8 s | 56 % | `PiFindingsTest` 46, `PiJsonFormatterTest` 15, `PiAgentTest` 14, `PiCredentialExtractorTest` 13, `PiContainerSetupTest` 10, the declaration tests (session id, waiting, run end) 10, and the document and pin rules 26 |
| Integration | 67 + 12 | 10.5 s, plus the checks inside the 2.5 min Build job | 33 % | the build script with stand-in `podman` and `curl` (`PinAgreementTest` 16), the workflow-step tests 49 (bash with stand-in tools, real git), `ProcessesTest` 2, and the `dist` checks of the built tree and packages (`BuiltTreeCheck` 6, `PackageVersionCheck` 4, `NativeLinkageCheck` 2) |
| End-to-end | 25 scenarios | 13.6 min (fedora) and 11.3 min (ubuntu), one rented machine each | 11 % | `credential` 9, `installed` 8, `terminal` 4, `session` 2, `waiting` 1, `wake` 1 |

The unit share is below the pyramid's 70-80 % because the Build's own workflow steps and the tree's build script are
bash and are proven by running them with stand-in tools, which is their lowest level.

## Flows proven only above the unit level

- **The status extension's account of an open prompt** (`PiStatusExtension`): that nested prompts are a stack, that
  the file names the innermost one still open, and that closing the last returns the state to what Pi was doing, is
  proven only by `BuiltTreeCheck`, which runs the extension under the Node of the built tree in the `dist` build. The
  unit suite checks only that the file is written. This is the one flow of the adapter's own logic above the unit
  level.
- **The tree's build script and the workflow steps** (publish and snapshot tokens, the update request, release
  channels, following upstream, commits a workflow makes): bash run with stand-in tools. They stay integration: the
  script is the real piece, and its tests take seconds.
- **No sign-in stored where Pi reads one** (`terminal.feature`, *nothing in a task can store a sign-in where Pi reads
  one*): that the image's `pi` wrapper names a directory outside the home, and that the layer leaves `auth.json` and
  `mcp-auth.json` there root's and read-only in a sticky directory, is proven only end-to-end. No unit test reads
  those lines of the image layer in `pi.yaml`, so a change that moves the directory under the home fails only on a
  rented machine. A key in `auth.json` wins over the broker, so this is a guard against a credential bypass.
- **End-to-end only:** no other flow of this adapter. Every other scenario's adapter fact - waiting that Porelo
  cannot read, the screen at rest, the session id, the routing to the broker, the run's end, the credential kinds -
  has a unit test over the recorded output or the written file. What the scenarios add is that the pinned Pi honors
  those facts, which only the real CLI can show.
- **Porelo's own flows in this suite:** five scenarios test Porelo and not this adapter: in `credential.feature`
  *a person unlocks the vault and the passphrase is not echoed*, *a person stores the key, and it is echoed
  nowhere* and *the credential is not recoverable from the vault file*; in `installed.feature` *the hooks register
  for this user* and *doctor says what this machine can do*. Each runs on both legs.

## Waits, sleeps and the network

- `ProcessesTest` runs `sleep 120` under a limit of 2 s and so waits those two seconds each run; a limit of 100 ms
  shows the same kill.
- No test reaches the network: the build script's `curl` and every download in a workflow step are stand-ins, and
  no test opens a socket or a URL (searched for `HttpClient`, `openConnection`, `openStream`, `new URL(`,
  `InetAddress` and `ServerSocket`).

## Plan

- **The status extension is proven in the unit phase** (S): its two tests move from `BuiltTreeCheck` into a unit
  test that runs `PiStatusExtension.document()` in a JavaScript engine on the test classpath, with `node:fs` and
  `node:path` as stand-ins that record the file written. Seen red first by a stack that returns the outer prompt
  wrongly. `BuiltTreeCheck` keeps one run under the real Node, that the extension loads there.
- **The sign-in store is guarded in the unit phase** (XS): a unit test reads the definition's image layer and holds
  that the wrapper's `PI_CODING_AGENT_DIR` lies outside `/home`, that its directory is made sticky, and that both
  stores are written there read-only before the agent runs. Seen red first with the directory moved under the home.
  The end-to-end scenario stays: only the real image shows that Pi then reads nothing the agent wrote.
- **Porelo's flows leave this suite** (S): the five scenarios above are deleted from `credential.feature` and
  `installed.feature` once Porelo's own suite is confirmed to cover each; one it does not is handed to Porelo's
  agent first. The end-to-end count falls from 25 to 20, on both legs.
- **The timeout test waits 100 ms** (XS): `ProcessesTest` with a limit of 100 ms, seen red first by a limit the
  sleep does not reach.

## Acceptance

- The unit suite stays under a minute, runs the status extension's prompt stack without the built tree, and fails
  when Pi's directory moves under the home.
- The end-to-end legs hold 20 scenarios, none of which tests only Porelo.
- `ProcessesTest` takes under 0.5 s.

## To be checked

- Which JavaScript engine runs an ES module with stand-in `node:` imports on the test classpath, pinned like every
  other dependency; measured in a throwaway project first.
- Whether Porelo's own acceptance suite proves the passphrase and the stored value are echoed nowhere, the vault file
  does not hold the value, `porelo setup` registers the hooks and `porelo doctor` reports them; asked of Porelo's agent.
