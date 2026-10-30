# PI42 — The Sign-In Says How It Is Answered

**Status:** Accepted.
**Size:** XS - the key in `pi.yaml` and the Porelo version in `pom.xml`, under an hour.

**What must be true.** A person granting Pi's GitHub Copilot sign-in from a host, at a terminal or in the GUI, is told
to confirm the code on GitHub's device page.

## Plan

- **The contract built against:** Porelo's agent API takes `login.flow` with the values `paste-code`, `device-code`
  and `loopback`, refuses a `loopback` flow on a remote account by naming the agent, and never sets a browser opener for
  a login on a host; the agent listing it gives the frontend names the flow.
- **`src/main/resources/agent/pi.yaml`** names the flow `device-code` as `login.flow`, on the definition or on
  its `github-copilot` grant, wherever Porelo's agent API places it: its sign-in is Porelo's own
  `vault authorize github-copilot`, GitHub's device flow.
- **`pom.xml`:** `porelo.version` moves to the Porelo version whose agent API takes the key.
- No test: a wrong value either shows the wrong hint, which only reads wrong, or, as `loopback`, is refused by name
  at once; neither does harm.

## Acceptance

- Seen to fail first: the agent listing for today's definition names no flow.
- With the change, the definition loads and the agent listing names `device-code`.

## To be checked

- Whether `login.flow` belongs to a definition with `login.grants` only and no login of its own, or to the grant.
