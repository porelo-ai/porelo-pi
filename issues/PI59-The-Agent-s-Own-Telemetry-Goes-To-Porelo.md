# PI59 — The Agent's Own Telemetry Goes To Porelo

```json
{
  "$schema": "issue-1.json",
  "id": "PI59",
  "title": "The Agent's Own Telemetry Goes To Porelo",
  "state": "Idea",
  "size": "-",
  "priority": "Medium",
  "rank": 1,
  "blockedBy": [],
  "covers": "the adapter declares Pi's own OpenTelemetry export pointed at Porelo's local receiver in the task, with every switch that would carry content - prompts, tool arguments and output, responses - set explicitly off"
}
```

**What must be true.** A task's Pi sends its own telemetry - its events, metrics and, where it has them, traces -
over OTLP to the receiver Porelo runs for the task, and to nowhere else. Every switch that would put content into that
telemetry (the person's prompts, tool arguments and output, the agent's responses) is set off explicitly in the
definition, never left to a default, so a new version that changes a default changes nothing here.

The contract built against: Porelo runs a local OTLP receiver for each task and keeps what arrives in the task's
records under its run; the adapter only declares how its agent is pointed at it.

What is known today, to be confirmed at the pinned version:
- Pi has no export of its own; it comes only through extensions (pi-otel, @desek/pi-opentelemetry), whose fit with
  the pinned Pi is open.

## To be checked

- Whether either extension works with the pinned Pi, what it exports and which content it carries.
- That an extension packaged with the adapter still loads under the task's `--no-approve` and `--no-skills`.
