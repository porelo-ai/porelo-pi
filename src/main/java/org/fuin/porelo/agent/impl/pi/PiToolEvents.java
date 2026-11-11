package org.fuin.porelo.agent.impl.pi;

import java.util.List;
import java.util.Map;
import org.fuin.porelo.agent.api.ToolEvent;
import org.fuin.porelo.agent.api.ToolEvent.Kind;
import org.fuin.porelo.agent.api.ToolEvent.Phase;
import org.fuin.porelo.wire.Json;
import org.jspecify.annotations.Nullable;

/** Reads Pi's tool-call events without keeping state between output lines. */
public final class PiToolEvents {

    private PiToolEvents() {}

    /**
     * Maps one raw output line to its tool event.
     *
     * @param line One line from Pi's JSON mode.
     * @return Its tool event, or an empty list for unreadable or unrelated output.
     */
    public static List<ToolEvent> read(String line) {
        final String trimmed = line.strip();
        if (!trimmed.startsWith("{")) {
            return List.of();
        }
        final Object parsed;
        try {
            parsed = Json.parse(trimmed);
        } catch (RuntimeException ex) {
            return List.of();
        }
        if (!(parsed instanceof Map<?, ?> event)
                || !(event.get("toolCallId") instanceof String id)
                || id.isBlank()
                || !(event.get("toolName") instanceof String tool)
                || tool.isBlank()) {
            return List.of();
        }

        final Phase phase;
        final @Nullable String command;
        final @Nullable Long durationMs;
        if ("tool_execution_start".equals(event.get("type"))) {
            phase = Phase.START;
            // A start without its input is still a call; only its command line is unknown.
            command =
                    "bash".equals(tool)
                                    && event.get("args") instanceof Map<?, ?> args
                                    && args.get("command") instanceof String shellCommand
                            ? shellCommand
                            : null;
            durationMs = null;
        } else if ("tool_execution_end".equals(event.get("type"))) {
            phase = Phase.END;
            command = null;
            durationMs =
                    event.get("durationMs") instanceof Number duration
                            ? Long.valueOf(duration.longValue())
                            : null;
        } else {
            return List.of();
        }
        return List.of(
                new ToolEvent(
                        0,
                        id,
                        phase,
                        tool,
                        kind(tool),
                        command,
                        false,
                        null,
                        null,
                        null,
                        null,
                        durationMs));
    }

    private static Kind kind(String tool) {
        return switch (tool) {
            case "bash" -> Kind.SHELL;
            case "edit", "write" -> Kind.EDIT;
            case "read" -> Kind.READ;
            case "grep", "find", "ls" -> Kind.SEARCH;
            default -> Kind.OTHER;
        };
    }
}
