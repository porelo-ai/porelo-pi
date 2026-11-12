package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import org.fuin.porelo.agent.api.Waiting;
import org.junit.jupiter.api.Test;

/**
 * The waiting declaration read against the screens it was written from.
 *
 * <p>The open dialog is a screen the acceptance suite's run captured in a task. The prompt after
 * the answer and the question in plain text were measured with Pi 1.5.0 at 80 by 24 outside a task,
 * with the working-directory line and footer of a task; the quoted title is laid out as the open
 * dialog, with the empty editor in its place. Whether the pinned Pi still draws these is the
 * acceptance suite's question, not this one's.
 */
class WaitingDeclarationTest {

    /** Pi's footer in the captured task; its cache counters are not issue numbers. */
    private static final String TASK_FOOTER =
            "↑2.7k ↓18 R704 CH20.8% $0.000 0.3%/1.0M (auto)                             z-ai/glm-5.3-flash • high\n"; // not-a-citation

    /**
     * The ask tool's dialog, open, in a task: the bottom of what Pi 1.5.0 drew at 100 by 39 in the
     * acceptance suite's run of 2026-11-12, the model having called the tool. Inside a task Pi
     * draws rules around the dialog, which puts its title on the sixth non-empty line from the
     * bottom.
     */
    static final String ASKING =
            """
             Use your ask_person tool to ask me whether I prefer red or blue. Do nothing else.



             ask_person question="Do you prefer red or blue?"








            ────────────────────────────────────────────────────────────────────────────────────────────────────

             Pi asks you

            >

             enter submit  escape/ctrl+c cancel

            ────────────────────────────────────────────────────────────────────────────────────────────────────
            /workspace (main)
            """
                    + TASK_FOOTER;

    /** After the answer: the dialog is gone and Pi is back at its prompt. */
    static final String ANSWERED =
            """

             answer: use main




            /workspace
            0.2%/1.0M (auto)                                       z-ai/glm-5.3-flash • high
            """;

    /**
     * A question written as plain text: nothing but the words tells it from any other last line,
     * which is why the declaration reads it as not waiting.
     */
    private static final String QUESTION_IN_TEXT =
            """

             Which branch should I use for the fix?




            /workspace
            0.2%/1.0M (auto)                                       z-ai/glm-5.3-flash • high
            """;

    /**
     * A resting screen whose conversation quotes the dialog's title, as it would in work on this
     * package, laid out as {@link #ASKING} with the empty editor in place of the dialog: the title
     * sits above the lines the declaration reads. A quote in the last three lines of an answer
     * would still read as waiting until the next turn.
     */
    private static final String TITLE_QUOTED_ABOVE =
            """

             The extension opens a dialog titled "Pi asks you" and returns the
             person's answer to the model; pi.yaml reads that title as waiting.
             The unit tests read it against screens measured in a task.
             I changed nothing else.




            ────────────────────────────────────────────────────────────────────────────────────────────────────

            ────────────────────────────────────────────────────────────────────────────────────────────────────
            /workspace (main)
            """
                    + TASK_FOOTER;

    private Waiting waiting() {
        final Waiting waiting = new PiAgent().definition().waiting();
        assertThat(waiting).as("pi.yaml declares what waiting looks like").isNotNull();
        return waiting;
    }

    @Test
    void readsItsAskDialogAsWaitingForAnAnswer() {

        final Waiting.Reading reading = waiting().read(ASKING);

        assertThat(reading.seen()).isEqualTo(Waiting.Seen.WAITING);
        assertThat(reading.waitingFor()).isEqualTo("an answer to its question");
    }

    @Test
    void readsItsPromptAfterTheAnswerAsNotWaiting() {

        // The other half: a rule that matched everything would pass the test above.
        assertThat(waiting().read(ANSWERED).seen()).isEqualTo(Waiting.Seen.NOT_WAITING);
    }

    @Test
    void readsAQuestionInPlainTextAsNotWaiting() {

        // The operator's decision of 2026-11-11: only the tool's dialog is waiting.
        assertThat(waiting().read(QUESTION_IN_TEXT).seen()).isEqualTo(Waiting.Seen.NOT_WAITING);
    }

    @Test
    void readsTheTitleQuotedInTheConversationAsNotWaiting() {

        assertThat(waiting().read(TITLE_QUOTED_ABOVE).seen()).isEqualTo(Waiting.Seen.NOT_WAITING);
    }

    @Test
    void isAtRestWhereTheConversationQuotesTheTitle() {

        // A wake reads the last 20 lines, so a lacks on the title would hold every wake back
        // until the quote scrolled away.
        final var atRest = new PiAgent().definition().atRest();
        assertThat(atRest).isNotNull();
        assertThat(atRest.matches(TITLE_QUOTED_ABOVE)).isTrue();
    }

    @Test
    void isNotAtRestWhileItsDialogIsOpen() {

        // The footer stays under the dialog, so its "(auto)" alone would let a wake line be typed
        // into the answer.
        final var atRest = new PiAgent().definition().atRest();
        assertThat(atRest).isNotNull();
        assertThat(atRest.matches(ASKING)).as("at rest with the dialog open").isFalse();
        assertThat(atRest.matches(ANSWERED)).as("at rest after the answer").isTrue();
    }
}
