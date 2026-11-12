package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import org.fuin.porelo.agent.api.Waiting;
import org.junit.jupiter.api.Test;

/**
 * Pi declares no screen rule to read waiting by, and that is a measurement, not an omission.
 *
 * <p>On 0.85.0, captured at a terminal, a question it puts is plain text on its idle screen. A rule
 * written without measuring would match nothing and make Porelo say "not waiting" where the true
 * answer is "cannot say", and let Porelo type a wake line into a question nobody can see is open.
 * Its {@code waiting} carries only the last message of a finished run. This fails when a screen
 * rule appears, so whoever adds one also replaces the acceptance scenario that proves "cannot say"
 * with one that drives Pi to a question.
 */
class WaitingDeclarationTest {

    @Test
    void declaresNoScreenRuleToReadWaitingBy() {

        final Waiting waiting = new PiAgent().definition().waiting();
        assertThat(waiting)
                .as("pi.yaml declares the last message under session.waiting")
                .isNotNull();
        assertThat(waiting.screen()).as("no rule reads Pi's screen as waiting").isEmpty();
        assertThat(waiting.notItsScreen()).as("no rule reads a screen over Pi's").isEmpty();
    }
}
