package org.fuin.porelo.agent.impl.pi;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.fuin.porelo.agent.api.AgentEnd;
import org.fuin.porelo.agent.api.LogFormatter;
import org.fuin.porelo.wire.Json;
import org.jspecify.annotations.Nullable;

/**
 * Renders the events of {@code pi --print --mode json} as readable lines.
 *
 * <p>Most of what Pi writes is streaming: every token arrives as a {@code message_update} and every
 * chunk of tool output as a {@code tool_execution_update}, and a single line can hold a whole file.
 * The finished message is repeated in {@code message_end}, so the deltas are dropped and the
 * assistant's text is taken from there.
 *
 * <p>A person reads what the agent says and does, so an event of a type this formatter does not
 * know is shown only when it reports an error, as {@code [error] <message>}: nothing that went
 * wrong is hidden. A line that is not a JSON object still shows as it came, cut.
 */
public final class PiJsonFormatter implements LogFormatter {

    /**
     * Lifecycle and streaming events: what they carry arrives again, complete, in a later event.
     */
    private static final Set<String> SILENT =
            Set.of(
                    "session",
                    "agent_start",
                    "agent_settled",
                    "turn_start",
                    "turn_end",
                    "message_start",
                    "message_update",
                    "tool_execution_update");

    /** Prefixes a line the formatter recognised but could not read, so it is visibly not prose. */
    private static final String UNREADABLE = "[unreadable] ";

    /**
     * An HTTP status a provider's refusal starts its message with: {@code 401: ...}, {@code 403 Key
     * ...}.
     */
    private static final java.util.regex.Pattern LEADING_STATUS =
            java.util.regex.Pattern.compile("^([45]\\d{2})\\b");

    /**
     * In code points: long enough for a sentence or a provider's refusal, short enough to stay one
     * line.
     */
    private static final int MAX = 300;

    /** Marks a line that was cut. */
    private static final String CUT = "…";

    @Override
    public @Nullable String format(String line) {
        final String shown = shape(line);
        return shown == null ? null : printable(shown);
    }

    private static @Nullable String shape(String line) {

        final String trimmed = line.strip();
        if (!trimmed.startsWith("{")) {
            return cut(line);
        }

        final Object parsed;
        try {
            parsed = Json.parse(trimmed);
        } catch (RuntimeException ex) {
            // Not JSON after all, or a partial line. Show it.
            return cut(line);
        }
        if (!(parsed instanceof Map<?, ?> event)) {
            return cut(line);
        }

        final String type = String.valueOf(event.get("type"));
        if (SILENT.contains(type)) {
            return null;
        }
        return switch (type) {
            case "message_end" -> message(event, line);
            case "tool_execution_start" ->
                    event.get("toolName") instanceof String tool
                            ? "[" + tool + "]"
                            : UNREADABLE + cut(line);
            case "tool_execution_end" ->
                    !(event.get("toolName") instanceof String tool)
                            ? UNREADABLE + cut(line)
                            : Boolean.TRUE.equals(event.get("isError"))
                                    ? "[" + tool + " failed]"
                                    : null;
            case "agent_end" -> end(event, line);
            default -> unknown(event, type);
        };
    }

    /** The words a status uses for a step that went wrong. */
    private static final Set<String> FAILED =
            Set.of("failed", "failure", "error", "killed", "rejected", "denied", "aborted");

    /**
     * An unknown event that reports an error or a warning, as such with its message on one line;
     * any other is dropped. An error is named by the event's type, its level, a status of failure,
     * an error mark, a success that is false (retries that ran out), or an error it carries; a
     * warning by its type or level.
     */
    private static @Nullable String unknown(Map<?, ?> event, String type) {
        final boolean error =
                names(type, "error", "fail", "fatal", "exception")
                        || names(event.get("level"), "error", "fatal")
                        || failure(event.get("status"))
                        || marked(event.get("isError"))
                        || marked(event.get("is_error"))
                        || Boolean.FALSE.equals(event.get("success"))
                        || carries(event.get("error"))
                        || carries(event.get("errorMessage"))
                        || carries(event.get("finalError"))
                        || carries(event.get("errors"));
        final boolean warning = names(type, "warn") || names(event.get("level"), "warn");
        if (!error && !warning) {
            return null;
        }
        return (error ? "[error] " : "[warning] ") + cut(oneLine(troubleText(event, type)));
    }

    private static boolean names(@Nullable Object value, String... words) {
        if (!(value instanceof String text)) {
            return false;
        }
        final String lower = text.toLowerCase(java.util.Locale.ROOT);
        for (final String word : words) {
            if (lower.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private static boolean failure(@Nullable Object status) {
        return status instanceof String text
                && FAILED.contains(text.strip().toLowerCase(java.util.Locale.ROOT));
    }

    private static boolean marked(@Nullable Object value) {
        return Boolean.TRUE.equals(value)
                || value instanceof String text && "true".equalsIgnoreCase(text.strip());
    }

    /**
     * Whether a field holds an error: any text, a flag that is set, a number, a list or an object.
     */
    private static boolean carries(@Nullable Object error) {
        return text(error) != null
                || Boolean.TRUE.equals(error)
                || error instanceof Number
                || error instanceof List<?> list && !list.isEmpty()
                || error instanceof Map<?, ?> map && !map.isEmpty();
    }

    /** The fields an event gives an error in, in the order they are read. */
    private static final List<String> ERROR_KEYS =
            List.of("error", "errorMessage", "finalError", "errors");

    /** What an error says: its own text or message, the event's, the error itself, or its type. */
    private static String troubleText(Map<?, ?> event, String type) {
        for (final String key : ERROR_KEYS) {
            final Object error = event.get(key);
            if (text(error) instanceof String own) {
                return own;
            }
            if (error instanceof Map<?, ?> nested
                    && text(nested.get("message")) instanceof String message) {
                return message;
            }
        }
        if (text(event.get("message")) instanceof String said) {
            return said;
        }
        for (final String key : ERROR_KEYS) {
            if (carries(event.get(key)) && !Boolean.TRUE.equals(event.get(key))) {
                return String.valueOf(event.get(key));
            }
        }
        return type;
    }

    private static @Nullable String text(@Nullable Object value) {
        return value instanceof String said && !said.isBlank() ? said.strip() : null;
    }

    /**
     * Folds a message onto one line, so a line break in it - a Unicode separator included - cannot
     * pass for a line of the log, and drops the invisible marks that turn text around.
     */
    private static String oneLine(String text) {
        return text.replaceAll("\\p{Cf}", "")
                .replaceAll("[\\s\\u0085\\u2028\\u2029]+", " ")
                .strip();
    }

    private static @Nullable String message(Map<?, ?> event, String line) {
        if (!(event.get("message") instanceof Map<?, ?> message)) {
            return UNREADABLE + cut(line);
        }
        // The prompt and every tool result end as a message too; the tool call already showed.
        if (!"assistant".equals(message.get("role"))) {
            return null;
        }
        final String failure = failure(message);
        if (failure != null) {
            return "-- error: " + failure;
        }
        if (!(message.get("content") instanceof List<?> content)) {
            return UNREADABLE + cut(line);
        }
        final StringBuilder out = new StringBuilder();
        for (final Object block : content) {
            // Thinking is skipped, and a tool call is shown by its own event.
            if (block instanceof Map<?, ?> map
                    && "text".equals(map.get("type"))
                    && map.get("text") instanceof String text) {
                out.append(text).append(' ');
            }
        }
        final String text = collapse(out.toString());
        return text.isEmpty() ? null : text;
    }

    private static String end(Map<?, ?> event, String line) {
        if (!(event.get("messages") instanceof List<?> messages)) {
            return UNREADABLE + cut(line);
        }
        int turns = 0;
        Map<?, ?> last = null;
        for (final Object item : messages) {
            if (item instanceof Map<?, ?> message && "assistant".equals(message.get("role"))) {
                turns++;
                last = message;
            }
        }
        final String count = turns + (turns == 1 ? " turn" : " turns");
        final String failure = last == null ? null : failure(last);
        if (failure == null) {
            return "-- done, " + count;
        }
        // Pi retries a refusal it judges transient and ends the run again afterwards, so this end
        // is
        // not the last one and must not read like it.
        if (Boolean.TRUE.equals(event.get("willRetry"))) {
            return "-- error, retrying: " + failure + ", " + count;
        }
        return "-- ended with an error: " + failure + ", " + count;
    }

    /**
     * Reads one line for how the run ended: an {@code agent_end} whose last assistant message
     * decides it. An end Pi is about to retry is not the run's end, because another follows it.
     *
     * @param line One raw line of output.
     * @return How the run ended, or {@code null} for a line that is not its end.
     */
    static @Nullable AgentEnd ended(String line) {
        final String trimmed = line.strip();
        if (!trimmed.startsWith("{")) {
            return null;
        }
        final Object parsed;
        try {
            parsed = Json.parse(trimmed);
        } catch (RuntimeException ex) {
            return null;
        }
        if (!(parsed instanceof Map<?, ?> event)
                || !"agent_end".equals(event.get("type"))
                || Boolean.TRUE.equals(event.get("willRetry"))
                || !(event.get("messages") instanceof List<?> messages)) {
            return null;
        }
        Map<?, ?> last = null;
        for (final Object item : messages) {
            if (item instanceof Map<?, ?> message && "assistant".equals(message.get("role"))) {
                last = message;
            }
        }
        final Map<?, ?> found = last;
        final String failure = found == null ? null : failure(found);
        if (found == null || failure == null) {
            return new AgentEnd(true, "", AgentEnd.AGENT, null);
        }
        // The provider's refusal carries its HTTP status, as a field or at the head of the message
        // ("401: {...}"); without one it is the agent's own stop.
        if (found.get("errorStatus") instanceof Number status) {
            return new AgentEnd(false, failure, AgentEnd.PROVIDER, status.intValue());
        }
        final java.util.regex.Matcher leading = LEADING_STATUS.matcher(failure);
        return leading.find()
                ? new AgentEnd(false, failure, AgentEnd.PROVIDER, Integer.valueOf(leading.group(1)))
                : new AgentEnd(false, failure, AgentEnd.AGENT, null);
    }

    /**
     * Says why an assistant message failed, the way Pi's own text mode does: its error message, or
     * the stop reason when there is none. An aborted run is a failure too; read as done, it looked
     * like a success with nothing in it.
     */
    private static @Nullable String failure(Map<?, ?> message) {
        final Object reason = message.get("stopReason");
        if (!"error".equals(reason) && !"aborted".equals(reason)) {
            return null;
        }
        if (message.get("errorMessage") instanceof String said && !said.isBlank()) {
            return printable(collapse(said));
        }
        return "request " + reason;
    }

    /**
     * Drops every control character but a newline and a tab: what the formatter returns reaches a
     * person's terminal, where an escape sequence moves the cursor, clears the screen or, as OSC
     * 52, writes the clipboard.
     */
    private static String printable(String text) {
        final StringBuilder out = new StringBuilder(text.length());
        text.codePoints()
                .filter(c -> !Character.isISOControl(c) || c == '\n' || c == '\t')
                .forEach(out::appendCodePoint);
        return out.toString();
    }

    private static String collapse(String text) {
        return cut(text.strip().replaceAll("\\s+", " "));
    }

    /** Cuts after {@link #MAX} code points, so a character outside the BMP is never split. */
    private static String cut(String text) {
        return text.codePointCount(0, text.length()) > MAX
                ? text.substring(0, text.offsetByCodePoints(0, MAX)) + CUT
                : text;
    }
}
