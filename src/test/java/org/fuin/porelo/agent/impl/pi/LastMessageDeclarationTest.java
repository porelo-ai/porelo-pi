package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.fuin.porelo.agent.api.Waiting;
import org.fuin.porelo.wire.Json;
import org.junit.jupiter.api.Test;

/**
 * What a finished Pi run said last: the harm is a reader whose findings stand in its last message
 * and whose run is judged on an empty answer, or on a tool's output in place of its own.
 */
class LastMessageDeclarationTest {

    /**
     * Pi's records of a run that ended after a tool call, as at a turn limit: the user's message,
     * the assistant's reply of thinking and two text parts, its tool call, and that call's result.
     */
    private static final List<Object> RECORDS =
            List.of(
                    Json.parse(
                            "{\"type\":\"message_end\",\"message\":{\"role\":\"user\","
                                    + "\"content\":[{\"type\":\"text\",\"text\":\"Read the issue.\"}]}}"),
                    Json.parse(
                            "{\"type\":\"message_end\",\"message\":{\"role\":\"assistant\","
                                    + "\"content\":[{\"type\":\"thinking\",\"thinking\":\"Let me see.\"},"
                                    + "{\"type\":\"text\",\"text\":\"Findings:\"},"
                                    + "{\"type\":\"text\",\"text\":\"- one\"}]}}"),
                    Json.parse(
                            "{\"type\":\"message_end\",\"message\":{\"role\":\"assistant\","
                                    + "\"content\":[{\"type\":\"toolCall\",\"name\":\"bash\"}]}}"),
                    Json.parse(
                            "{\"type\":\"message_end\",\"message\":{\"role\":\"toolResult\","
                                    + "\"content\":[{\"type\":\"text\",\"text\":\"tool output\"}]}}"));

    private static Waiting waiting() {
        final Waiting waiting = new PiAgent().definition().waiting();
        assertThat(waiting).as("pi.yaml declares session.waiting.last_message").isNotNull();
        return waiting;
    }

    @Test
    void answersTheTextOfTheLastAssistantMessagePastItsThinking() {

        final Waiting.LastMessage last = waiting().lastMessage();
        assertThat(last).as("pi.yaml declares session.waiting.last_message").isNotNull();
        assertThat(last.of(RECORDS))
                .as(
                        "the text parts of the last assistant reply, never its thinking or a tool's output")
                .isEqualTo("Findings:\n- one");
    }
}
