package org.fuin.porelo.agent.impl.pi;

import org.fuin.porelo.wire.Json;

/**
 * The extension that gives Pi a way to ask the person, one the host can see.
 *
 * <p><strong>Why this exists at all.</strong> Pi has no tool to ask a person. A question the model
 * writes as text ends its run like any other answer, so nothing on Pi's screen or in what it
 * reports tells a question waiting for an answer from a turn that is done. Pi does tell its own
 * dialogs apart: it draws their title on the screen. So this extension gives the model a tool that
 * asks through a dialog with a fixed title, and {@code pi.yaml} declares that title as waiting.
 *
 * <p>Without a person - an unattended run, where Pi has no dialogs - the tool answers at once and
 * tells the model to decide itself, so the run never hangs on a question nobody sees.
 */
final class PiAskExtension {

    /** Auto-discovered by Pi; the name only has to be unique and end in {@code .ts}. */
    static final String FILE = "/var/lib/pi/agent/extensions/porelo-ask.ts";

    /** The dialog's title, which {@code pi.yaml} reads as waiting. */
    static final String TITLE = "Pi asks you";

    private PiAskExtension() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Returns the extension that registers the tool to ask the person.
     *
     * @return File content.
     */
    static String document() {
        return """
                // Written by Porelo. Gives the model a tool to ask the person, through a dialog
                // whose title the host reads as "waiting for a person".
                const TITLE = %s;

                export default function (pi) {
                    pi.registerTool({
                        name: "ask_person",
                        label: "Ask the person",
                        description:
                            "Ask the person attending this session a question and wait for the answer.",
                        promptSnippet: "Ask the person attending this session a question and wait for the answer",
                        promptGuidelines: [
                            "To ask the person anything, use ask_person and never end your turn with a question in text.",
                        ],
                        parameters: {
                            type: "object",
                            properties: {
                                question: { type: "string", description: "The question, complete in itself" },
                            },
                            required: ["question"],
                        },
                        async execute(toolCallId, params, signal, onUpdate, ctx) {
                            let text;
                            if (!ctx || !ctx.hasUI) {
                                text = "no person is attending; decide yourself and say what you decided";
                            } else {
                                // The question stands in the tool call above the dialog; the title
                                // stays fixed, since it is what the host reads.
                                const answer = await ctx.ui.input(TITLE, String(params.question));
                                text = answer === undefined || answer === null || answer === ""
                                    ? "no answer"
                                    : String(answer);
                            }
                            return { content: [{ type: "text", text }], details: {} };
                        },
                    });
                }
                """
                .formatted(Json.write(TITLE));
    }
}
