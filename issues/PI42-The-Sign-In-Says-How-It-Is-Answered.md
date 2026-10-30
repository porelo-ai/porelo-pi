# PI42 — The Sign-In Says How It Is Answered

**Status:** Accepted; waits for a Porelo release whose agent API takes `login.flow`.
**Size:** XS - one key in `pi.yaml`, no contract another repository reads.

**What must be true.** A person signing in to Pi from a host is told whether to paste a code or to confirm a
device page, because the agent definition names its sign-in's flow, `login.flow: device-code`.

## Plan

- **The contract built against:** Porelo's agent API takes `login.flow` with the values `paste-code`, `device-code`
  and `loopback`, refuses a `loopback` flow on a remote account by naming the agent, and never sets a browser opener for
  a login on a host; the agent listing it gives the frontend names the flow.
- **`src/main/resources/agent/pi.yaml`** gains `login.flow: device-code`.
  Its sign-in is Porelo's own `vault authorize github-copilot`, GitHub's device flow.
- **`porelo.version`** moves to the release that has the key.
- No test: a wrong flow only shows the wrong hint, which reads wrong and is fixed.

## Acceptance

- The definition loads with the new Porelo release, and the agent listing names `device-code` for Pi.

## To be checked

- Whether `login.flow` belongs to a definition with `login.grants` only and no login of its own, or to the grant.
