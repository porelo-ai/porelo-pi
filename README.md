# porelo-pi

<img src="doc/images/crazy-chicken.svg" width="350" alt="Crazy chicken - just hatched">

> **Crazy chicken - just hatched.** Interfaces, formats and even the purpose may change any day. Use it only to try ideas, never to build on.

Runs [Pi](https://github.com/earendil-works/pi), the coding agent published as
`@earendil-works/pi-coding-agent` (not Oh My Pi), inside [Porelo](https://github.com/porelo-ai/porelo):
in a hardened container, reaching only its provider, with your credential kept on the machine and
its work waiting for your review.
It is an adapter, not Porelo: it describes the agent and shapes in Java only what the agent cannot
express as data, and it depends on Porelo's published agent API alone, never on Porelo's implementation.

Its documentation: **<https://porelo-ai.github.io/pi/>**.

## Install

Once Porelo's package repository is set up, as Porelo's
[getting started](https://github.com/porelo-ai/porelo/blob/main/doc/getting-started.md) describes:

```
sudo apt install porelo-agent-pi      # or: sudo dnf install porelo-agent-pi
```

How to sign in, start a task, what the task reaches and what to read when it does not work:
[doc/index.md](doc/index.md).

## More

- [build.md](build.md) - building the package, and moving the pinned Pi version.
- [CONTRIBUTING.md](CONTRIBUTING.md) - how to change it, and [SECURITY.md](SECURITY.md) - how to report a vulnerability.
- [doc/decisions.md](doc/decisions.md) - why it works the way it does.

## Licence

GNU General Public License v3.0 or later. See [LICENSE](LICENSE).
