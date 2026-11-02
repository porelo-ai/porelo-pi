package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.fuin.porelo.agent.api.AgentException;
import org.fuin.porelo.agent.api.Credential;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The shapes are the ones {@link PiFindingsTest} holds to Pi 1.4.0. */
class PiCredentialExtractorTest {

    private static final String KEY = "fixture-key-value";
    private static final String ACCESS = "fixture-access-value";
    private static final String REFRESH = "fixture-refresh-value";
    private static final String API_KEY = "{\"type\":\"api_key\",\"key\":\"" + KEY + "\"}";
    private static final String OAUTH =
            "{\"type\":\"oauth\",\"access\":\""
                    + ACCESS
                    + "\",\"refresh\":\""
                    + REFRESH
                    + "\",\"expires\":1234}";

    @TempDir Path directory;

    private final PiCredentialExtractor extractor = new PiCredentialExtractor();

    private void auth(final String content) throws IOException {
        Files.writeString(directory.resolve("auth.json"), content);
    }

    private Credential extract(final String provider) {
        return extractor.extract(directory, provider).orElseThrow();
    }

    @Test
    void theProviderAskedForIsTakenAndNoOther() throws IOException {

        // Prevents a credential landing under the wrong provider's name in the vault.
        auth("{\"anthropic\":" + OAUTH + ",\"openrouter\":" + API_KEY + "}");

        final Credential credential = extract("openrouter");

        assertThat(credential.provider()).as("the provider it names").isEqualTo("openrouter");
        assertThat(credential.type()).as("its type").isEqualTo("api-key");
        assertThat(credential.secret()).as("its value").isEqualTo(KEY);
    }

    @Test
    void aProviderItDoesNotHoldFindsNothing() throws IOException {

        // Prevents another provider's credential answering for the one asked for.
        auth("{\"anthropic\":" + OAUTH + "}");

        assertThat(extractor.extract(directory, "openrouter"))
                .as("asking for a provider Pi is not signed in to")
                .isEmpty();
    }

    @Test
    void withoutAProviderTheFirstTheImportCanTakeAnswers() throws IOException {

        // Prevents a command being resolved, or nothing found, when a usable sign-in is there.
        auth(
                "{\"openrouter\":"
                        + API_KEY
                        + ",\"anthropic\":{\"type\":\"api_key\",\"key\":\"!never-run\"}}");

        final Credential credential = extractor.extract(directory).orElseThrow();

        assertThat(credential.provider()).as("the provider it names").isEqualTo("openrouter");
    }

    @Test
    void anAnthropicSignInKeepsWhatRenewsIt() throws IOException {

        // Prevents a subscription ending with its access token, and being stored as an API key.
        auth("{\"anthropic\":" + OAUTH + "}");

        final Credential credential = extract("anthropic");

        assertThat(credential.type()).as("its type").isEqualTo("oauth");
        assertThat(credential.secret()).as("its value").isEqualTo(ACCESS);
        assertThat(credential.attributes())
                .as("what renews it")
                .containsEntry(Credential.REFRESH_TOKEN, REFRESH)
                .containsEntry(Credential.EXPIRES_AT, "1234")
                .containsEntry(Credential.TOKEN_URL, "https://platform.claude.com/v1/oauth/token")
                .containsEntry(Credential.CLIENT_ID, "9d1c250a-e61b-44d9-88ed-5944d1962f5e");
    }

    @Test
    void anOpenRouterSignInIsTheKeyItEndedIn() throws IOException {

        // Prevents a key that never expires being renewed, or sent as a subscription token.
        auth(
                "{\"openrouter\":{\"type\":\"oauth\",\"access\":\""
                        + ACCESS
                        + "\",\"refresh\":\"\",\"expires\":9007199254740991}}");

        final Credential credential = extract("openrouter");

        assertThat(credential.type()).as("its type").isEqualTo("api-key");
        assertThat(credential.secret()).as("its value").isEqualTo(ACCESS);
        assertThat(credential.attributes()).as("its attributes").isEmpty();
    }

    @Test
    void aCopilotSignInIsItsGitHubToken() throws IOException {

        // Prevents the session token, which ends within the hour, standing where Porelo keeps the
        // GitHub token that never does.
        auth("{\"github-copilot\":" + OAUTH + "}");

        final Credential credential = extract("github-copilot");

        assertThat(credential.type()).as("its type").isEqualTo("access-token");
        assertThat(credential.secret()).as("its value").isEqualTo(REFRESH);
        assertThat(credential.attributes()).as("its attributes").isEmpty();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"type\":\"api_key\",\"key\":\"!touch marker\"}",
                "{\"type\":\"api_key\",\"key\":\"$OPENROUTER_KEY\"}",
                "{\"type\":\"api_key\"}"
            })
    void whatFindsSaysCannotBeImportedIsNotImported(final String entry) throws IOException {

        // Prevents a command's text or a variable's name being stored as if it were the key.
        auth("{\"openrouter\":" + entry + "}");

        assertThat(extractor.extract(directory, "openrouter")).as("the entry %s", entry).isEmpty();
    }

    @Test
    void aProviderMovedToAnotherAddressIsNotImported() throws IOException {

        // Prevents a gateway's key being stored as the provider's and sent to the provider.
        auth("{\"openrouter\":" + API_KEY + "}");
        Files.writeString(
                directory.resolve("models.json"),
                "{\"providers\":{\"openrouter\":{\"baseUrl\":\"http://localhost\"}}}");

        assertThat(extractor.extract(directory, "openrouter")).as("a moved provider").isEmpty();
    }

    @Test
    void noFileFindsNothing() {

        // Prevents an account where Pi never signed in failing the import.
        assertThat(extractor.extract(directory, "openrouter")).as("no auth.json").isEmpty();
    }

    @Test
    void aFileThatCannotBeUnderstoodSaysSoAndNotItsContent() throws IOException {

        // Prevents "nothing to import" sending a signed-in person to sign in again, and a
        // parser's message carrying a piece of the file into a log.
        auth("{\"openrouter\":\"" + KEY);

        assertThatThrownBy(() -> extractor.extract(directory, "openrouter"))
                .as("a broken auth.json")
                .isInstanceOf(AgentException.class)
                .hasMessageContaining("auth.json")
                .hasMessageNotContaining(KEY);
    }

    @Test
    void anEntryPiRefusesSaysSo() throws IOException {

        // Prevents one provider being imported from a file Pi itself refuses as a whole.
        auth("{\"openrouter\":" + API_KEY + ",\"groq\":{\"type\":\"made-up\"}}");

        assertThatThrownBy(() -> extractor.extract(directory, "openrouter"))
                .as("an auth.json with an entry Pi refuses")
                .isInstanceOf(AgentException.class)
                .hasMessageNotContaining(KEY);
    }
}
