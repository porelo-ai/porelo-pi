package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * The tests that run a workflow step or a command end when it hangs, rather than hanging the suite
 * with it.
 */
class ProcessesTest {

    @Test
    void aStepThatHangsFailsAtItsLimit() {

        // Read to its end before the wait, a pipe holds the test until the step ends by itself.
        assertTimeoutPreemptively(
                Duration.ofSeconds(30),
                () ->
                        assertThatThrownBy(
                                        () ->
                                                Processes.run(
                                                        new ProcessBuilder(
                                                                "bash",
                                                                "-c",
                                                                "echo started; sleep 120"),
                                                        Duration.ofSeconds(2)))
                                .as("a step sleeping past its limit")
                                .isInstanceOf(AssertionError.class)
                                .hasMessageContaining("did not end within PT2S")
                                .hasMessageContaining("started"));
    }

    @Test
    void aStepThatEndsGivesItsOutputAndExit() throws Exception {

        final Processes.Result result =
                Processes.run(
                        new ProcessBuilder("bash", "-c", "echo out; echo err >&2; exit 3"),
                        Duration.ofSeconds(10));

        assertThat(result.exit()).as("the step's exit").isEqualTo(3);
        assertThat(result.output()).as("both streams").contains("out", "err");
    }
}
