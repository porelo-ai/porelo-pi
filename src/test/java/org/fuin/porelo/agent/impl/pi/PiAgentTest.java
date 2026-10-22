package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.fuin.porelo.agent.api.Agent;
import org.fuin.porelo.agent.api.AgentGrant;
import org.fuin.porelo.agent.api.AgentRegistry;
import org.fuin.porelo.agent.api.PackagedTree;
import org.fuin.porelo.agent.api.RunRequest;
import org.junit.jupiter.api.Test;

/** Tests for the Pi agent module. */
class PiAgentTest {

    private final Agent agent = new PiAgent();

    @Test
    void declaresWhatItShowsOnceAtWork() {

        // The kit's check waits for this text and types nothing; a dialog before the prompt hides
        // it.
        assertThat(agent.definition().ready()).isNotNull();
        assertThat(agent.definition().ready().text()).isEqualTo("Pi can explain its own features");
    }

    @Test
    void grantsGitHubCopilotThroughTheAppPiSignsInWith() {

        // Without it 'porelo vault authorize github-copilot' refuses Pi as an agent that signs in
        // nowhere, so a Copilot task cannot be given its credential at all.
        final AgentGrant grant = agent.definition().grantFor("github-copilot");
        assertThat(grant).as("the GitHub Copilot grant pi.yaml declares").isNotNull();
        assertThat(Objects.requireNonNull(grant).clientIdFor("github.com"))
                .as("the client id Pi itself signs in with, on github.com")
                .isEqualTo("Iv1.b507a08c87ecfe98");
    }

    @Test
    void isDiscoveredThroughTheServiceLoader() {

        // Found through META-INF/services, like claude and omp; the directory was here and empty.
        final AgentRegistry registry = AgentRegistry.discover();

        assertThat(registry.names()).contains("pi");
        assertThat(registry.require("pi").definition().binary()).isEqualTo("pi");
    }

    @Test
    void shipsItsTreeBesideItsBinary() throws IOException {

        // Relative to the binary, the tree follows the agent into an account's own directory; an
        // absolute path would copy the machine's tree whichever copy of the agent ran.
        assertThat(agent.definition().packaged())
                .extracting(PackagedTree::source)
                .containsExactly("pi/pi-tree.tar.gz");
        // And both packages put it there, beside the binary they install.
        final String pom = Files.readString(Path.of("pom.xml"));
        assertThat(pom)
                .contains("<prefix>${agent.install.dir}/pi</prefix>")
                .contains("<name>${agent.install.dir}/pi/pi-tree.tar.gz</name>")
                .doesNotContain("/usr/share/porelo/agents/pi");
    }

    @Test
    void ignoresWhatTheRepositoryWouldConfigure() {

        // The workspace is untrusted input; its .pi extensions would load after porelo-route.ts.
        assertThat(agent.definition().sandboxedArguments()).containsExactly("--no-approve");
    }

    @Test
    void buildsAHeadlessCommandWithThePromptLast() {

        // The prompt is positional, so it has to come after the flags rather than behind one.
        assertThat(
                        agent.headlessCommand(
                                new RunRequest(
                                        "fix the bug", "z-ai/glm-4.6", null, null, false, true)))
                .containsExactly(
                        "pi",
                        "--no-approve",
                        "--model",
                        "z-ai/glm-4.6",
                        "--print",
                        "--mode",
                        "json",
                        "fix the bug");
    }

    @Test
    void resumesWithTheFlagPiItselfUses() {

        assertThat(
                        agent.headlessCommand(
                                new RunRequest("carry on", null, null, "01a07a41", false, false)))
                .containsExactly("pi", "--no-approve", "--session", "01a07a41", "carry on");
    }

    @Test
    void refusesPiDev() {

        // Where the install ping and the update check go; refused, so the policy is stated, not
        // implied.
        assertThat(agent.definition().refusedDomains()).contains("pi.dev");
    }

    @Test
    void rendersItsLogRatherThanShowingRawEvents() {

        // Without the override the default formatter passes every event through as raw JSON.
        assertThat(
                        new PiAgent()
                                .logFormatter()
                                .format(
                                        "{\"type\":\"agent_end\",\"messages\":["
                                                + "{\"role\":\"assistant\",\"content\":[],\"stopReason\":\"stop\"}],\"willRetry\":false}"))
                .isEqualTo("-- done, 1 turn");
    }

    @Test
    void isToldWhatPoreloPutsInItsTaskThroughItsSystemPrompt() {

        // Porelo puts these behind the sandboxed arguments in every task Porelo makes, {file}
        // standing for the
        // path of its guide to the task; the CLI reads the file, so the text never rides on the
        // command line.
        assertThat(agent.definition().instructionArguments())
                .containsExactly("--append-system-prompt", "{file}");
    }

    @Test
    void saysWhenItIsAtRestAsMeasuredAtATerminal() {

        // Porelo types its wake line only where this matches the last lines of the screen. The
        // screens are
        // cut from ones measured on the VM: after an answer, and during a 'sleep 90' tool call.
        final var atRest = agent.definition().atRest();
        assertThat(atRest).isNotNull();
        assertThat(atRest.shows()).containsExactly("(auto)");
        assertThat(atRest.lacks()).containsExactly("Working ─");
        assertThat(
                        atRest.matches(
                                "PONG\n────\n↑1.9k ↓19 $0.000 0.2%/1.0M (auto)      z-ai/glm-5.3-flash • high"))
                .isTrue();
        assertThat(
                        atRest.matches(
                                "── ⠹ Working ────────────\n0.0%/1.0M (auto)      z-ai/glm-5.3-flash • high"))
                .isFalse();
    }
}
