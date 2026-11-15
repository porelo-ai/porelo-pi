package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * How the agent takes a reasoning effort, as its definition declares it for Porelo's Start.
 *
 * <p>Prevents a definition that drifts from what was read off the CLI: Porelo offers exactly the
 * declared values and puts the picked one where {@code {effort}} stands, so a renamed flag or a
 * lost level would reach every start. Read from the definition itself, since an older Porelo's
 * reader skips the section.
 */
class EffortDeclarationTest {

    private static Map<?, ?> effort() throws IOException {
        try (InputStream in =
                EffortDeclarationTest.class.getClassLoader().getResourceAsStream("agent/pi.yaml")) {
            assertThat(in).as("the definition on the classpath").isNotNull();
            final Map<?, ?> definition = new Yaml().load(in);
            assertThat(definition.get("effort"))
                    .as("pi.yaml declares an effort section")
                    .isInstanceOf(Map.class);
            return (Map<?, ?>) definition.get("effort");
        }
    }

    @Test
    void passesThePickedEffortWhereTheCliReadsIt() throws IOException {

        assertThat(effort().get("arguments"))
                .as("'pi --help' names --thinking <level>")
                .isEqualTo(List.of("--thinking", "{effort}"));
    }

    @Test
    void offersTheLevelsTheCliTakes() throws IOException {

        assertThat(effort().get("values"))
                .as("the levels the CLI names, in its words and order")
                .isEqualTo(List.of("off", "minimal", "low", "medium", "high", "xhigh", "max"));
    }
}
