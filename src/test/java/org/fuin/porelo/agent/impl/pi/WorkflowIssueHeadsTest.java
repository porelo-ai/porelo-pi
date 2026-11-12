package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The daily check of the issue heads: an issue head or index that breaks the rules would otherwise
 * turn nothing red until the next push, and a [skip ci] push of issues alone starts no Build.
 */
class WorkflowIssueHeadsTest {

    private static final String STEP = "Every issue head holds to its schema";

    @Test
    void theStepHoldsTheIssueHeads(@TempDir final Path dir) throws Exception {

        final Processes.Result run = runStep(dir, "0");

        assertThat(run.exit()).as("exit of the step: %s", run.output()).isZero();
        assertThat(Files.readAllLines(dir.resolve("bin/java.calls")))
                .as("what the step asks the release tooling, on the resolved classpath")
                .containsExactly(
                        "-cp unused-classpath org.fuin.porelo.release.Main issues check issues");
    }

    @Test
    void aHeadTheToolingRefusesFailsTheStep(@TempDir final Path dir) throws Exception {

        final Processes.Result run = runStep(dir, "1");

        assertThat(run.exit()).as("exit of the step: %s", run.output()).isEqualTo(1);
    }

    @Test
    void theStepRunsOnEveryTrigger() throws Exception {

        // A daily run, a pull request and a dispatch alike: an if: would leave one of them out.
        final List<String> lines =
                Files.readAllLines(
                        RepositoryDocuments.root().resolve(".github/workflows/shared-rules.yml"));
        final int start = lines.indexOf("            - name: " + STEP);
        assertThat(start).as("the step in shared-rules.yml").isNotNegative();
        int end = start + 1;
        while (end < lines.size() && !lines.get(end).startsWith("            - ")) {
            end++;
        }

        assertThat(lines.subList(start, end))
                .noneMatch(
                        line ->
                                line.trim().startsWith("if:")
                                        || line.trim().startsWith("continue-on-error:"));
    }

    private static Processes.Result runStep(final Path dir, final String exit) throws Exception {
        final Path bin = dir.resolve("bin");
        WorkflowSteps.stub(bin, "java", "exit \"$CHECK_EXIT\"");
        Files.createDirectory(dir.resolve("target"));
        Files.writeString(dir.resolve("target/cp.txt"), "unused-classpath");
        final List<String> scripts = WorkflowSteps.scripts("shared-rules.yml", STEP);
        assertThat(scripts).as("the issue heads' step in shared-rules.yml").hasSize(1);
        return WorkflowSteps.run(scripts.getFirst(), dir, bin, Map.of("CHECK_EXIT", exit));
    }
}
