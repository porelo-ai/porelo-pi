package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class WorkflowTimeLimitTest {

    /** A job's id, as YAML allows it: any key one level under {@code jobs:}. */
    private static final Pattern JOB = Pattern.compile("^    ([A-Za-z0-9_][A-Za-z0-9_-]*):\\s*$");

    /** The {@code jobs:} key, with a comment or trailing blanks after it. */
    private static final Pattern JOBS = Pattern.compile("^jobs:\\s*(#.*)?$");

    private static final Pattern LIMIT =
            Pattern.compile("^        timeout-minutes: [1-9][0-9]*\\s*$");

    @Test
    void everyJobStopsAfterALimit() throws IOException {

        // GitHub's default is six hours, and a leg on a rented machine pays for every minute of a
        // hang.
        final Map<String, Boolean> jobs = new LinkedHashMap<>();
        try (Stream<Path> files =
                Files.list(RepositoryDocuments.root().resolve(".github/workflows"))) {
            for (final Path file :
                    files.filter(f -> f.toString().endsWith(".yml")).sorted().toList()) {
                jobs(Files.readAllLines(file))
                        .forEach(
                                (job, limited) ->
                                        jobs.put(file.getFileName() + " " + job, limited));
            }
        }

        // A guard that reads no job passes forever: the jobs it read, then what it checks.
        assertThat(jobs.keySet())
                .as("the jobs read")
                .containsExactly(
                        "artifactory-smoke.yml smoke",
                        "build.yml build",
                        "build.yml proof",
                        "build.yml acceptance",
                        "build.yml release",
                        "delete-runs.yml delete",
                        "shared-rules.yml check-shared",
                        "update.yml update");
        assertThat(jobs.entrySet().stream().filter(job -> !job.getValue()).map(Map.Entry::getKey))
                .as("jobs without timeout-minutes")
                .isEmpty();
    }

    @Test
    void readsEveryJobIdYamlAllows() {

        final Map<String, Boolean> jobs =
                jobs(
                        List.of(
                                "on: push",
                                "jobs:  # the work",
                                "    Smoke:",
                                "        runs-on: ubuntu-24.04",
                                "    build_2:",
                                "        timeout-minutes: 5",
                                "        steps:",
                                "            - run: true",
                                "              timeout-minutes: 3",
                                "    3rd: ",
                                "        runs-on: ubuntu-24.04",
                                "            timeout-minutes: 9"));

        assertThat(jobs)
                .as("each job and whether it has a limit of its own")
                .containsExactly(
                        Map.entry("Smoke", false),
                        Map.entry("build_2", true),
                        Map.entry("3rd", false));
    }

    /**
     * Returns each job of a workflow and whether it sets a job-level {@code timeout-minutes}.
     *
     * @param lines The workflow's lines.
     * @return Job id to whether it is limited, in the order of the file.
     */
    private static Map<String, Boolean> jobs(final List<String> lines) {
        final Map<String, Boolean> jobs = new LinkedHashMap<>();
        boolean inJobs = false;
        String job = null;
        for (final String line : lines) {
            if (JOBS.matcher(line).matches()) {
                inJobs = true;
                continue;
            }
            if (!line.isBlank() && !line.startsWith(" ") && !line.startsWith("#")) {
                inJobs = false;
                job = null;
                continue;
            }
            final Matcher m = JOB.matcher(line);
            if (inJobs && m.matches()) {
                job = m.group(1);
                jobs.put(job, false);
            } else if (job != null && LIMIT.matcher(line).matches()) {
                jobs.put(job, true);
            }
        }
        return jobs;
    }
}
