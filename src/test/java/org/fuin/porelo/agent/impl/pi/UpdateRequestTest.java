package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs the update job's last two steps, which open the pull request and may merge it, as they run
 * after a step before them failed or was killed: such a step writes no STOP, and the request must
 * still not call the update green, nor may it be merged.
 */
class UpdateRequestTest {

    private static final String REQUEST = "Open the pull request";

    private static final String MERGE = "Merge, or say why not";

    /**
     * The extension of the files the steps write, spelled apart: these are files of the steps, not
     * documents of the repository a documents test reads.
     */
    private static final String MARKDOWN = "m" + "d";

    /** The body the request step writes into the runner's temporary directory. */
    private static final String BODY = "body." + MARKDOWN;

    @Test
    void aStepThatDidNotSucceedIsNeitherGreenNorMerged(@TempDir final Path dir) throws Exception {

        final Path stubs = stubs(dir);
        final Processes.Result request = run(dir, stubs, REQUEST, "failure");

        assertThat(request.exit()).as("exit of the request:%n%s", request.output()).isZero();
        assertThat(Files.readString(dir.resolve(BODY)))
                .as("the request's body after a failed step")
                .doesNotContain("Green")
                .contains("This is not ready to publish");

        final Processes.Result merge = run(dir, stubs, MERGE, "failure");
        assertThat(WorkflowSteps.calls(stubs, "gh"))
                .as("gh calls after a failed step:%n%s", merge.output())
                .noneMatch(call -> call.startsWith("pr merge"));
    }

    @Test
    void aJobThatSucceededIsGreenAndMerged(@TempDir final Path dir) throws Exception {

        // The same steps on a job that succeeded, so the refusal above is the failure's.
        final Path stubs = stubs(dir);
        final Processes.Result request = run(dir, stubs, REQUEST, "success");

        assertThat(request.exit()).as("exit of the request:%n%s", request.output()).isZero();
        assertThat(Files.readString(dir.resolve(BODY))).contains("**Green.**");

        final Processes.Result merge = run(dir, stubs, MERGE, "success");
        assertThat(merge.exit()).as("exit of the merge:%n%s", merge.output()).isZero();
        assertThat(WorkflowSteps.calls(stubs, "gh"))
                .as("gh calls after a job that succeeded")
                .contains("pr merge --merge --delete-branch update/pi");
    }

    @Test
    void thePushGetsTheTokenThroughItsEnvironmentOnly(@TempDir final Path dir) throws Exception {

        // The checkout keeps no token, so only this step may push, and the token on a command line
        // would be readable by every process of the runner.
        final Path stubs = stubs(dir);
        final Processes.Result request = run(dir, stubs, REQUEST, "success");

        assertThat(request.exit()).as("exit of the request:%n%s", request.output()).isZero();
        assertThat(WorkflowSteps.calls(stubs, "git"))
                .as("git's command lines")
                .contains("push --force origin update/pi")
                .noneMatch(call -> call.contains("a-token") || call.contains(BASIC));
        assertThat(Files.readAllLines(stubs.resolve("git.header")))
                .as("the header the push was given")
                .containsExactly(
                        "http.https://github.com/.extraheader|AUTHORIZATION: basic " + BASIC);
        assertThat(request.output()).as("the step's log").doesNotContain("a-token");
    }

    @Test
    void aBranchLeftByAClosedRequestGetsAFreshRequest(@TempDir final Path dir) throws Exception {

        // Measured on 2026-10-18: update/pi still held the closed #4, so the run edited and
        // commented that closed request, opened none, and its green result reached nobody. Moving
        // that branch would also move the older main's workflows, which GitHub refuses the token.
        final Path stubs = stubs(dir);
        WorkflowSteps.stub(stubs, "gh", "exit 0");
        final Processes.Result request = run(dir, stubs, REQUEST, "success");

        assertThat(request.exit()).as("exit of the request:%n%s", request.output()).isZero();
        assertThat(WorkflowSteps.calls(stubs, "gh"))
                .as("gh calls with a closed request on the branch")
                .contains("api -X DELETE repos/porelo-ai/porelo-pi/git/refs/heads/update/pi")
                .anyMatch(call -> call.startsWith("pr create"))
                .noneMatch(call -> call.startsWith("pr edit") || call.startsWith("pr comment"));
    }

    /** {@code x-access-token:a-token} in base64, as GitHub takes a token over https. */
    private static final String BASIC =
            java.util.Base64.getEncoder()
                    .encodeToString("x-access-token:a-token".getBytes(StandardCharsets.UTF_8));

    /** A {@code gh} that finds no open request, and a {@code git} that does nothing. */
    private static Path stubs(final Path dir) throws Exception {
        final Path stubs = dir.resolve("stubs");
        WorkflowSteps.stub(stubs, "gh", "[ \"$1 $2\" = \"pr view\" ] && exit 1\nexit 0");
        // Records the header git would send, so a test can tell what reached git and how.
        WorkflowSteps.stub(
                stubs,
                "git",
                "[ \"$1\" = push ] && printf '%s|%s\\n' \"${GIT_CONFIG_KEY_0:-}\" \"${GIT_CONFIG_VALUE_0:-}\""
                        + " >> \""
                        + stubs.resolve("git.header")
                        + "\"\nexit 0");
        Files.createDirectories(dir.resolve("target/update"));
        Files.writeString(
                dir.resolve("target/update").resolve("summary." + MARKDOWN), "Pi 1.0.0 -> 1.0.1\n");
        Files.writeString(dir.resolve("output"), "");
        return stubs;
    }

    /** Runs the one step of that name in update.yml, as GitHub does with this job status. */
    private static Processes.Result run(
            final Path dir, final Path stubs, final String name, final String status)
            throws Exception {
        final List<String> steps = WorkflowSteps.scripts("update.yml", name);
        assertThat(steps).as("the step \"%s\" in update.yml", name).hasSize(1);
        final Map<String, String> env = new HashMap<>();
        env.put("GH_TOKEN", "a-token");
        env.put("TITLE", "Move Pi to 1.0.1");
        env.put("RUN_NUMBER", "7");
        env.put("GITHUB_REPOSITORY", "porelo-ai/porelo-pi");
        env.put("GITHUB_SHA", "the-checked-out-commit");
        env.put("GITHUB_RUN_NUMBER", "7");
        env.put("RUNNER_TEMP", dir.toString());
        env.put("GITHUB_OUTPUT", dir.resolve("output").toString());
        env.put("JOB_STATUS", status);
        env.put("AUTO", "true");
        env.put("HAVE_TOKEN", "true");
        // No step before wrote STOP: a killed one writes nothing.
        env.put("STOP", "");
        return WorkflowSteps.run(steps.get(0), dir, stubs, env);
    }
}
