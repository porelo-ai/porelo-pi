@credential
Feature: Porelo shows Pi waiting for a person while its ask dialog is open

    Pi has no tool of its own to ask a person, so the package gives it one, ask_person, which asks through a
    dialog titled "Pi asks you"; pi.yaml declares that title as waiting. This drives the pinned Pi to the dialog
    and asks Porelo, never the screen, what it makes of it - so a release that draws its dialog differently
    fails here instead of going quiet in the field. A question the model writes as text reads as not waiting.

    Background:
        Given the suite runs as an unprivileged user
        And the environment variable "PORELO_E2E_OPENROUTER_API_KEY" is set
        And the environment variable "PORELO_E2E_MODEL" is set

    @slow
    Scenario: an attached Pi is shown not waiting at its prompt, waiting once it asks, and not once answered
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds the value of "PORELO_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
        And a project called "asks" of class "guarded" with a file in it
        And a terminal on the machine
        When I run "porelo task start asks --project asks --repository asks --agent pi --provider openrouter --model ${PORELO_E2E_MODEL} --clearance deny"
        Then the "pi" agent in task "asks" of "asks" reaches work without being asked anything
        And porelo shows the "pi" agent in task "asks" of "asks" not waiting for a person
        When I type "Use your ask_person tool to ask me whether I prefer red or blue. Do nothing else."
        And I press Enter
        # The dialog on the screen first, so a run where it never comes shows what Pi showed; Porelo's reading of it
        # is asked after.
        Then within 120 seconds this script exits zero:
            """
            screen=$(podman exec --user root "$(porelo task status asks --json | grep -o '"name": *"[^"]*"' | head -n 1 | cut -d'"' -f4)" tmux -S /run/porelo-tmux/tmux capture-pane -p -t porelo)
            printf '%s\n' "$screen" | tail -n 12 | grep -qF 'Pi asks you' && exit 0
            printf 'screen, last lines:\n'; printf '%s\n' "$screen" | grep -v '^ *$' | tail -25
            exit 1
            """
        Then porelo shows the "pi" agent in task "asks" of "asks" waiting for a person
        When I type "red"
        And I press Enter
        # Not only the dialog gone, which it is the moment Enter is pressed, but the turn finished and Pi at rest.
        Then within 120 seconds this script exits zero:
            """
            screen=$(podman exec --user root "$(porelo task status asks --json | grep -o '"name": *"[^"]*"' | head -n 1 | cut -d'"' -f4)" tmux -S /run/porelo-tmux/tmux capture-pane -p -t porelo)
            printf '%s\n' "$screen" | grep -qF '(auto)' || exit 1
            ! printf '%s\n' "$screen" | grep -qF -e 'Working ─' -e 'escape/ctrl+c cancel'
            """
        And porelo shows the "pi" agent in task "asks" of "asks" not waiting for a person
        When a script runs "porelo project remove asks --force"
        Then it exits zero
