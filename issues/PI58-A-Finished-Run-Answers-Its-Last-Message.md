# PI58 — A Finished Run Answers Its Last Message

```json
{
  "$schema": "issue-1.json",
  "id": "PI58",
  "title": "A Finished Run Answers Its Last Message",
  "state": "Implementing",
  "size": "S",
  "priority": "Highest",
  "rank": 1,
  "blockedBy": [],
  "covers": "a finished unattended Pi run answers the text of its last assistant message, its text parts past its thinking, through session.waiting.last_message, while Porelo still never types a wake line into Pi and still says it cannot tell whether Pi waits"
}
```

**Why this size.** one declaration and its test in this repository; it builds against an optional field of Porelo's
agent API and must not change how Porelo wakes Pi.

**What must be true.** When an unattended Pi run ends, Porelo answers what Pi said last: the text of its last assistant
message. A reader whose findings stand in that message is judged on them, not on an empty answer.

## Plan

- The contract built against: `session.waiting.last_message` names the last record whose fields match `record` and
  the field holding its text. With `part: { record: {...}, text: <field> }`, that field may hold a list of parts, and
  the answer is the text of each matching part, joined by a newline in order.
- Pi ends each message with `{"type":"message_end","message":{"role":"assistant","content":[...]}}`, its content a
  list of parts: `{"type":"thinking","thinking":...}`, `{"type":"text","text":...}` and tool calls (Pi's
  `docs/json.md` calls `message_end` "the authoritative final message"). `src/main/resources/agent/pi.yaml` declares
  `last_message: { record: { type: message_end, message.role: assistant }, text: message.content, part: { record:
  { type: text }, text: text } }`.
- Pi declares no screen rules on purpose: its screen cannot tell a question it put from rest. A `waiting` block that
  carries only `last_message` must therefore keep Porelo from typing a wake line into Pi and keep its waiting
  reading at "cannot say". This holds only where Porelo treats a `waiting` without screen rules as declaring none,
  which is checked before hand-over.
- Tests: a unit test reads the definition and answers the last message of a run of Pi's records (red first, without
  the declaration); it also asserts that the declaration names no screen rule.

## Acceptance

- Over Pi's records of a run (a user message, an assistant message of thinking and text, an assistant message of
  tool calls only), the answer is the text of the assistant's text parts, without its thinking.
- The definition declares no screen and no not-its-screen rule.

## To be checked

- That Porelo, as built against, wakes no agent whose `waiting` has no screen rules, and reads it as "cannot say".
