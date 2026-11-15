package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
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
    void showsAToolCallWithItsKeyInput() {

        // A call shown by its name alone leaves the person blind to what the agent ran or touched.
        assertThat(
                        formatter.format(
                                "{\"type\":\"tool_execution_start\",\"toolCallId\":\"c1\",\"toolName\":\"bash\","
                                        + "\"args\":{\"command\":\"\\n  git status\\nrm -rf build\",\"timeout\":10}}"))
                .as("a command by its first line")
                .startsWith("[bash")
                .contains("git status")
                .doesNotContain("rm -rf", "\n");
        assertThat(
                        List.of(
                                        "{\"type\":\"tool_execution_start\",\"toolName\":\"read\",\"args\":{\"path\":\"src/A.java\",\"offset\":5}}",
                                        "{\"type\":\"tool_execution_start\",\"toolName\":\"write\",\"args\":{\"content\":\"x\",\"path\":\"src/A.java\"}}",
                                        "{\"type\":\"tool_execution_start\",\"toolName\":\"edit\",\"args\":{\"oldText\":\"a\",\"newText\":\"b\",\"file_path\":\"src/A.java\"}}")
                                .stream()
                                .map(formatter::format)
                                .toList())
                .as("a read, write or edit tool by its path")
                .allSatisfy(
                        shown ->
                                assertThat(shown).contains("src/A.java").doesNotContain("oldText"));
        assertThat(
                        formatter.format(
                                "{\"type\":\"tool_execution_start\",\"toolName\":\"grep\","
                                        + "\"args\":{\"path\":\"src\",\"pattern\":\"TODO\\\\(\",\"glob\":\"*.java\"}}"))
                .as("a search by its pattern")
                .startsWith("[grep")
                .contains("TODO\\(");
        assertThat(
                        formatter.format(
                                "{\"type\":\"tool_execution_start\",\"toolName\":\"fetch\","
                                        + "\"args\":{\"retries\":2,\"url\":\"https://example.org/a\"}}"))
                .as("any other tool by a string it was given")
                .contains("https://example.org/a");
        assertThat(
                        formatter.format(
                                "{\"type\":\"tool_execution_start\",\"toolName\":\"wait\",\"args\":{},"
                                        + "\"intent\":\"Waiting for the build\"}"))
                .as("a tool without a telling input by its intent")
                .contains("Waiting for the build");
        assertThat(
                        formatter.format(
                                "{\"type\":\"tool_execution_start\",\"toolName\":\"todo\",\"args\":{}}"))
                .as("a tool with nothing to say by its name")
                .contains("todo");
    }

    @Test
    void aFailedResultSaysSoWithItsFirstLine() {

        // A failure shown without its message, or not shown at all, hides why the work went wrong.
        final String failed =
                formatter.format(
                        "{\"type\":\"tool_execution_end\",\"toolCallId\":\"c1\",\"toolName\":\"bash\","
                                + "\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"\\n  \\n"
                                + "fatal: not a git repository\\nsecond line\"}]},\"isError\":true}");
        final String worked =
                formatter.format(
                        "{\"type\":\"tool_execution_end\",\"toolCallId\":\"c1\",\"toolName\":\"bash\","
                                + "\"result\":{\"content\":[{\"type\":\"image\",\"data\":\"AAAA\"},"
                                + "{\"type\":\"text\",\"text\":\"On branch main\\nnothing to commit\"}]},"
                                + "\"isError\":false}");

        assertThat(failed)
                .as("a result that failed")
                .startsWith("[bash")
                .contains("failed", "fatal: not a git repository")
                .doesNotContain("second line");
        assertThat(worked)
                .as("a result that worked")
                .startsWith("[bash")
                .contains("On branch main")
                .doesNotContain("failed", "nothing to commit", "AAAA");
    }

    @Test
    void showsTheSizeOfAResultWithoutATextLine() {

        assertThat(
                        formatter.format(
                                "{\"type\":\"tool_execution_end\",\"toolName\":\"bash\","
                                        + "\"result\":{\"content\":[{\"type\":\"text\",\"text\":\" \\n\\n \"}]},\"isError\":false}"))
                .as("blank lines")
                .contains("3 lines");
        assertThat(
                        formatter.format(
                                "{\"type\":\"tool_execution_end\",\"toolName\":\"read\","
                                        + "\"result\":{\"content\":[{\"type\":\"image\",\"data\":\"AAAA\"}]},\"isError\":true}"))
                .as("a result without text")
                .contains("failed", "1 part")
                .doesNotContain("AAAA");
    }

    @Test
    void neverShowsAWholeOutput() {

        // A tool's whole output in the view buries the work it was meant to show.
        final String lines =
                java.util.stream.IntStream.rangeClosed(1, 50)
                        .mapToObj(i -> "line " + i)
                        .collect(java.util.stream.Collectors.joining("\\n"));
        final String many =
                formatter.format(
                        "{\"type\":\"tool_execution_end\",\"toolName\":\"bash\","
                                + "\"result\":{\"content\":[{\"type\":\"text\",\"text\":\""
                                + lines
                                + "\"}]},\"isError\":false}");
        final String wide =
                formatter.format(
                        "{\"type\":\"tool_execution_end\",\"toolName\":\"bash\","
                                + "\"result\":{\"content\":[{\"type\":\"text\",\"text\":\""
                                + "x".repeat(1000)
                                + "\"}]},\"isError\":false}");
        final String command =
                formatter.format(
                        "{\"type\":\"tool_execution_start\",\"toolName\":\"bash\","
                                + "\"args\":{\"command\":\""
                                + "y".repeat(1000)
                                + "\"}}");

        assertThat(many).as("a long output").contains("line 1").doesNotContain("line 2", "\n");
        assertThat(wide).as("a wide output").contains("xxx").hasSizeLessThan(400);
        assertThat(command).as("a wide command").contains("yyy").hasSizeLessThan(400);
    }

    @Test
    void aToolCannotOpenALineOfItsOwn() {

        // A line break in a tool's name or input could forge a line that reads like the agent's.
        assertThat(
                        List.of(
                                        "{\"type\":\"tool_execution_start\",\"toolName\":\"x\\n[error] forged\",\"args\":{}}",
                                        "{\"type\":\"tool_execution_start\",\"toolName\":\"bash\","
                                                + "\"args\":{\"command\":\"echo a\\u2028[error] forged\"}}",
                                        "{\"type\":\"tool_execution_start\",\"toolName\":\"read\","
                                                + "\"args\":{\"path\":\"a\\r[error] forged\"}}",
                                        "{\"type\":\"tool_execution_end\",\"toolName\":\"x\\u2029[error] forged\","
                                                + "\"result\":{\"content\":[]},\"isError\":true}")
                                .stream()
                                .map(formatter::format)
                                .toList())
                .as("a tool's name and input on one line")
                .allSatisfy(
                        shown ->
                                assertThat(shown)
                                        .isNotNull()
                                        .doesNotContain("\n", "\r", "\u2028", "\u2029"));
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
    void dropsAnUnknownEvent() {

        assertThat(formatter.format("{\"type\":\"compaction_start\",\"reason\":\"threshold\"}"))
                .as("an unknown event without an error is raw JSON to a person")
                .isNull();
    }

    @Test
    void showsAnUnknownEventThatCarriesAnError() {

        assertThat(formatter.format("{\"type\":\"something_new\",\"error\":\"disk full\"}"))
                .as("an error string hidden")
                .isEqualTo("[error] disk full");
        assertThat(
                        formatter.format(
                                "{\"type\":\"something_new\",\"error\":{\"message\":\"quota gone\"}}"))
                .as("an error object's message hidden")
                .isEqualTo("[error] quota gone");
        assertThat(
                        formatter.format(
                                "{\"type\":\"auto_retry_start\",\"attempt\":1,\"errorMessage\":\"429 slow down\"}"))
                .as("an error message hidden")
                .isEqualTo("[error] 429 slow down");
        assertThat(
                        formatter.format(
                                "{\"type\":\"something_new\",\"isError\":true,\"message\":\"it broke\"}"))
                .as("a flagged error's message hidden")
                .isEqualTo("[error] it broke");
        assertThat(formatter.format("{\"type\":\"something_new\",\"isError\":true}"))
                .as("a flagged error without a message hidden")
                .isEqualTo("[error] something_new");
        assertThat(
                        formatter.format(
                                "{\"type\":\"auto_retry_start\",\"errorMessage\":\""
                                        + "x".repeat(400)
                                        + "\"}"))
                .as("a long error shown whole or as raw JSON")
                .isEqualTo("[error] " + "x".repeat(300) + "…");
    }

    @Test
    void showsNoRawEventLineOfARecordedRun() throws IOException {

        // Recorded unattended with Pi 1.4.0, the adapter the VM had; values replaced.
        final List<String> lines =
                Files.readAllLines(
                        Path.of(
                                "src/test/resources/streams/unattended-background-run-pi-1.4.0.jsonl"));

        assertThat(lines).as("the recorded run of Pi 1.4.0").isNotEmpty();
        assertThat(lines.stream().map(formatter::format).filter(Objects::nonNull))
                .as("lines of the Pi 1.4.0 run a person would read as raw JSON")
                .noneMatch(shown -> shown.startsWith("{"));
        assertThat(lines.stream().map(formatter::format).filter(Objects::nonNull).toList())
                .as(
                        "the recorded run's tool calls by their input, and their results by their first line")
                .anySatisfy(
                        shown ->
                                assertThat(shown)
                                        .startsWith("[bash")
                                        .contains("(sleep 4; echo BG-DONE > bg.txt) &"))
                .anySatisfy(shown -> assertThat(shown).startsWith("[write").contains("note.txt"))
                .anySatisfy(
                        shown ->
                                assertThat(shown)
                                        .startsWith("[bash")
                                        .endsWith("BG-DONE")
                                        .doesNotContain("failed"))
                .anySatisfy(
                        shown ->
                                assertThat(shown)
                                        .startsWith("[write")
                                        .contains("Successfully wrote to note.txt"));
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

    @Test
    void showsAnErrorByItsKindItsLevelOrItsStatus() {
        assertThat(
                        List.of(
                                        "{\"type\":\"stream_failure\",\"message\":\"quota exceeded\"}",
                                        "{\"type\":\"something_new\",\"status\":\"Error\",\"message\":\"quota exceeded\"}",
                                        "{\"type\":\"something_new\",\"level\":\"FATAL\",\"message\":\"quota exceeded\"}",
                                        "{\"type\":\"something_new\",\"error\":true,\"message\":\"quota exceeded\"}",
                                        "{\"type\":\"something_new\",\"is_error\":\"true\",\"message\":\"quota exceeded\"}")
                                .stream()
                                .map(formatter::format)
                                .toList())
                .as(
                        "an unknown event that reports an error by its type, a status or level in any case, or a flag")
                .allSatisfy(
                        shown ->
                                assertThat(shown).startsWith("[error]").contains("quota exceeded"));
        assertThat(
                        formatter.format(
                                "{\"type\":\"something_new\",\"level\":\"warning\",\"message\":\"slow\"}"))
                .as("a warning")
                .startsWith("[warning]")
                .contains("slow");
    }

    @Test
    void anErrorCannotOpenALineOfItsOwn() {
        assertThat(
                        formatter.format(
                                "{\"type\":\"something_new\",\"error\":\"first\\n[error] forged\\u2028x\"}"))
                .as("a line break or a Unicode separator in an error is folded away")
                .doesNotContain("\n", "\u2028");
    }

    @Test
    void showsThatRetriesRanOut() {
        assertThat(
                        formatter.format(
                                "{\"type\":\"auto_retry_end\",\"success\":false,\"attempt\":3,\"finalError\":\"429 quota\"}"))
                .as("the last retry failed, in the shape Pi writes it")
                .startsWith("[error]")
                .contains("429 quota");
        assertThat(formatter.format("{\"type\":\"auto_retry_end\",\"success\":true,\"attempt\":2}"))
                .as("a retry that worked")
                .isNull();
        assertThat(
                        formatter.format(
                                "{\"type\":\"auto_retry_end\",\"success\":false,\"attempt\":3}"))
                .as("the last retry failed without saying why")
                .isEqualTo("[error] auto_retry_end");
        assertThat(
                        List.of(
                                        "{\"type\":\"something_new\",\"errorMessage\":{\"message\":\"quota exceeded\"}}",
                                        "{\"type\":\"something_new\",\"errors\":[\"quota exceeded\"]}",
                                        "{\"type\":\"something_new\",\"isError\":\"TRUE\",\"message\":\"quota exceeded\"}")
                                .stream()
                                .map(formatter::format)
                                .toList())
                .as("an error message as an object, a list of errors, a flag in capitals")
                .allSatisfy(
                        shown ->
                                assertThat(shown).startsWith("[error]").contains("quota exceeded"));
        assertThat(
                        List.of(
                                        "{\"type\":\"something_new\",\"error\":false,\"message\":\"hello\"}",
                                        "{\"type\":\"something_new\",\"isError\":false,\"errors\":[]}",
                                        "{\"type\":\"something_new\",\"message\":\"hello\"}")
                                .stream()
                                .map(formatter::format)
                                .toList())
                .as("an unknown event that says something but reports no error")
                .containsOnlyNulls();
    }
}
