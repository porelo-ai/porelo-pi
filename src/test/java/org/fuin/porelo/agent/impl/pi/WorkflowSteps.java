package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Finds a workflow step's script and runs it as the runner does, so a test proves what the step
 * does rather than that its text is there.
 */
final class WorkflowSteps {

    private static final Path WORKFLOWS = RepositoryDocuments.root().resolve(".github/workflows");

    private WorkflowSteps() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Returns the run block of each step of that name, its indentation removed.
     *
     * @param workflow The workflow's file name, such as {@code build.yml}.
     * @param name The step's name.
     * @return One script per step, in the order of the file.
     * @throws IOException The workflow cannot be read.
     */
    static List<String> scripts(final String workflow, final String name) throws IOException {
        final List<String> lines = Files.readAllLines(WORKFLOWS.resolve(workflow));
        final List<String> result = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).strip().equals("- name: " + name)) {
                continue;
            }
            final int step = indentOf(lines.get(i));
            int j = i + 1;
            while (j < lines.size()
                    && !lines.get(j).strip().startsWith("run:")
                    && (lines.get(j).isBlank() || indentOf(lines.get(j)) > step)) {
                j++;
            }
            assertThat(j < lines.size() && lines.get(j).strip().startsWith("run:"))
                    .as("the step \"%s\" in %s runs a script", name, workflow)
                    .isTrue();
            final String inline = lines.get(j).strip().substring("run:".length()).strip();
            if (!inline.equals("|")) {
                result.add(inline + "\n");
                continue;
            }
            final int key = indentOf(lines.get(j));
            final StringBuilder script = new StringBuilder();
            int body = -1;
            for (int k = j + 1;
                    k < lines.size() && (lines.get(k).isBlank() || indentOf(lines.get(k)) > key);
                    k++) {
                if (lines.get(k).isBlank()) {
                    script.append('\n');
                    continue;
                }
                if (body < 0) {
                    body = indentOf(lines.get(k));
                }
                script.append(lines.get(k).substring(body)).append('\n');
            }
            result.add(script.toString());
        }
        return result;
    }

    /**
     * Runs a step's script in a directory with the shell GitHub gives a step without {@code
     * shell:}, {@code bash -e}, the stubs' directory first on the {@code PATH}.
     *
     * @param script The step's script, holding no {@code ${{ }}} expression.
     * @param dir Where it runs.
     * @param stubs Directory of stand-ins for the commands it must not really run.
     * @param env What the step's environment adds.
     * @return What it printed and how it exited.
     * @throws IOException The script cannot be written or started.
     * @throws InterruptedException The wait was interrupted.
     */
    static Processes.Result run(
            final String script, final Path dir, final Path stubs, final Map<String, String> env)
            throws IOException, InterruptedException {
        assertThat(script)
                .as("a step's script that the runner, not bash, would expand")
                .doesNotContain("${{");
        final Path file = dir.resolve("step.sh");
        Files.writeString(file, script);
        final ProcessBuilder builder =
                new ProcessBuilder("bash", "--noprofile", "--norc", "-e", file.toString())
                        .directory(dir.toFile());
        builder.environment().putAll(env);
        builder.environment().put("PATH", stubs + ":" + System.getenv("PATH"));
        return Processes.run(builder, Duration.ofSeconds(60));
    }

    /**
     * Writes a stub command that records each call's arguments, one line per call, to {@code
     * <name>.calls} beside it, then runs the given body.
     *
     * @param stubs The stubs' directory.
     * @param name The command.
     * @param body What it does after recording, as bash.
     * @throws IOException It cannot be written.
     */
    static void stub(final Path stubs, final String name, final String body) throws IOException {
        Files.createDirectories(stubs);
        final Path file = stubs.resolve(name);
        Files.writeString(
                file,
                "#!/usr/bin/env bash\n"
                        + "printf '%s\\n' \"$*\" >> \""
                        + stubs.resolve(name + ".calls")
                        + "\"\n"
                        + body
                        + "\n");
        assertThat(file.toFile().setExecutable(true)).as("%s made executable", file).isTrue();
    }

    /**
     * Returns the calls a stub recorded.
     *
     * @param stubs The stubs' directory.
     * @param name The command.
     * @return One entry per call, its arguments joined by spaces; empty when it was never called.
     * @throws IOException The record cannot be read.
     */
    static List<String> calls(final Path stubs, final String name) throws IOException {
        final Path file = stubs.resolve(name + ".calls");
        return Files.exists(file) ? Files.readAllLines(file) : List.of();
    }

    private static int indentOf(final String line) {
        return line.length() - line.stripLeading().length();
    }
}
