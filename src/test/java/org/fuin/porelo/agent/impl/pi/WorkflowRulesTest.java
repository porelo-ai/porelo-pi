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
