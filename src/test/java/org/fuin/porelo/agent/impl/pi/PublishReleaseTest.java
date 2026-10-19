package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs both "Publish the packages" steps of the build workflow on a release, with {@code curl}
 * stubbed and the repository's own {@code artifactory.sh} beside them: a published release is never
 * replaced, and a question about it that gets no answer must stop the upload rather than read as
 * "not published".
 */
class PublishReleaseTest {

    private static final String DEB = "porelo-agent-pi_0.5.0_amd64.deb";

    private static final String RPM = "porelo-agent-pi-0.5.0-1.x86_64.rpm";

    /**
     * {@code curl}: an upload ({@code -T}, as {@code art_put} sends one) accepted, the deb index
     * from {@code $INDEX}, the rpm's storage status from {@code $RPM_CODE}.
     */
    private static final String CURL =
            """
            [ -n "${CURL_EXIT:-}" ] && exit "$CURL_EXIT"
            out=/dev/stdout
            url=
            upload=
            while [ $# -gt 0 ]; do
                case "$1" in
                    -o) out="$2"; shift ;;
                    -T) upload="$2"; shift ;;
                    http*) url="$1" ;;
                esac
                shift
            done
            [ -n "$upload" ] && exit 0
            case "$url" in
                */Packages) printf '%s' "$INDEX" > "$out" ;;
                */api/storage/*) printf '%s' "$RPM_CODE" ;;
                *) echo "unexpected url $url" >&2; exit 2 ;;
            esac
            """;

    @Test
    void aFailedFetchStopsTheUpload(@TempDir final Path dir) throws Exception {

        for (final Run run : run(dir, Map.of("CURL_EXIT", "22"))) {
            assertThat(run.uploads())
                    .as("uploads after a failed fetch:%n%s", run.output())
                    .isEmpty();
            assertThat(run.exit()).as("exit after a failed fetch").isNotZero();
        }
    }

    @Test
    void aDebInTheIndexIsNotReplaced(@TempDir final Path dir) throws Exception {

        final String index =
                "Package: porelo-agent-pi\nFilename: pool/main/p/porelo-agent-pi/" + DEB + "\n";
        for (final Run run : run(dir, Map.of("INDEX", index, "RPM_CODE", "404"))) {
            assertThat(run.uploads()).as("uploads of a published deb:%n%s", run.output()).isEmpty();
            assertThat(run.output()).contains("published already");
        }
    }

    @Test
    void aStoredRpmIsNotReplaced(@TempDir final Path dir) throws Exception {

        for (final Run run : run(dir, Map.of("INDEX", "", "RPM_CODE", "200"))) {
            assertThat(run.uploads()).as("uploads of a published rpm:%n%s", run.output()).isEmpty();
            assertThat(run.output()).contains("published already");
        }
    }

    @Test
    void anAnswerOtherThanNotFoundStopsTheUpload(@TempDir final Path dir) throws Exception {

        for (final Run run : run(dir, Map.of("INDEX", "", "RPM_CODE", "503"))) {
            assertThat(run.uploads()).as("uploads after HTTP 503:%n%s", run.output()).isEmpty();
            assertThat(run.exit()).as("exit after HTTP 503").isNotZero();
        }
    }

    @Test
    void aReleaseNotYetPublishedIsUploaded(@TempDir final Path dir) throws Exception {

        // So the stops above are the answers', not the stubs'.
        final String index =
                "Package: porelo-agent-pi\nFilename: pool/main/p/porelo-agent-pi/porelo-agent-pi_0.4.1_amd64.deb\n";
        for (final Run run : run(dir, Map.of("INDEX", index, "RPM_CODE", "404"))) {
            assertThat(run.exit()).as("exit:%n%s", run.output()).isZero();
            assertThat(run.uploads()).as("uploads").hasSize(4);
        }
    }

    private record Run(int exit, String output, List<String> uploads) {}

    /** Runs each publish step in a directory of its own with the packages and the stubs. */
    private static List<Run> run(final Path dir, final Map<String, String> answers)
            throws Exception {
        final List<String> steps = WorkflowSteps.scripts("build.yml", "Publish the packages");
        assertThat(steps)
                .as("the publish steps of build.yml, the build job's and the release job's")
                .hasSize(2);
        final List<Run> runs = new java.util.ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            final Path work = dir.resolve("step" + i);
            Files.createDirectories(work.resolve("target"));
            for (final String file : List.of(DEB, RPM, "porelo-agent-pi.cdx.json")) {
                Files.writeString(work.resolve("target").resolve(file), "");
            }
            final Path stubs = work.resolve("stubs");
            WorkflowSteps.stub(stubs, "curl", CURL);
            final Path scripts = Files.createDirectories(work.resolve(".github/scripts"));
            Files.copy(
                    Path.of(".github/scripts/artifactory.sh"), scripts.resolve("artifactory.sh"));
            final Map<String, String> env = new HashMap<>(answers);
            env.put("JF_URL", "https://artifactory.invalid");
            env.put("JF_ACCESS_TOKEN", "not-a-token");
            env.put("CHANNEL", "releases");
            final Processes.Result result = WorkflowSteps.run(steps.get(i), work, stubs, env);
            runs.add(
                    new Run(
                            result.exit(),
                            result.output(),
                            WorkflowSteps.calls(stubs, "curl").stream()
                                    .filter(call -> call.contains("-T "))
                                    .toList()));
        }
        return runs;
    }
}
