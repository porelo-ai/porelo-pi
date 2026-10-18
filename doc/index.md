# Pi for Porelo

Runs [Pi](https://github.com/earendil-works/pi), the coding agent published as
`@earendil-works/pi-coding-agent` (not Oh My Pi), inside [Porelo](https://github.com/porelo-ai/porelo):
in a hardened container, reaching only its provider, with your credential kept on the machine and
its work waiting for your review.

## Install

Set Porelo's package repository up once, as Porelo's
[getting started](https://github.com/porelo-ai/porelo/blob/main/doc/getting-started.md) describes,
then:

```
sudo apt install porelo-agent-pi      # or: sudo dnf install porelo-agent-pi
```

Setting a machine up from Porelo's interface offers it too. `porelo agents` lists it at once; nothing
has to be registered. Unlike a single-binary agent, the package carries Pi itself: its npm tree, a
Node runtime, `fd` and `ripgrep`, about 79 MB, copied into the task's image under `/opt` and run as
`/usr/local/bin/pi`, so the image build downloads nothing.

## Sign in

With an **OpenRouter API key**:

```
porelo vault put openrouter --type api-key
```

It asks for the key without showing it.

With **GitHub Copilot**, granted once through GitHub's device flow:

```
porelo vault authorize github-copilot
```

Open the link it shows in any browser and enter the code. **Whose app you grant:** the OAuth app Pi itself
signs in with, `Iv1.b507a08c87ecfe98`, as its source holds it. Pi's source does not name the app's owner;
it is widely cited as GitHub's own Copilot app for editors, and Pi presents itself to GitHub as that
editor (`Editor-Version: vscode/…`, `Copilot-Integration-Id: vscode-chat`). Neither is verified. A grant is
withdrawn on GitHub under Settings → Applications → Authorized OAuth Apps. Porelo keeps the token on the machine; Pi's
own `/login` is never run. Start the task with `--provider github-copilot` and a model Pi's catalogue
lists for Copilot, such as `--model gpt-5-mini`; a model it does not list is answered with `400`.
These two are the providers measured with Pi.

## Start a task

```
porelo project default add <address>
porelo task start <name> -p default -r <repository> --agent pi
```

No project file is needed. The address is where the repository is cloned from and where its approved
work goes; `porelo project default list` shows the name `-r` takes. Started inside a checkout,
`porelo task start` adds that checkout's repository by itself. A project of your own, with a
`project.yml`, is for settings beyond that.

## What the task holds and reaches

- **Not your credential.** The container gets a token that works for this task only, and Porelo's
  broker swaps the real credential in on the way to the provider.
- **Reachable:** the provider only, `openrouter.ai`, or `api.githubcopilot.com` with
  `--provider github-copilot`. Pi needs nothing else to start. Nothing else resolves.
- **Not reached:** Pi's version check and its telemetry are switched off, and `pi.dev`, where both
  go, is refused. `fd` and `ripgrep`, which Pi would otherwise download from GitHub, ship in the
  image.

`porelo agents --verbose` shows all of it for the installed version.

## When it does not work

The task's broker logs every request in `vault.log`, in `/run/user/<uid>/porelo/<container>/`:

| What it says | What it means |
|---|---|
| `401 from the provider` | the request reached the provider, which refused the credential: wrong or out of credit |
| `401 token not accepted` | a request came without this task's token |
| `401 this task's token expired at …` | the task outlived `--token-hours` |
| `503` | the vault was locked or the entry removed: `porelo vault unlock` |
| no `request` line at all | Pi never reached the broker: see `relay.log` in the same directory |

Why it works the way it does is in [the decisions](decisions.md).
