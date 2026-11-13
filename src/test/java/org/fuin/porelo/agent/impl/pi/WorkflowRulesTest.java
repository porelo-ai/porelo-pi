package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkflowRulesTest {

    private static final Path WORKFLOWS = RepositoryDocuments.root().resolve(".github/workflows");

    /** A Maven call that activates the {@code dist} profile, which builds the packages. */
    private static final Pattern PACKAGE_BUILD =
            Pattern.compile("(-P|--activate-profiles)[\\s=]*(\\S*,)?dist\\b");

    /** What skips the tests, and with them the checks of what was built. */
    private static final Pattern SKIPS_TESTS =
            Pattern.compile("-DskipTests\\b|-Dmaven\\.test\\.skip\\b");

    @Test
    void noPackageBuildSkipsTheTests() throws IOException {

        // -DskipTests also skips NativeLinkageCheck and the built-tree checks, the ones that need
        // what was built.
        final List<String> builds = new ArrayList<>();
        try (Stream<Path> files = Files.list(WORKFLOWS)) {
            for (final Path file :
                    files.filter(f -> f.toString().endsWith(".yml")).sorted().toList()) {
                for (final String command : mavenCommands(Files.readAllLines(file))) {
                    if (PACKAGE_BUILD.matcher(command).find()) {
                        builds.add(file.getFileName() + ": " + command);
                    }
                }
            }
        }

        // A scan that finds no build passes forever: the builds it must see, then what it checks.
        assertThat(builds.stream().map(build -> build.substring(0, build.indexOf(':'))))
                .as("the package builds found: %s", builds)
                .containsExactly("build.yml", "update.yml");
        assertThat(builds)
                .as("package builds that skip their tests")
                .noneMatch(build -> SKIPS_TESTS.matcher(build).find());
    }

    @Test
    void seesAPackageBuildInEveryShapeItCanBeWritten() {

        final List<String> commands =
                mavenCommands(
                        List.of(
                                "              run: ./mvnw -B -P native,dist verify",
                                "                  -DskipTests",
                                "",
                                "                  ./mvnw -B -Pdist,native \\",
                                "              -Dmaven.test.skip=true verify",
                                "                  ./mvnw -B --activate-profiles=native,dist verify",
                                "                  ./mvnw -B -Pci-tools dependency:build-classpath",
                                "                  echo distant"));

        assertThat(commands)
                .as("the Maven calls, each with its folded and continued lines")
                .hasSize(4);
        assertThat(commands.stream().filter(c -> PACKAGE_BUILD.matcher(c).find()))
                .as("the package builds among them")
                .hasSize(3);
        assertThat(
                        commands.stream()
                                .filter(c -> PACKAGE_BUILD.matcher(c).find())
                                .filter(c -> SKIPS_TESTS.matcher(c).find()))
                .as("the package builds that skip their tests")
                .hasSize(2);
    }

    @Test
    void aLegInstallsWhatItsRunBuiltUnlessThatRunPublishedIt(@TempDir final Path dir)
            throws Exception {

        // Only a push to main publishes; any other run that took the published package would
        // test what was published last against this commit's features, and its green would
        // prove nothing about the commit.
        final List<String> steps =
                WorkflowSteps.scripts("build.yml", "Acceptance on ${{ matrix.os }}");
        assertThat(steps).as("the acceptance step of build.yml").hasSize(1);
        Files.writeString(dir.resolve("pom.xml"), "<agent.cli.version>1.0.0</agent.cli.version>\n");
        Files.createDirectories(dir.resolve("target"));
        Files.writeString(dir.resolve("target/cp.txt"), "cp");
        final Path stubs = dir.resolve("stubs");
        WorkflowSteps.stub(stubs, "java", "exit 0");

        assertThat(
                        legCall(
                                steps.get(0),
                                dir,
                                stubs,
                                "workflow_dispatch",
                                "refs/heads/main",
                                "true"))
                .as("a dispatch on main, which publishes nothing")
                .contains("--candidate candidate");
        assertThat(legCall(steps.get(0), dir, stubs, "push", "refs/tags/v1.0.0", "true"))
                .as("a tag, published only after its legs")
                .contains("--candidate candidate");
        assertThat(legCall(steps.get(0), dir, stubs, "push", "refs/heads/main", "false"))
                .as("a push to main on a forge that publishes nothing")
                .contains("--candidate candidate");
        assertThat(legCall(steps.get(0), dir, stubs, "push", "refs/heads/main", "true"))
                .as("a push to main that published its snapshot")
                .contains("--package porelo-agent-pi")
                .doesNotContain("--candidate");
    }

    /** Runs the acceptance leg for one trigger and returns how it called the machine tooling. */
    private static String legCall(
            final String leg,
            final Path dir,
            final Path stubs,
            final String event,
            final String ref,
            final String publish)
            throws Exception {
        final Processes.Result run =
                WorkflowSteps.run(
                        leg,
                        dir,
                        stubs,
                        Map.of(
                                "PORELO_E2E_OPENROUTER_API_KEY",
                                "a-key",
                                "OS",
                                "ubuntu",
                                "GITHUB_EVENT_NAME",
                                event,
                                "GITHUB_REF",
                                ref,
                                "GITHUB_REF_TYPE",
                                ref.startsWith("refs/tags/") ? "tag" : "branch",
                                "PORELO_PUBLISH",
                                publish));
        assertThat(run.exit())
                .as("exit of the leg on %s %s:%n%s", event, ref, run.output())
                .isZero();
        final List<String> calls = WorkflowSteps.calls(stubs, "java");
        assertThat(calls).as("the machine tooling, called by the leg").isNotEmpty();
        return calls.getLast();
    }

    @Test
    void anEmptyProviderKeyStopsTheBuildsAcceptanceLeg(@TempDir final Path dir) throws Exception {

        // Without it the credential scenarios skip, and a leg passes on less than it claims.
        final List<String> steps =
                WorkflowSteps.scripts("build.yml", "Acceptance on ${{ matrix.os }}");
        assertThat(steps).as("the acceptance step of build.yml").hasSize(1);
        Files.createDirectories(dir.resolve("target"));
        Files.writeString(dir.resolve("target/cp.txt"), "cp");
        // The leg reads the pin it tells the suite to expect from the pom it is run beside.
        Files.writeString(dir.resolve("pom.xml"), "<agent.cli.version>1.0.0</agent.cli.version>\n");
        final Path stubs = dir.resolve("stubs");
        WorkflowSteps.stub(stubs, "java", "exit 0");

        final Processes.Result empty = WorkflowSteps.run(steps.get(0), dir, stubs, leg(""));

        assertThat(WorkflowSteps.calls(stubs, "java"))
                .as("acceptance runs with an empty key:%n%s", empty.output())
                .isEmpty();
        assertThat(empty.exit()).as("exit with an empty key:%n%s", empty.output()).isNotZero();
        assertThat(empty.output()).contains("OPEN_ROUTER_API_KEY is empty");

        // With a key the same step reaches the acceptance run, so the stop above is the key's.
        final Processes.Result set = WorkflowSteps.run(steps.get(0), dir, stubs, leg("a-key"));
        assertThat(set.exit()).as("exit with a key:%n%s", set.output()).isZero();
        assertThat(WorkflowSteps.calls(stubs, "java")).as("acceptance runs with a key").hasSize(1);
    }

    @Test
    void anEmptyProviderKeyStopsTheUpdate(@TempDir final Path dir) throws Exception {

        final List<String> steps = WorkflowSteps.scripts("update.yml", "The provider key is there");
        assertThat(steps).as("the key's step in update.yml").hasSize(1);
        final Path env = dir.resolve("env");

        Files.writeString(env, "");
        final Processes.Result empty =
                WorkflowSteps.run(
                        steps.get(0),
                        dir,
                        dir.resolve("stubs"),
                        Map.of("KEY", "", "GITHUB_ENV", env.toString()));
        assertThat(empty.exit()).as("exit:%n%s", empty.output()).isZero();
        assertThat(Files.readString(env))
                .as("what an empty key leaves for the steps after it")
                .startsWith("STOP=")
                .contains("OPEN_ROUTER_API_KEY is empty");

        Files.writeString(env, "");
        final Processes.Result set =
                WorkflowSteps.run(
                        steps.get(0),
                        dir,
                        dir.resolve("stubs"),
                        Map.of("KEY", "a-key", "GITHUB_ENV", env.toString()));
        assertThat(set.exit()).as("exit:%n%s", set.output()).isZero();
        assertThat(Files.readString(env)).as("what a key leaves for the steps after it").isEmpty();
    }

    @Test
    void theAcceptanceLegsRunOnEveryForge() throws IOException {

        // On the host's forge a leg takes a machine of its kind from the host's pool, told where by
        // the forge; a condition naming GitHub's server would leave the host's forge untested until
        // the gathered push.
        final List<String> lines = Files.readAllLines(WORKFLOWS.resolve("build.yml"));
        final int start = lines.indexOf("    acceptance:");
        assertThat(start).as("the acceptance job in build.yml").isNotNegative();
        int end = start + 1;
        while (end < lines.size() && !lines.get(end).matches("^ {4}[a-z].*:$")) {
            end++;
        }
        assertThat(lines.subList(start, end))
                .as("the acceptance job names no forge's server")
                .noneMatch(line -> line.contains("github.server_url"));

        assertThat(step(lines, "Acceptance on ${{ matrix.os }}"))
                .as("the leg is given the pool's run key, empty where there is no pool")
                .anyMatch(
                        line ->
                                line.trim()
                                        .equals("PORELO_POOL_SSH: ${{ secrets.PORELO_POOL_SSH }}"));

        // GitHub's artifact actions refuse every other server, so the packages travel through the
        // cache, saved and restored under the condition the leg passes --candidate on.
        final String key = "key: candidate-${{ github.run_id }}-${{ github.run_attempt }}";
        final String when =
                "if: \"!(vars.PORELO_PUBLISH == 'true' && github.event_name == 'push'"
                        + " && github.ref == 'refs/heads/main')\"";
        assertThat(lines)
                .as("the packages are copied, saved and restored under one condition")
                .filteredOn(line -> line.trim().replaceFirst("^- ", "").equals(when))
                .hasSize(3);
        assertThat(lines)
                .as("the build job saves the packages under the key the legs restore")
                .filteredOn(line -> line.trim().equals(key))
                .hasSize(2);
        assertThat(step(lines, "The packages this run built"))
                .map(String::trim)
                .contains(
                        when,
                        key,
                        "restore-keys: candidate-${{ github.run_id }}-",
                        "fail-on-cache-miss: true")
                .noneMatch(line -> line.contains("download-artifact"));
    }

    /** The lines of the named step, up to the next step or the next job. */
    private static List<String> step(final List<String> lines, final String name) {
        final int start = lines.indexOf("            - name: " + name);
        assertThat(start).as("the step %s", name).isNotNegative();
        int end = start + 1;
        while (end < lines.size()
                && !lines.get(end).startsWith("            - ")
                && !lines.get(end).matches("^ {0,8}\\S.*")) {
            end++;
        }
        return lines.subList(start, end);
    }

    /** The environment the acceptance step of a branch's run has, with the given key. */
    private static Map<String, String> leg(final String key) {
        return Map.of(
                "PORELO_E2E_OPENROUTER_API_KEY", key, "GITHUB_REF_TYPE", "branch", "OS", "ubuntu");
    }

    /**
     * Returns each Maven call with the lines it continues onto: after a backslash, or folded onto
     * lines indented deeper than the one it starts on.
     */
    private static List<String> mavenCommands(final List<String> lines) {
        final List<String> commands = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).contains("mvnw")) {
                continue;
            }
            final int indent = indentOf(lines.get(i));
            final StringBuilder command = new StringBuilder(lines.get(i).strip());
            int j = i + 1;
            while (j < lines.size()
                    && !lines.get(j).isBlank()
                    && (lines.get(j - 1).endsWith("\\") || indentOf(lines.get(j)) > indent)) {
                command.append(' ').append(lines.get(j).strip());
                j++;
            }
            commands.add(command.toString());
            i = j - 1;
        }
        return commands;
    }

    private static int indentOf(final String line) {
        return line.length() - line.stripLeading().length();
    }
}
