package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs {@code buildtools/follow-upstream.sh} in a scratch repository against a stand-in for
 * Porelo's release tool, as the update job runs it.
 */
class FollowUpstreamTest {

    @Test
    void theAcceptanceIsToldThePinnedPiWhenOnlyATreePinMoved(@TempDir final Path dir)
            throws Exception {

        // Measured on 2026-10-18: a run that moved only Node told the acceptance no Pi version,
        // so its version scenario was skipped and the request still called the run green.
        final Path stubs = dir.resolve("stubs");
        WorkflowSteps.stub(
                stubs,
                "release",
                """
                if [ "$1" = upstream-version ]; then
                    case "$*" in
                        *"--pin node"*) printf 'pinned=22.20.0\\nupstream=22.23.3\\nupdate=yes\\n' ;;
                        *--channel*) printf 'pinned=0.85.0\\nupstream=1.3.0\\nupdate=no\\nwaiting=later\\n' ;;
                        *) printf 'pinned=1.0.0\\nupdate=no\\n' ;;
                    esac
                else
                    echo moved >> pom.xml
                fi
                """);
        final Path repo = Files.createDirectories(dir.resolve("repo/buildtools"));
        Files.copy(Path.of("buildtools/follow-upstream.sh"), repo.resolve("follow-upstream.sh"));
        final Path root = repo.getParent();
        Files.writeString(
                root.resolve("pom.xml"), "<agent.cli.version>0.85.0</agent.cli.version>\n");
        Files.writeString(root.resolve(".gitignore"), "target\nstep.sh\n");

        final Map<String, String> env = new HashMap<>();
        env.put("GIT_CONFIG_GLOBAL", "/dev/null");
        env.put("GIT_CONFIG_NOSYSTEM", "1");
        env.put("GIT_AUTHOR_NAME", "t");
        env.put("GIT_AUTHOR_EMAIL", "t@example.invalid");
        env.put("GIT_COMMITTER_NAME", "t");
        env.put("GIT_COMMITTER_EMAIL", "t@example.invalid");
        env.put("PORELO_RELEASE", stubs.resolve("release").toAbsolutePath().toString());
        env.put("GITHUB_OUTPUT", "");
        final Processes.Result setup =
                WorkflowSteps.run(
                        "git init -q -b main .\ngit add .\ngit commit -q -m base\n",
                        root,
                        stubs,
                        env);
        assertThat(setup.exit()).as("setting up the repository:%n%s", setup.output()).isZero();

        final Processes.Result run =
                WorkflowSteps.run("bash buildtools/follow-upstream.sh\n", root, stubs, env);

        assertThat(run.exit()).as("exit of the script:%n%s", run.output()).isZero();
        assertThat(Files.readAllLines(root.resolve("target/update/outputs")))
                .as("the version the acceptance expects after a run that moved only Node")
                .contains("moved=yes", "cli=0.85.0");
    }
}
