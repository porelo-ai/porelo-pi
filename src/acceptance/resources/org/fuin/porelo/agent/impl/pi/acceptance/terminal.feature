Feature: What a person sees with this agent, before there is any credential

    Every scenario here needs no secret and no model. They are the things somebody meets in the
    first ten minutes with this agent on a machine, and each of them is a refusal that has to say
    what it is rather than fail later.

    Background:
        Given the suite runs as an unprivileged user

    Scenario: an agent that does not say how to log in is answered, not guessed at
        # Porelo does not know how any agent logs in; the verb comes from the agent's own manifest,
        # and this one declares none. Hardcoding one agent's verb would be wrong for every other.
        When logging in to the "pi" agent is attempted
        Then it exits non-zero

    Scenario: starting a task without a credential says what is missing, before anything is built
        Given a project called "nocred" of class "guarded" with a file in it
        And a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds no credential for "openrouter"
        When a task nobody is watching is started in "nocred" for the "pi" agent
        Then its output mentions one of "no credential, is locked, cannot authenticate"
        And its output contains "openrouter"

    Scenario: a script is never asked a question by this agent's task
        Given a project called "noask" of class "guarded" with a file in it
        When a script asks what a task in "noask" for the "pi" agent would do
        Then its output does not contain "[Y/n]"
        And its output contains no escape sequences

    Scenario: nothing in a task can store a sign-in where Pi reads one
        # Pi has no switch to turn /login off, and a key in its auth.json wins over the broker's. Its store writes the
        # file in place, so a write as the agent is what /login would do. Pi reads the directory its wrapper names, or
        # its own default ~/.pi/agent; under the home the agent can move even a root-owned one aside and write its own.
        # A key that opens nothing is enough: no model is asked.
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        When a script runs "printf sk-or-opens-nothing | porelo vault put openrouter --type api-key"
        Then it exits zero
        Given a project called "nologin" of class "guarded" with a file in it
        When a task is started in "nologin" for the "pi" agent and left running
        And the task's container runs:
            """
            dir=$(sed -n 's/^export PI_CODING_AGENT_DIR=\(.*\)$/\1/p' /usr/local/bin/pi); dir=${dir:-$HOME/.pi/agent}
            echo "pi reads $dir"
            case "$dir" in "$HOME"/*) echo "UNDER THE HOME";; esac
            for f in auth.json mcp-auth.json; do
                chmod u+w "$dir/$f" 2>/dev/null && echo "MADE WRITABLE $f"
                (printf '{"openrouter":{"type":"api_key","key":"planted"}}' > "$dir/$f") 2>/dev/null && echo "WROTE $f"
                (printf '{}' > "$dir/$f.new" && mv -f "$dir/$f.new" "$dir/$f") 2>/dev/null && echo "REPLACED $f"
                rm -f "$dir/$f.new"
                rm -f "$dir/$f" 2>/dev/null && echo "REMOVED $f"
            done
            # Owning the parent would let the agent move the directory aside and make its own, as under the home.
            mv "$dir" "$dir.moved" 2>/dev/null && echo "MOVED THE DIRECTORY"
            (printf '{}' > "$(dirname "$dir")/write-try") 2>/dev/null && { echo "WROTE IN THE PARENT"; rm -f "$(dirname "$dir")/write-try"; }
            printf 'stores: %s %s\n' "$(cat "$dir/auth.json")" "$(cat "$dir/mcp-auth.json")"
            echo "every write tried"
            """
        Then its output contains "every write tried"
        And its output contains "pi reads /var/lib/pi/agent"
        And its output contains "stores: {} {}"
        And its output does not contain "UNDER THE HOME"
        And its output does not contain "WROTE"
        And its output does not contain "REPLACED"
        And its output does not contain "REMOVED"
        And its output does not contain "MADE WRITABLE"
        And its output does not contain "MOVED"
        When a script runs "porelo project unfollow nologin --force"
        Then it exits zero
