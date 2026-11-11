package org.fuin.porelo.agent.impl.pi;

import java.util.List;
import org.fuin.porelo.agent.api.AgentEnd;
import org.fuin.porelo.agent.api.ContainerSetup;
import org.fuin.porelo.agent.api.CredentialExtractor;
import org.fuin.porelo.agent.api.Finder;
import org.fuin.porelo.agent.api.LogFormatter;
import org.fuin.porelo.agent.api.ToolEvent;
import org.fuin.porelo.agent.api.YamlAgent;
import org.jspecify.annotations.Nullable;

/**
 * Pi.
 *
 * <p>Four overrides: the endpoint and the token go into an extension Pi discovers, because it has
 * no variable for either; its JSON events are rendered for the log, because a raw one can hold a
 * whole file; and what it finds of itself on the host, and what an import takes, depend on what
 * Pi's files hold. Everything else is {@code pi.yaml}.
 */
public final class PiAgent extends YamlAgent {

    /** Constructor. */
    public PiAgent() {
        super("pi");
    }

    /**
     * Says what Pi finds of itself on the host; overrides the agent API's default once Porelo
     * publishes it.
     *
     * @return Finder reading Pi's own files.
     */
    @Override
    public Finder finder() {
        return new PiFindings();
    }

    @Override
    public CredentialExtractor credentialExtractor() {
        return new PiCredentialExtractor();
    }

    @Override
    public ContainerSetup containerSetup() {
        return new PiContainerSetup();
    }

    @Override
    public LogFormatter logFormatter() {
        return new PiJsonFormatter();
    }

    @Override
    public @Nullable AgentEnd ended(String line) {
        return PiJsonFormatter.ended(line);
    }

    /**
     * Reads the tool calls Pi starts or ends in one output line.
     *
     * @param line One raw line of output.
     * @return The line's tool events.
     */
    @Override
    public List<ToolEvent> toolEvents(String line) {
        return PiToolEvents.read(line);
    }

    /**
     * Entry point of the {@code porelo-agent-pi} binary.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        org.fuin.porelo.agent.api.AgentMain.run(new PiAgent(), args);
    }
}
