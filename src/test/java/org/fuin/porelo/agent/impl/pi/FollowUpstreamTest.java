package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
        nodeRelease(
                stubs,
                "*--channel*) printf 'pinned=0.85.0\\nupstream=1.3.0\\nupdate=no\\nwaiting=later\\n' ;;");
        final Path root = scratch(dir, nodeTree("0.85.0", taskImage()));

        final Processes.Result run = follow(root, stubs);

        assertThat(run.exit()).as("exit of the script:%n%s", run.output()).isZero();
        assertThat(Files.readAllLines(root.resolve("target/update/outputs")))
                .as("the version the acceptance expects after a run that moved only Node")
                .contains("moved=yes", "cli=0.85.0");
    }

    @Test
    void aNodeMoveTakesTheTaskImageAlong(@TempDir final Path dir) throws Exception {

        // Measured on 2026-10-27: an update moved Node in pom.xml alone, and the task image kept
        // building Pi's tree with the old Node, so the run stopped at its own pin check.
        final Path stubs = dir.resolve("stubs");
        nodeRelease(stubs, "");
        final Path root = scratch(dir, nodeTree("1.3.0", taskImage()));

        final Processes.Result run = follow(root, stubs);

        assertThat(run.exit()).as("exit of the script:%n%s", run.output()).isZero();
        assertThat(
                        PinAgreementTest.taskImageDisagreements(
                                Files.readString(root.resolve("pom.xml")),
                                Files.readString(root.resolve("porelo.yml"))))
                .as("how the task image's Node differs from the moved pin")
                .isEmpty();
        assertThat(Files.readString(root.resolve("porelo.yml")))
                .as("the task image after the move")
                .contains(
                        "/v22.23.4/node-v22.23.4-linux-x64.tar.gz", NEW_SHA + "  /tmp/node.tar.gz");
        final Processes.Result show =
                WorkflowSteps.run("git show --stat --format=%s HEAD\n", root, stubs, env(stubs));
        assertThat(show.output())
                .as("the commit that pins Node")
                .contains("Pin Node 22.23.4", "porelo.yml", "pom.xml");
    }

    // Moving the pin alone would hand on the very disagreement the move is meant to avoid, and a
    // rewrite of a file it cannot read, or with a digest that is none, writes what nobody checked.
    @ParameterizedTest(name = "{0}")
    @MethodSource("refusedMoves")
    void aNodeMoveItCannotCarryIntoTheTaskImageStops(
            final String why,
            final String image,
            final String version,
            final String digest,
            @TempDir final Path dir)
            throws Exception {

        final Path stubs = dir.resolve("stubs");
        nodeRelease(stubs, "", version, digest);
        final Path root = scratch(dir, nodeTree("1.3.0", image));

        final Processes.Result run = follow(root, stubs);

        assertThat(run.exit()).as("exit of the script:%n%s", run.output()).isZero();
        assertThat(Files.readAllLines(root.resolve("target/update/outputs")))
                .as("the update's verdict when %s", why)
                .contains("moved=no")
                .anySatisfy(
                        line ->
                                assertThat(line)
                                        .startsWith("stop=Node 22.23.4 could not be pinned"));
        final Processes.Result status =
                WorkflowSteps.run("git status --porcelain\n", root, stubs, env(stubs));
        assertThat(status.output())
                .as("the working tree after the refused move, when %s", why)
                .isEmpty();
    }

    static Stream<Arguments> refusedMoves() throws Exception {
        final String image = taskImage();
        final String download = "nodejs.org/dist/v22.23.3/node-v22.23.3-linux-x64.tar.gz";
        return Stream.of(
                Arguments.of(
                        "the task image downloads no Node",
                        image.replace(download, "example.org/node.tar.gz"),
                        "22.23.4",
                        NEW_SHA),
                Arguments.of(
                        "the task image checks Node against no digest",
                        image.replace("\"" + OLD_SHA + "  /tmp/node.tar.gz\"", "\"-\""),
                        "22.23.4",
                        NEW_SHA),
                Arguments.of(
                        "the task image downloads Node twice on one line",
                        image.replace(download, download + " https://" + download),
                        "22.23.4",
                        NEW_SHA),
                Arguments.of("the pom's new digest is no digest", image, "22.23.4", "not-a-digest"),
                Arguments.of("the pom's new version is no version", image, "latest", NEW_SHA));
    }

    /**
     * A release tool that finds Node 22.23.3 pinned and 22.23.4 upstream, moves pom.xml's pin and
     * digest as the real one does, and answers everything else as {@code others} says or as
     * current.
     */
    private static void nodeRelease(final Path stubs, final String others) throws Exception {
        nodeRelease(stubs, others, "22.23.4", NEW_SHA);
    }

    private static void nodeRelease(
            final Path stubs, final String others, final String version, final String digest)
            throws Exception {
        WorkflowSteps.stub(
                stubs,
                "release",
                """
                if [ "$1" = upstream-version ]; then
                    case "$*" in
                        *"--pin node"*) printf 'pinned=22.23.3\\nupstream=22.23.4\\nupdate=yes\\n' ;;
                        %s
                        *) printf 'pinned=1.0.0\\nupdate=no\\n' ;;
                    esac
                else
                    sed -i -e 's/22\\.23\\.3/%s/g' -e 's/%s/%s/' pom.xml
                fi
                """
                        .formatted(others, version, OLD_SHA, digest));
    }

    /**
     * This repository's pom, pinning the given Pi and Node 22.23.3 with the stand-in digest, and
     * the given task image.
     */
    private static Map<String, String> nodeTree(final String cli, final String image)
            throws Exception {
        final String pom = Files.readString(Path.of("pom.xml"));
        return Map.of(
                "pom.xml",
                pom.replace(pinned(pom, "pin.node.version"), "22.23.3")
                        .replace(pinned(pom, "pin.node.sha256"), OLD_SHA)
                        .replace(
                                "<agent.cli.version>" + pinned(pom, "agent.cli.version") + "<",
                                "<agent.cli.version>" + cli + "<"),
                "porelo.yml",
                image);
    }

    /** This repository's task image, its Node set back to 22.23.3 with the stand-in digest. */
    private static String taskImage() throws Exception {
        final String pom = Files.readString(Path.of("pom.xml"));
        return Files.readString(Path.of("porelo.yml"))
                .replace(pinned(pom, "pin.node.version"), "22.23.3")
                .replace(pinned(pom, "pin.node.sha256"), OLD_SHA);
    }

    private static final String OLD_SHA = "a".repeat(64);
    private static final String NEW_SHA = "b".repeat(64);

    /** A scratch repository holding the script and the given files, committed. */
    private static Path scratch(final Path dir, final Map<String, String> files) throws Exception {
        final Path repo = Files.createDirectories(dir.resolve("repo/buildtools"));
        Files.copy(Path.of("buildtools/follow-upstream.sh"), repo.resolve("follow-upstream.sh"));
        final Path root = repo.getParent();
        for (final Map.Entry<String, String> file : files.entrySet()) {
            Files.writeString(root.resolve(file.getKey()), file.getValue());
        }
        Files.writeString(root.resolve(".gitignore"), "target\nstep.sh\n");
        final Path stubs = dir.resolve("stubs");
        final Processes.Result setup =
                WorkflowSteps.run(
                        "git init -q -b main .\ngit add .\ngit commit -q -m base\n",
                        root,
                        stubs,
                        env(stubs));
        assertThat(setup.exit()).as("setting up the repository:%n%s", setup.output()).isZero();
        return root;
    }

    private static Processes.Result follow(final Path root, final Path stubs) throws Exception {
        return WorkflowSteps.run("bash buildtools/follow-upstream.sh\n", root, stubs, env(stubs));
    }

    private static Map<String, String> env(final Path stubs) {
        final Map<String, String> env = new HashMap<>();
        env.put("GIT_CONFIG_GLOBAL", "/dev/null");
        env.put("GIT_CONFIG_NOSYSTEM", "1");
        env.put("GIT_AUTHOR_NAME", "t");
        env.put("GIT_AUTHOR_EMAIL", "t@example.invalid");
        env.put("GIT_COMMITTER_NAME", "t");
        env.put("GIT_COMMITTER_EMAIL", "t@example.invalid");
        env.put("PORELO_RELEASE", stubs.resolve("release").toAbsolutePath().toString());
        env.put("GITHUB_OUTPUT", "");
        return env;
    }

    private static String pinned(final String pom, final String property) {
        final Matcher matcher =
                Pattern.compile("<" + Pattern.quote(property) + ">([^<]+)</").matcher(pom);
        assertThat(matcher.find()).as("pom.xml pins %s", property).isTrue();
        return matcher.group(1);
    }
}
