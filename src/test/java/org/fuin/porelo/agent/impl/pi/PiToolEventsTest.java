package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.fuin.porelo.agent.api.ToolEvent;
import org.fuin.porelo.agent.api.ToolEvent.Kind;
import org.fuin.porelo.agent.api.ToolEvent.Phase;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/** Tests the tool-call contract through the adapter on shortened recorded Pi output. */
class PiToolEventsTest {

    private final PiAgent agent = new PiAgent();

    @Test
    void keepsOverlappingCallsIdentifiableWhenTheyEndInReverseOrder() throws IOException {
        final List<String> lines = recording();

        assertThat(lines.subList(4, 8).stream().flatMap(line -> agent.toolEvents(line).stream()))
                .as("Each overlapping call must retain its own ID, kind and duration")
                .containsExactly(
                        event(
                                "functions.bash:2",
                                Phase.START,
                                "bash",
                                Kind.SHELL,
                                "find . -name \"*.java\" -exec grep -l \"Sluice\" {} \\; | wc -l",
                                null),
                        event("functions.edit:3", Phase.START, "edit", Kind.EDIT, null, null),
                        event("functions.edit:3", Phase.END, "edit", Kind.EDIT, null, 10L),
                        event("functions.bash:2", Phase.END, "bash", Kind.SHELL, null, 78L));
    }

    @Test
    void passesTheWholeShellCommandWithoutInferringABackgroundJob() throws IOException {
        final List<String> lines = recording();

        assertThat(agent.toolEvents(lines.get(0)))
                .as("A shell ampersand is command text, and masking belongs to Porelo")
                .containsExactly(
                        event(
                                "functions.bash:0",
                                Phase.START,
                                "bash",
                                Kind.SHELL,
                                "./mvnw -B -s settings.xml -pl filter/app -am test > /tmp/build.log 2>&1 &\necho $!",
                                null));
        assertThat(agent.toolEvents(lines.get(2)))
                .as("The end must stand alone without retaining the command or output")
                .containsExactly(
                        event("functions.bash:0", Phase.END, "bash", Kind.SHELL, null, 9L));
    }

    @Test
    void recordsAReadWithoutCopyingTheFilesContents() throws IOException {
        final List<String> lines = recording();

        assertThat(agent.toolEvents(lines.get(1)))
                .as("A file path must not become a shell command")
                .containsExactly(
                        event("functions.read:1", Phase.START, "read", Kind.READ, null, null));
        assertThat(agent.toolEvents(lines.get(3)))
                .as("The read result must contribute only identity and timing")
                .containsExactly(
                        event("functions.read:1", Phase.END, "read", Kind.READ, null, 10L));
    }

    @Test
    void classifiesBuiltInAndExtensionToolsWithoutDroppingUnknownCalls() throws IOException {
        final String end = recording().get(3);
        final Map<String, Kind> tools =
                Map.of(
                        "read",
                        Kind.READ,
                        "edit",
                        Kind.EDIT,
                        "write",
                        Kind.EDIT,
                        "grep",
                        Kind.SEARCH,
                        "find",
                        Kind.SEARCH,
                        "ls",
                        Kind.SEARCH,
                        "extension",
                        Kind.OTHER);

        // Vary the tool name because the recording does not invoke every tool.
        for (final Map.Entry<String, Kind> tool : tools.entrySet()) {
            final String line =
                    end.replace("\"toolName\":\"read\"", "\"toolName\":\"" + tool.getKey() + "\"");
            assertThat(agent.toolEvents(line))
                    .as("The %s call must remain visible with its tool kind", tool.getKey())
                    .containsExactly(
                            event(
                                    "functions.read:1",
                                    Phase.END,
                                    tool.getKey(),
                                    tool.getValue(),
                                    null,
                                    10L));
        }
    }

    @Test
    void endsFailedCallsWithoutCopyingTheirErrorOutput() throws IOException {
        // Vary the recorded success flag because this run contains no failed tool call.
        final String line = recording().get(9).replace("\"isError\":false", "\"isError\":true");

        assertThat(agent.toolEvents(line))
                .as("Failure must still close the call and retain its reported duration")
                .containsExactly(
                        event("functions.bash:4", Phase.END, "bash", Kind.SHELL, null, 11L));
    }

    @Test
    void ignoresUnreadableAndUnrelatedLinesWithoutInterruptingTheRun() throws IOException {
        final String start = recording().get(0);
        final List<String> lines =
                List.of(
                        "",
                        "not json",
                        "{",
                        "[]",
                        "null",
                        "42",
                        "{}",
                        "{\"type\":\"message_end\"}",
                        start.replace("tool_execution_start", "tool_execution_update"),
                        start.replace("toolCallId", "unknownId"),
                        start.replace("\"functions.bash:0\"", "null"),
                        start.replace("\"functions.bash:0\"", "\"\""),
                        start.replace("\"toolName\":\"bash\"", "\"toolName\":[]"),
                        start.replace("\"toolName\":\"bash\"", "\"toolName\":\"\""));

        for (final String line : lines) {
            assertThat(agent.toolEvents(line))
                    .as("Unreadable or unrelated output must yield no tool event: %s", line)
                    .isEmpty();
        }
    }

    @Test
    void keepsAShellStartWhoseCommandLineIsMissing() throws IOException {
        final String start = recording().get(0);

        for (final String line :
                List.of(
                        start.replace("\"args\":{", "\"unknownArgs\":{"),
                        start.replace("\"command\":", "\"unknownCommand\":"))) {
            assertThat(agent.toolEvents(line))
                    .as(
                            "A call whose input cannot be read still starts, so its time is not lost: %s",
                            line)
                    .containsExactly(
                            event("functions.bash:0", Phase.START, "bash", Kind.SHELL, null, null));
        }
    }

    @Test
    void endsACallWhoseDurationIsMissing() throws IOException {
        final String end = recording().get(2).replace("\"durationMs\":9", "\"unknownDuration\":9");

        assertThat(agent.toolEvents(end))
                .as("An end without a duration still ends the call, so it is not left open")
                .containsExactly(
                        event("functions.bash:0", Phase.END, "bash", Kind.SHELL, null, null));
    }

    private static ToolEvent event(
            String id,
            Phase phase,
            String tool,
            Kind kind,
            @Nullable String command,
            @Nullable Long durationMs) {
        return new ToolEvent(
                0, id, phase, tool, kind, command, false, null, null, null, null, durationMs);
    }

    private static List<String> recording() throws IOException {
        try (final var stream = PiToolEventsTest.class.getResourceAsStream("tool-events.jsonl")) {
            if (stream == null) {
                throw new IOException("Missing recorded tool events");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }
}
