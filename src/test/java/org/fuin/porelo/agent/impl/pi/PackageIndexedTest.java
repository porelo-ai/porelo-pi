package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs both "The package is indexed" steps of the build workflow against a stubbed index: a publish
 * is believed only once the index shows the exact version built, and an index that still shows only
 * an older one must fail the step instead of reporting it published.
 */
class PackageIndexedTest {

    private static final String STEP = "The package is indexed, not merely stored";

    private static final String VERSION = "0.5.1~snapshot.7";

    @Test
    void anIndexWithOnlyAnOlderVersionFails(@TempDir final Path dir) throws Exception {

        final String older =
                "Package: porelo-agent\nVersion: "
                        + VERSION
                        + "\n\n"
                        + "Package: porelo-agent-pi\nVersion: 0.5.1~snapshot.6\n";
        for (final Processes.Result run : run(dir, older)) {
            assertThat(run.exit())
                    .as("exit with only an older version indexed:%n%s", run.output())
                    .isEqualTo(1);
            assertThat(run.output()).contains("never appeared in the index");
        }
    }

    @Test
    void anIndexWithTheVersionBuiltPasses(@TempDir final Path dir) throws Exception {

        // So the failure above is the version's, not the stubs'.
        final String index =
                "Package: porelo-agent-pi\nVersion: 0.5.1~snapshot.6\n\n"
                        + "Package: porelo-agent-pi\nVersion: "
                        + VERSION
                        + "\n";
        for (final Processes.Result run : run(dir, index)) {
            assertThat(run.exit())
                    .as("exit with the version built indexed:%n%s", run.output())
                    .isZero();
        }
    }

    /** Runs each step in a directory of its own, the index served by a {@code curl} stub. */
    private static List<Processes.Result> run(final Path dir, final String index) throws Exception {
        final List<String> steps = WorkflowSteps.scripts("build.yml", STEP);
        assertThat(steps)
                .as("the index steps of build.yml, the build job's and the release job's")
                .hasSize(2);
        final List<Processes.Result> runs = new java.util.ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            final Path work = dir.resolve("step" + i);
            Files.createDirectories(work.resolve("target"));
            Files.writeString(work.resolve("target/porelo-agent-pi_" + VERSION + "_amd64.deb"), "");
            final Path stubs = work.resolve("stubs");
            WorkflowSteps.stub(stubs, "curl", "printf '%s' \"$INDEX\"");
            WorkflowSteps.stub(
                    stubs, "dpkg-deb", "[ \"$1 $3\" = \"-f Version\" ] && echo '" + VERSION + "'");
            // Twenty tries six seconds apart would make a test of two minutes.
            WorkflowSteps.stub(stubs, "sleep", "exit 0");
            runs.add(
                    WorkflowSteps.run(
                            steps.get(i),
                            work,
                            stubs,
                            Map.of(
                                    "INDEX",
                                    index,
                                    "CHANNEL",
                                    "snapshots",
                                    "JF_URL",
                                    "https://artifactory.invalid")));
        }
        return runs;
    }
}
