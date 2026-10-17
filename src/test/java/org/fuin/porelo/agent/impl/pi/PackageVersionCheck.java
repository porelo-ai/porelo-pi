package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Whether the packages the build made carry the project's version, read from the packages.
 *
 * <p>jdeb and the rpm plugin each take their version from their own configuration, and a plugin may
 * take it from a property that means something else - a test of the pom would not see that, a look
 * into the built package does. The version expected is the project's, mapped as the packaging
 * promises: {@code 0.4.1-SNAPSHOT} built as run 7 is {@code 0.4.1~snapshot.7}, so a snapshot sorts
 * below its release and each run above the last; a release is its own version. The rpm's release is
 * always {@code 1}, so the rpm and the deb say the same version.
 *
 * <p>Not a unit test: the packages exist only after jdeb and the rpm plugin, which run at {@code
 * verify}. The {@code dist} profile runs this class by name after both, so neither the unit tests
 * nor the acceptance suite pick it up.
 */
class PackageVersionCheck {

    private static final String NAME = "porelo-agent-pi";

    private static final Path TARGET = Path.of(System.getProperty("porelo.target", "target"));

    @Test
    void theDebCarriesTheProjectsVersion() throws IOException, InterruptedException {

        final Path deb = newest(NAME + "_", ".deb");

        assertThat(run("dpkg-deb", "-f", deb.toString(), "Package").strip()).isEqualTo(NAME);
        assertThat(run("dpkg-deb", "-f", deb.toString(), "Version").strip())
                .as("the Version of %s", deb)
                .isEqualTo(expected());
    }

    @Test
    void theRpmCarriesTheProjectsVersion() throws IOException, InterruptedException {

        final Path rpm = newest(NAME + "-", ".rpm");

        assertThat(
                        run(
                                "rpm",
                                "-qp",
                                "--queryformat",
                                "%{NAME} %{VERSION} %{RELEASE}",
                                rpm.toString()))
                .as("the name, version and release of %s", rpm)
                .isEqualTo(NAME + " " + expected() + " 1");
    }

    @Test
    void aSnapshotSortsBelowItsReleaseAndAboveTheRunBeforeForApt() throws Exception {

        // A snapshot sorting above its release would keep every installation on the snapshot.
        assumeTrue(onPath("dpkg"), "dpkg is not installed here");
        assertSortsInOrder(
                (a, b) -> {
                    final Processes.Result r =
                            Processes.run(
                                    new ProcessBuilder("dpkg", "--compare-versions", a, "lt", b),
                                    Duration.ofSeconds(30));
                    assertThat(r.exit()).as("dpkg's answer: %s", r.output()).isBetween(0, 1);
                    return r.exit() == 0;
                });
    }

    @Test
    void aSnapshotSortsBelowItsReleaseAndAboveTheRunBeforeForDnf() throws Exception {

        assumeTrue(onPath("rpm"), "rpm is not installed here");
        assertSortsInOrder(
                (a, b) -> {
                    final Processes.Result r =
                            Processes.run(
                                    new ProcessBuilder(
                                            "rpm",
                                            "--eval",
                                            "%{lua: print(rpm.vercmp('" + a + "', '" + b + "'))}"),
                                    Duration.ofSeconds(30));
                    assertThat(r.exit()).as("rpm's answer: %s", r.output()).isZero();
                    return r.output().strip().equals("-1");
                });
    }

    /** Asks whether one package version sorts below another. */
    @FunctionalInterface
    private interface Below {
        boolean test(String a, String b) throws IOException, InterruptedException;
    }

    /**
     * Asserts that the release before, two snapshot runs and their release sort in that order, the
     * runs 9 and 10 so that a comparison of text instead of numbers shows.
     */
    private static void assertSortsInOrder(final Below below)
            throws IOException, InterruptedException {
        final List<String> order =
                List.of(
                        packageVersion("0.4.0", "8"),
                        packageVersion("0.4.1-SNAPSHOT", "9"),
                        packageVersion("0.4.1-SNAPSHOT", "10"),
                        packageVersion("0.4.1", "11"));
        for (int i = 1; i < order.size(); i++) {
            assertThat(below.test(order.get(i - 1), order.get(i)))
                    .as("%s sorts below %s", order.get(i - 1), order.get(i))
                    .isTrue();
        }
    }

    private static boolean onPath(final String command) {
        return Stream.of(System.getenv("PATH").split(":"))
                .anyMatch(dir -> Files.isExecutable(Path.of(dir, command)));
    }

    /** The version the packages must carry, from what Maven built and the run it was given. */
    private static String expected() {
        return packageVersion(
                System.getProperty("project.version", ""),
                System.getProperty("agent.snapshot.run", ""));
    }

    /**
     * Maps a project version to the version its packages carry.
     *
     * @param project The project's version, as Maven has it.
     * @param run The build's run number.
     * @return The package version.
     */
    static String packageVersion(final String project, final String run) {
        assertThat(project).as("project.version, passed by the dist profile").isNotBlank();
        return project.endsWith("-SNAPSHOT")
                ? project.replaceFirst("-SNAPSHOT$", "~snapshot." + run)
                : project;
    }

    /**
     * The newest file in the target directory whose name starts and ends so - a build may leave
     * older ones.
     */
    private static Path newest(final String prefix, final String suffix) throws IOException {
        try (Stream<Path> files = Files.list(TARGET)) {
            return files.filter(
                            f ->
                                    f.getFileName().toString().startsWith(prefix)
                                            && f.getFileName().toString().endsWith(suffix))
                    .max(
                            (a, b) ->
                                    Long.compare(
                                            a.toFile().lastModified(), b.toFile().lastModified()))
                    .orElseThrow(
                            () ->
                                    new AssertionError(
                                            "no "
                                                    + prefix
                                                    + "*"
                                                    + suffix
                                                    + " in "
                                                    + TARGET
                                                    + " - built by -Pnative,dist"));
        }
    }

    /**
     * Runs a command and returns what it printed; what it says on stderr only shows when it fails.
     */
    private static String run(final String... command) throws IOException, InterruptedException {
        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().put("LC_ALL", "C");
        // To files rather than pipes: reading a pipe to its end would wait out a hang, not the
        // timeout.
        // rpm warns on stderr about a database a query of a package file does not need.
        final Path out = Files.createTempFile("package-version", ".out");
        final Path err = Files.createTempFile("package-version", ".err");
        try {
            final Process process =
                    builder.redirectOutput(out.toFile()).redirectError(err.toFile()).start();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new AssertionError(List.of(command) + " did not finish within 30 seconds");
            }
            final String output = Files.readString(out, StandardCharsets.UTF_8);
            assertThat(process.exitValue())
                    .as(
                            "%s: %s%s",
                            List.of(command), output, Files.readString(err, StandardCharsets.UTF_8))
                    .isZero();
            return output;
        } finally {
            Files.deleteIfExists(out);
            Files.deleteIfExists(err);
        }
    }
}
