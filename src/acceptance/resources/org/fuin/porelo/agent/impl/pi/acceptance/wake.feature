@credential
Feature: Pi is woken at rest, and never while its ask dialog is open

    Porelo types a wake line - for a message, a handed-in file or a build verdict - only into an agent that
    declares both at_rest and waiting, and only while at_rest matches its screen. Pi's waiting is the dialog of
    the ask tool this package gives it, and its at_rest lacks that dialog's title, so a message that arrives
    while Pi asks is announced only once the question is answered and Pi rests at its prompt. A line typed into
    the dialog would have become the answer. This scenario leaves a task running while it waits, so it is in a
    file of its own that runs after every other. Without PORELO_E2E_OPENROUTER_API_KEY and PORELO_E2E_MODEL it
    is skipped.

    Background:
        Given the suite runs as an unprivileged user
        And the environment variable "PORELO_E2E_OPENROUTER_API_KEY" is set
        And the environment variable "PORELO_E2E_MODEL" is set

    @slow
    Scenario: a message that arrives while Pi asks types nothing into the dialog, and wakes Pi once it is answered
        Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
        And the vault holds the value of "PORELO_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
        # A wake comes from the daemon's passes, which a leased machine's account does not run by itself.
        And a daemon of this scenario's own
        And a project called "wake" of class "guarded" with a file in it
        And a terminal on the machine
        When I run "porelo task start wake --project wake --repository wake --agent pi --provider openrouter --model ${PORELO_E2E_MODEL} --clearance deny"
        Then the "pi" agent in task "wake" of "wake" reaches work without being asked anything
        When I type "Use your ask_person tool to ask me whether I prefer red or blue. Do nothing else."
        And I press Enter
        Then within 120 seconds this script exits zero:
            """
            podman exec "$(porelo task status wake --json | grep -o '"name": *"[^"]*"' | head -n 1 | cut -d'"' -f4)" tmux capture-pane -p -t porelo | tail -n 12 | grep -qF 'Pi asks you'
            """
        When a script runs "echo 'What is 1234 plus 4321? Reply with the digits only.' | porelo talk tell wake"
        Then it exits zero
        Then within 60 seconds this script exits zero:
            """
            podman exec "$(porelo task status wake --json | grep -o '"name": *"[^"]*"' | head -n 1 | cut -d'"' -f4)" sh -c 'find /run/porelo/mail/inbox/new /run/porelo/mail/inbox/cur -type f | grep -q .'
            """
        # The daemon's message watcher passes every 5 seconds while idle: three of its passes go by with the dialog
        # open before the screen is read.
        When a script runs "sleep 15"
        Then the terminal does not show "A message for you waits"
        When I type "red"
        And I press Enter
        # Answered, Pi finishes its turn and rests at its prompt: the message is announced then.
        Then within 180 seconds the terminal shows "A message for you waits"
        When a script runs "porelo task remove wake --force"
        And a script runs "porelo project remove wake --force"
        Then it exits zero
