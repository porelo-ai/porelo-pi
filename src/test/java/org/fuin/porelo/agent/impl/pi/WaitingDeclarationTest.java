package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import org.fuin.porelo.agent.api.Waiting;
import org.junit.jupiter.api.Test;

/**
 * The waiting declaration read against the screens it was written from.
 *
 * <p>Each screen is the bottom of what Pi 1.5.0 drew at an 80 by 24 terminal with the ask extension
 * this package installs: its dialog open, the prompt after it was answered, and the prompt with a
 * question as plain text above it. The working-directory line and the footer are those measured in
 * a task, since the capture ran outside one. Pi draws the dialog's title on a line of its own at
 * any width, so the narrowest terminal is the one that tests the region. Whether the pinned Pi
 * still draws these is the acceptance suite's question, not this one's.
 */
class WaitingDeclarationTest {

    /** The ask tool's dialog, open: its title, the empty answer line and the dialog's keys. */
    static final String ASKING =
            """

             Pi asks you

            >

             enter submit  escape/ctrl+c cancel



            /workspace
            0.2%/1.0M (auto)                                       z-ai/glm-5.3-flash • high
            """;

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
    void isNotAtRestWhileItsDialogIsOpen() {

        // The footer stays under the dialog, so its "(auto)" alone would let a wake line be typed
        // into the answer.
        final var atRest = new PiAgent().definition().atRest();
        assertThat(atRest).isNotNull();
        assertThat(atRest.matches(ASKING)).as("at rest with the dialog open").isFalse();
        assertThat(atRest.matches(ANSWERED)).as("at rest after the answer").isTrue();
    }
}
