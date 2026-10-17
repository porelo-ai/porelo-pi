package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.fuin.porelo.agent.api.AgentEnd;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PiJsonFormatter}, on lines in the shapes {@code pi --print --mode json} writes.
 */
class PiJsonFormatterTest {

    private static final String REFUSAL = "403 Key limit exceeded (total limit).";

    private final PiJsonFormatter formatter = new PiJsonFormatter();

    @Test
    void showsNothingForLifecycleAndStreamingEvents() {

        // The deltas made up most of a real run's lines; the finished message repeats what they
        // carried.
        assertThat(
                        formatter.format(
                                "{\"type\":\"session\",\"version\":3,\"id\":\"s1\",\"cwd\":\"/workspace\"}"))
                .isNull();
        assertThat(formatter.format("{\"type\":\"agent_start\"}")).isNull();
        assertThat(formatter.format("{\"type\":\"turn_start\"}")).isNull();
        assertThat(
                        formatter.format(
                                "{\"type\":\"turn_end\",\"message\":{\"role\":\"assistant\",\"content\":[]},"
                                        + "\"toolResults\":[]}"))
                .isNull();
        assertThat(
                        formatter.format(
                                "{\"type\":\"message_start\",\"message\":{\"role\":\"assistant\",\"content\":[]}}"))
                .isNull();
        assertThat(
                        formatter.format(
                                "{\"type\":\"message_update\",\"usage\":{},\"assistantMessageEvent\":"
                                        + "{\"type\":\"text_delta\",\"contentIndex\":0,\"delta\":\"Hel\"}}"))
                .isNull();
        assertThat(
                        formatter.format(
                                "{\"type\":\"tool_execution_update\",\"toolCallId\":\"c1\",\"toolName\":\"bash\","
                                        + "\"args\":{},\"partialResult\":{\"content\":[{\"type\":\"text\",\"text\":\"...\"}]}}"))
                .isNull();
        assertThat(formatter.format("{\"type\":\"agent_settled\"}")).isNull();
    }

    @Test
    void showsTheAssistantsTextWithoutItsThinking() {

        assertThat(
                        formatter.format(
                                "{\"type\":\"message_end\",\"message\":{\"role\":\"assistant\",\"content\":["
                                        + "{\"type\":\"thinking\",\"thinking\":\"let me see\"},"
                                        + "{\"type\":\"text\",\"text\":\"Reading   the\\n\\nbuild.\"},"
                                        + "{\"type\":\"toolCall\",\"id\":\"c1\",\"name\":\"read\",\"arguments\":{}},"
                                        + "{\"type\":\"text\",\"text\":\"Then the tests.\"}],\"stopReason\":\"toolUse\"}}"))
                .isEqualTo("Reading the build. Then the tests.");
    }

    @Test
    void cutsALongAnswer() {

        final String shown =
                formatter.format(
                        "{\"type\":\"message_end\",\"message\":{\"role\":\"assistant\","
                                + "\"content\":[{\"type\":\"text\",\"text\":\""
                                + "a".repeat(1000)
                                + "\"}],\"stopReason\":\"stop\"}}");

        assertThat(shown).isEqualTo("a".repeat(300) + "…");
    }

    @Test
    void showsNothingForAMessageWithoutText() {

        // A message that only calls tools: the calls show through their own events.
        assertThat(
                        formatter.format(
                                "{\"type\":\"message_end\",\"message\":{\"role\":\"assistant\",\"content\":["
                                        + "{\"type\":\"thinking\",\"thinking\":\"hm\"},"
                                        + "{\"type\":\"toolCall\",\"id\":\"c1\",\"name\":\"bash\",\"arguments\":{}}],"
                                        + "\"stopReason\":\"toolUse\"}}"))
                .isNull();
    }

    @Test
    void showsNothingForThePromptOrAToolResult() {

        assertThat(
                        formatter.format(
                                "{\"type\":\"message_end\",\"message\":{\"role\":\"user\",\"content\":["
                                        + "{\"type\":\"text\",\"text\":\"fix the bug\"}]}}"))
                .isNull();
        assertThat(
                        formatter.format(
                                "{\"type\":\"message_end\",\"message\":{\"role\":\"toolResult\","
                                        + "\"toolCallId\":\"c1\",\"toolName\":\"bash\",\"content\":[{\"type\":\"text\",\"text\":\"ok\"}],"
                                        + "\"isError\":false}}"))
                .isNull();
    }

    @Test
    void showsAFailedMessageByItsErrorMessage() {

        assertThat(
                        formatter.format(
                                "{\"type\":\"message_end\",\"message\":{\"role\":\"assistant\",\"content\":["
                                        + "{\"type\":\"text\",\"text\":\"\"}],\"stopReason\":\"error\",\"errorMessage\":\""
                                        + REFUSAL
                                        + "\"}}"))
                .isEqualTo("-- error: " + REFUSAL);
    }

    @Test
    void namesAToolWhenItStartsAndAgainOnlyWhenItFails() {

        assertThat(
                        formatter.format(
                                "{\"type\":\"tool_execution_start\",\"toolCallId\":\"c1\",\"toolName\":\"bash\","
                                        + "\"args\":{\"command\":\"ls\"}}"))
                .isEqualTo("[bash]");
        assertThat(
                        formatter.format(
                                "{\"type\":\"tool_execution_end\",\"toolCallId\":\"c1\",\"toolName\":\"bash\","
                                        + "\"result\":{\"content\":[]},\"isError\":false}"))
                .isNull();
        assertThat(
                        formatter.format(
                                "{\"type\":\"tool_execution_end\",\"toolCallId\":\"c1\",\"toolName\":\"bash\","
                                        + "\"result\":{\"content\":[]},\"isError\":true}"))
                .isEqualTo("[bash failed]");
    }

    @Test
    void endsWithTheNumberOfTurns() {

        assertThat(
                        formatter.format(
                                "{\"type\":\"agent_end\",\"messages\":["
                                        + "{\"role\":\"user\",\"content\":[]},"
                                        + "{\"role\":\"assistant\",\"content\":[],\"stopReason\":\"toolUse\"},"
                                        + "{\"role\":\"toolResult\",\"content\":[]},"
                                        + "{\"role\":\"assistant\",\"content\":[],\"stopReason\":\"stop\"}],\"willRetry\":false}"))
                .isEqualTo("-- done, 2 turns");
        assertThat(
                        formatter.format(
                                "{\"type\":\"agent_end\",\"messages\":["
                                        + "{\"role\":\"assistant\",\"content\":[],\"stopReason\":\"stop\"}],\"willRetry\":false}"))
                .isEqualTo("-- done, 1 turn");
    }

    @Test
    void endsWithTheProvidersRefusal() {

        // Read as done, a refused run looked like a success with nothing in it.
        assertThat(
                        formatter.format(
                                "{\"type\":\"agent_end\",\"messages\":["
                                        + "{\"role\":\"user\",\"content\":[]},"
                                        + "{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"\"}],\"stopReason\":\"error\","
                                        + "\"errorMessage\":\""
                                        + REFUSAL
                                        + "\"}],\"willRetry\":false}"))
                .isEqualTo("-- ended with an error: " + REFUSAL + ", 1 turn");
    }

    @Test
    void saysAnAbortedRunEndedWithAnError() {

        // Without an error message Pi's own text mode names the stop reason; so does the log.
        assertThat(
                        formatter.format(
                                "{\"type\":\"agent_end\",\"messages\":["
                                        + "{\"role\":\"assistant\",\"content\":[],\"stopReason\":\"toolUse\"},"
                                        + "{\"role\":\"assistant\",\"content\":[],\"stopReason\":\"aborted\"}],\"willRetry\":false}"))
                .isEqualTo("-- ended with an error: request aborted, 2 turns");
    }

    @Test
    void doesNotCallAnEndFinalWhenPiWillRetry() {

        assertThat(
                        formatter.format(
                                "{\"type\":\"agent_end\",\"messages\":["
                                        + "{\"role\":\"assistant\",\"content\":[],\"stopReason\":\"error\","
                                        + "\"errorMessage\":\"429 rate limited\"}],\"willRetry\":true}"))
                .isEqualTo("-- error, retrying: 429 rate limited, 1 turn");
    }

    @Test
    void showsAnUnknownEventCut() {

        final String line =
                "{\"type\":\"auto_retry_start\",\"attempt\":1,\"errorMessage\":\""
                        + "x".repeat(400)
                        + "\"}";

        assertThat(formatter.format("{\"type\":\"compaction_start\",\"reason\":\"threshold\"}"))
                .isEqualTo("{\"type\":\"compaction_start\",\"reason\":\"threshold\"}");
        assertThat(formatter.format(line)).isEqualTo(line.substring(0, 300) + "…");
    }

    @Test
    void showsALineThatIsNotJson() {

        assertThat(formatter.format("Extension error (/x.ts): boom"))
                .isEqualTo("Extension error (/x.ts): boom");
        assertThat(formatter.format("{\"type\":\"message_end\","))
                .isEqualTo("{\"type\":\"message_end\",");
    }

    @Test
    void marksARecognisedEventItCannotRead() {

        assertThat(formatter.format("{\"type\":\"agent_end\",\"messages\":\"none\"}"))
                .isEqualTo("[unreadable] {\"type\":\"agent_end\",\"messages\":\"none\"}");
    }

    @Test
    void passesNoTerminalControlCharacterToTheTerminal() {

        // What it returns is printed to a person's terminal: an escape there clears the screen, and
        // an OSC 52 writes the clipboard. JSON's \\u001b arrives as the live byte.
        final List<String> shown =
                List.of(
                        formatter.format(
                                "{\"type\":\"message_end\",\"message\":{\"role\":\"assistant\","
                                        + "\"content\":[{\"type\":\"text\",\"text\":\"a\\u001b[2Jb\"}],"
                                        + "\"stopReason\":\"stop\"}}"),
                        formatter.format(
                                "{\"type\":\"tool_execution_start\",\"toolName\":\"x\\u001b]52;c;aGk=\\u0007\"}"),
                        formatter.format(
                                "{\"type\":\"agent_end\",\"messages\":[{\"role\":\"assistant\",\"content\":[],"
                                        + "\"stopReason\":\"error\",\"errorMessage\":\"401 \\u009b2J\\u001b[2J\"}],"
                                        + "\"willRetry\":false}"),
                        formatter.format("raw \u001b[31mred\u001b[0m\tand\u0000 more"));
        final AgentEnd end =
                PiJsonFormatter.ended(
                        "{\"type\":\"agent_end\",\"messages\":[{\"role\":\"assistant\",\"content\":[],"
                                + "\"stopReason\":\"error\",\"errorMessage\":\"401 a\\u001b[2Jb\"}],"
                                + "\"willRetry\":false}");

        assertThat(end).as("a refused run's end").isNotNull();
        assertThat(shown).as("what format() returns").allSatisfy(PiJsonFormatterTest::printable);
        printable(end.error());
        assertThat(shown.get(0)).as("the text around the escape stays").isEqualTo("a[2Jb");
        assertThat(shown.get(3)).as("a tab is not a control to strip").contains("\t");
        assertThat(end.status()).as("the status still read from the message").isEqualTo(401);
    }

    private static void printable(final String text) {
        assertThat(
                        text.codePoints()
                                .filter(c -> Character.isISOControl(c) && c != '\n' && c != '\t'))
                .as(
                        "control characters in %s",
                        text.codePoints().mapToObj(Integer::toHexString).toList())
                .isEmpty();
    }
}
