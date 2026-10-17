package org.fuin.porelo.agent.impl.pi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** Runs a process for a test, within a limit. */
final class Processes {

    private Processes() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * What a process printed, standard error included, and how it exited.
     *
     * @param exit Its exit code.
     * @param output Everything it printed.
     */
    record Result(int exit, String output) {}

    /**
     * Runs a process to its end, standard error merged into its output.
     *
     * @param builder The process.
     * @param limit How long it may take.
     * @return What it printed and how it exited.
     * @throws IOException It could not start.
     * @throws InterruptedException The wait was interrupted.
     */
    static Result run(final ProcessBuilder builder, final Duration limit)
            throws IOException, InterruptedException {
        // To a file rather than a pipe: reading a pipe to its end waits out a hang, not the limit.
        final Path out = Files.createTempFile("process", ".out");
        try {
            final Process process =
                    builder.redirectErrorStream(true).redirectOutput(out.toFile()).start();
            if (!process.waitFor(limit.toMillis(), TimeUnit.MILLISECONDS)) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
                throw new AssertionError(
                        builder.command()
                                + " did not end within "
                                + limit
                                + ", and printed: "
                                + Files.readString(out, StandardCharsets.UTF_8));
            }
            return new Result(process.exitValue(), Files.readString(out, StandardCharsets.UTF_8));
        } finally {
            Files.deleteIfExists(out);
        }
    }
}
