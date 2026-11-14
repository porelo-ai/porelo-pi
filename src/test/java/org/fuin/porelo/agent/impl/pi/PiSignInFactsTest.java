package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import org.fuin.porelo.agent.api.Finding.Kind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The entries' shapes are the ones {@link PiFindingsTest} holds to Pi's package. */
class PiSignInFactsTest {

    private static final String SECRET = "fixture-secret-value";
    private static final long EXPIRES = 1_800_000_000_000L;
    private static final Instant CHANGED = Instant.parse("2026-11-01T10:00:00Z");
    private static final String API_KEY = "{\"type\":\"api_key\",\"key\":\"" + SECRET + "\"}";

    @TempDir Path home;

    private Path directory;

    @BeforeEach
    void createTheDirectory() throws IOException {
        directory = Files.createDirectory(home.resolve("agent"));
    }

    private List<PiSignInFacts> facts() {
        return PiSignInFacts.of(new PiFindings().find(directory), directory);
    }

    private void auth(final String content) throws IOException {
        Files.setLastModifiedTime(
                Files.writeString(directory.resolve("auth.json"), content), FileTime.from(CHANGED));
    }

    private static String oauth(final String refresh, final long expires) {
        return "{\"type\":\"oauth\",\"access\":\""
                + SECRET
                + "\",\"refresh\":\""
                + refresh
                + "\",\"expires\":"
                + expires
                + "}";
    }

    // Prevents the person choosing a sign-in blind: how fresh it is and how long it holds.
    @Test
    void anAnthropicSignInSaysItsExpiryAndLastChange() throws IOException {
        auth("{\"anthropic\":" + oauth(SECRET, EXPIRES) + "}");

        assertThat(facts())
                .singleElement()
                .satisfies(
                        facts -> {
                            assertThat(facts.finding().provider()).isEqualTo("anthropic");
                            assertThat(facts.finding().kind()).isEqualTo(Kind.SUBSCRIPTION);
                            assertThat(facts.finding().importable()).as("carriable").isTrue();
                            assertThat(facts.account()).as("no entry names one").isNull();
                            assertThat(facts.expires())
                                    .as("milliseconds")
                                    .isEqualTo(Instant.ofEpochMilli(EXPIRES));
                            assertThat(facts.changed()).isEqualTo(CHANGED);
                        });
    }

    // Prevents each provider's entry answering for another's: the facts follow the provider.
    @Test
    void eachProviderHasTheFactsOfItsOwnEntry() throws IOException {
        auth(
                "{\"openrouter\":"
                        + API_KEY
                        + ",\"anthropic\":"
                        + oauth(SECRET, EXPIRES)
                        + ",\"github-copilot\":"
                        + oauth(SECRET, EXPIRES)
                        + "}");

        assertThat(facts())
                .extracting(facts -> facts.finding().provider(), PiSignInFacts::expires)
                .as("provider and expiry, in the findings' order")
                .containsExactly(
                        tuple("anthropic", Instant.ofEpochMilli(EXPIRES)),
                        tuple("github-copilot", null),
                        tuple("openrouter", null));
    }

    // Prevents OpenRouter's key, which never expires, claiming an expiry in the year 287396.
    @Test
    void anOpenRouterSignInHasNoExpiry() throws IOException {
        auth("{\"openrouter\":" + oauth("", 9_007_199_254_740_991L) + "}");

        assertThat(facts())
                .singleElement()
                .satisfies(
                        facts -> {
                            assertThat(facts.finding().kind()).isEqualTo(Kind.API_KEY);
                            assertThat(facts.expires()).isNull();
                            assertThat(facts.changed()).isEqualTo(CHANGED);
                        });
    }

    // Prevents Copilot's session token's hour standing as the life of the GitHub token carried.
    @Test
    void aCopilotSignInHasNoExpiry() throws IOException {
        auth("{\"github-copilot\":" + oauth(SECRET, EXPIRES) + "}");

        assertThat(facts()).singleElement().extracting(PiSignInFacts::expires).isNull();
    }

    // Prevents a zero expiry from reading as one in 1970.
    @Test
    void noExpiryHeldIsNoExpiry() throws IOException {
        auth("{\"anthropic\":" + oauth(SECRET, 0) + "}");

        assertThat(facts()).singleElement().extracting(PiSignInFacts::expires).isNull();
    }

    // Prevents a token's content reaching the person as an account, even one shaped like it.
    @Test
    void aTokenIsNeverReadAsTheAccount() throws IOException {
        auth(
                "{\"anthropic\":{\"type\":\"oauth\",\"access\":\"person@example.org\","
                        + "\"refresh\":\"person@example.org\",\"expires\":1}}");

        assertThat(facts()).singleElement().extracting(PiSignInFacts::account).isNull();
    }

    // Prevents a route an import cannot take - a command, a variable - being offered to carry.
    @Test
    void aKeyGivenAsACommandOrAVariableHasNoFacts() throws IOException {
        auth(
                "{\"anthropic\":{\"type\":\"api_key\",\"key\":\"!print-key\"},"
                        + "\"openrouter\":{\"type\":\"api_key\",\"key\":\"$OPENROUTER_KEY\"}}");

        assertThat(facts()).isEmpty();
    }

    // Prevents a key in the models file, which the import never takes, being offered to carry.
    @Test
    void aKeyInTheModelsFileHasNoFacts() throws IOException {
        Files.writeString(
                directory.resolve("models.json"),
                "{\"providers\":{\"openrouter\":{\"apiKey\":\"" + SECRET + "\"}}}");

        assertThat(facts()).isEmpty();
    }

    // Prevents a sign-in without a provider of Porelo's, or moved to another address, being
    // carried under a name that is not its own.
    @Test
    void aSignInWithoutAProviderHasNoFacts() throws IOException {
        auth("{\"some-gateway\":" + API_KEY + ",\"openrouter\":" + API_KEY + "}");
        Files.writeString(
                directory.resolve("models.json"),
                "{\"providers\":{\"openrouter\":{\"baseUrl\":\"https://gateway.example.org\"}}}");

        assertThat(facts()).isEmpty();
    }

    // Prevents a file Pi refuses being described as holding a sign-in.
    @Test
    void aBrokenFileHasNoFacts() throws IOException {
        auth("{\"anthropic\":");

        assertThat(facts()).isEmpty();
    }

    // Prevents a credential's value leaving the adapter in what is shown to the person.
    @Test
    void noFactCarriesACredentialsValue() throws IOException {
        auth(
                "{\"anthropic\":"
                        + oauth(SECRET, EXPIRES)
                        + ",\"github-copilot\":"
                        + oauth(SECRET, EXPIRES)
                        + ",\"openrouter\":"
                        + API_KEY
                        + "}");

        final List<PiSignInFacts> facts = facts();

        assertThat(facts).as("every route was read").hasSize(3);
        assertThat(facts.toString()).as("no value in any field").doesNotContain(SECRET);
    }
}
