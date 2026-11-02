package org.fuin.porelo.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;
import org.fuin.porelo.agent.api.Finding;
import org.fuin.porelo.agent.api.Finding.Kind;
import org.fuin.porelo.agent.api.Finding.SignedIn;
import org.fuin.porelo.agent.api.Finding.What;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The shapes are what Pi 1.4.0 and its pi-ai 1.4.0 read and write, read in the pinned package. */
class PiFindingsTest {

    private static final String SECRET = "fixture-secret-value";
    private static final String API_KEY = "{\"type\":\"api_key\",\"key\":\"" + SECRET + "\"}";
    private static final String OAUTH =
            "{\"type\":\"oauth\",\"access\":\""
                    + SECRET
                    + "\",\"refresh\":\""
                    + SECRET
                    + "\",\"expires\":1}";

    @TempDir Path home;

    private Path directory;

    @BeforeEach
    void createTheDirectory() throws IOException {
        directory = Files.createDirectory(home.resolve("agent"));
    }

    private List<Finding> find() throws IOException {
        return new PiFindings().find(directory);
    }

    private List<Finding> signIns() throws IOException {
        return find().stream().filter(finding -> finding.what() == What.SIGN_IN).toList();
    }

    private void auth(final String content) throws IOException {
        Files.writeString(directory.resolve("auth.json"), content);
    }

    private void models(final String content) throws IOException {
        Files.writeString(directory.resolve("models.json"), content);
    }

    private void settings(final String content) throws IOException {
        Files.writeString(directory.resolve("settings.json"), content);
    }

    private String path(final String file) {
        return directory.resolve(file).toString();
    }

    private Finding signedIn(final boolean importable, final String provider, final Kind kind) {
        return new Finding(
                What.SIGN_IN, path("auth.json"), SignedIn.SIGNED_IN, importable, provider, kind);
    }

    private Finding cannotSay(final String file) {
        return new Finding(What.SIGN_IN, path(file), SignedIn.CANNOT_SAY, null, null, null);
    }

    @Test
    void aDirectoryWithoutSignInsIsAConfigurationAndNothingElse() throws IOException {

        // Prevents the empty auth.json Pi writes on its first run reading as a sign-in.
        auth("{}");

        assertThat(find())
                .as("findings of a Pi that signed in to nothing")
                .containsExactly(
                        new Finding(
                                What.CONFIGURATION, directory.toString(), null, null, null, null));
    }

    @Test
    void anAbsentDirectoryFindsNothing() throws IOException {

        // Prevents an account where Pi never ran failing the question.
        assertThat(new PiFindings().find(home.resolve("never-ran")))
                .as("findings of no directory")
                .isEmpty();
    }

    @Test
    void aLiteralKeyIsAnApiKeyTheImportCanTake() throws IOException {

        // Prevents a usable key being reported as a route Porelo cannot take.
        auth("{\"openrouter\":" + API_KEY + "}");

        assertThat(signIns())
                .as("an API key for a provider Porelo declares")
                .containsExactly(signedIn(true, "openrouter", Kind.API_KEY));
    }

    @Test
    void anAnthropicSignInIsASubscriptionTheImportCanTake() throws IOException {

        // Prevents a subscription being sent as an API key, which fails like a wrong key.
        auth("{\"anthropic\":" + OAUTH + "}");

        assertThat(signIns())
                .as("Pi's sign-in with Anthropic")
                .containsExactly(signedIn(true, "anthropic", Kind.SUBSCRIPTION));
    }

    @Test
    void anOpenRouterSignInHoldsAnApiKey() throws IOException {

        // Prevents an OpenRouter key being taken for a subscription: its OAuth flow ends in a key.
        auth(
                "{\"openrouter\":{\"type\":\"oauth\",\"access\":\""
                        + SECRET
                        + "\",\"refresh\":\"\",\"expires\":1}}");

        assertThat(signIns())
                .as("Pi's sign-in with OpenRouter")
                .containsExactly(signedIn(true, "openrouter", Kind.API_KEY));
    }

    @Test
    void aCopilotSignInOnGitHubIsASubscriptionTheImportCanTake() throws IOException {

        // Prevents a person with Copilot on github.com being told to sign in again.
        auth("{\"github-copilot\":" + OAUTH + "}");

        assertThat(signIns())
                .as("Pi's sign-in with GitHub Copilot on github.com")
                .containsExactly(signedIn(true, "github-copilot", Kind.SUBSCRIPTION));
    }

    @Test
    void aCopilotSignInOnAnEnterpriseHostIsNoProviderOfPorelo() throws IOException {

        // Prevents a token of another host being imported as github.com's and sent to it.
        auth(
                "{\"github-copilot\":{\"type\":\"oauth\",\"access\":\""
                        + SECRET
                        + "\",\"refresh\":\""
                        + SECRET
                        + "\",\"expires\":1,\"enterpriseUrl\":\"company.ghe.com\"}}");

        assertThat(signIns())
                .as("Pi's sign-in with GitHub Copilot on an Enterprise host")
                .containsExactly(signedIn(false, null, Kind.SUBSCRIPTION));
    }

    @Test
    void aCopilotSignInWithoutItsGitHubTokenCannotBeImported() throws IOException {

        // Prevents an import of the short-lived session token, when the GitHub token is the one
        // Porelo holds for Copilot.
        auth(
                "{\"github-copilot\":{\"type\":\"oauth\",\"access\":\""
                        + SECRET
                        + "\",\"refresh\":\"\",\"expires\":1}}");

        assertThat(signIns())
                .as("Pi's sign-in with GitHub Copilot without a refresh token")
                .containsExactly(signedIn(false, "github-copilot", Kind.SUBSCRIPTION));
    }

    @Test
    void aKeyGivenAsACommandIsReportedAndNeverRun() throws IOException {

        // Prevents the adapter running what a file says, and a command's sign-in being imported.
        final Path marker = home.resolve("marker");
        auth("{\"openrouter\":{\"type\":\"api_key\",\"key\":\"!touch " + marker + "\"}}");

        assertThat(signIns())
                .as("a key given as a command")
                .containsExactly(signedIn(false, "openrouter", Kind.API_KEY));
        assertThat(marker).as("the marker the command would write").doesNotExist();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"type\":\"api_key\",\"key\":\"$OPENROUTER_KEY\"}",
                "{\"type\":\"api_key\",\"key\":\"${OPENROUTER_KEY}\"}",
                "{\"type\":\"api_key\",\"key\":\"prefix$OPENROUTER_KEY\"}",
                "{\"type\":\"api_key\"}",
                "{\"type\":\"api_key\",\"key\":\"\"}",
                "{\"type\":\"api_key\",\"key\":1}",
                "{\"type\":\"oauth\",\"refresh\":\"x\"}",
                "{\"type\":\"made-up\",\"key\":\"x\"}",
                "\"just-a-string\""
            })
    void anEntryThatDependsOnPisEnvironmentOrIsNotUnderstoodCannotSay(final String entry)
            throws IOException {

        // Prevents a reference being reported as a sign-in when its variable may be unset, a key
        // being looked up in the adapter's own environment, or a new shape reading as signed in.
        auth("{\"openrouter\":" + entry + "}");

        assertThat(signIns())
                .as("the finding of %s", entry)
                .containsExactly(cannotSay("auth.json"));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"type\":\"made-up\",\"key\":\"x\"}",
                "\"just-a-string\"",
                "{\"type\":\"oauth\",\"access\":\"x\"}",
                "{\"type\":\"oauth\",\"access\":\"x\",\"refresh\":\"y\",\"expires\":\"z\"}",
                "{\"type\":\"api_key\",\"key\":1}",
                "{\"type\":\"api_key\",\"key\":\"x\",\"env\":{\"A\":1}}"
            })
    void oneEntryPiRefusesLeavesTheWholeFileOpen(final String entry) throws IOException {

        // Prevents a provider reading as signed in beside such an entry: measured on Pi 1.4.0, one
        // invalid entry makes it refuse the whole file, for every provider.
        auth("{\"openrouter\":" + API_KEY + ",\"groq\":" + entry + "}");

        assertThat(signIns())
                .as("a good entry beside %s", entry)
                .containsExactly(cannotSay("auth.json"));
    }

    @Test
    void aProviderPoreloDoesNotDeclareIsSignedInWithoutAProvider() throws IOException {

        // Prevents a name in a person's file reaching Porelo, and being offered for import.
        auth("{\"made-up-provider\":" + API_KEY + "}");

        assertThat(signIns())
                .as("an API key for a provider Porelo does not declare")
                .containsExactly(signedIn(false, null, Kind.API_KEY));
    }

    @Test
    void aSignInOfAProviderPoreloDoesNotDeclareSaysNoKind() throws IOException {

        // Prevents a guess: what a foreign OAuth entry is, a subscription or a key, is not known.
        auth("{\"made-up-provider\":" + OAUTH + "}");

        assertThat(signIns())
                .as("a sign-in with a provider Porelo does not declare")
                .containsExactly(signedIn(false, null, null));
    }

    @Test
    void aBrokenFileCannotSay() throws IOException {

        // Prevents a file Pi itself fails on reading as signed out, or failing the question.
        auth("{nope");

        assertThat(signIns()).as("a broken auth.json").containsExactly(cannotSay("auth.json"));
    }

    @Test
    void aFileThatIsNoObjectCannotSay() throws IOException {

        // Prevents a list reading as nothing there, when Pi refuses it.
        auth("[]");

        assertThat(signIns()).as("an auth.json of a list").containsExactly(cannotSay("auth.json"));
    }

    @Test
    void aFileThatCannotBeReadCannotSay() throws IOException {

        // Prevents a sign-in that may be there being passed over.
        auth("{\"openrouter\":" + API_KEY + "}");
        final Path file = directory.resolve("auth.json");
        Files.setPosixFilePermissions(file, Set.of());
        try {
            assertThat(signIns())
                    .as("an auth.json that cannot be read")
                    .containsExactly(cannotSay("auth.json"));
        } finally {
            Files.setPosixFilePermissions(
                    file, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        }
    }

    @Test
    void aByteOrderMarkDoesNotHideASignIn() throws IOException {

        // Prevents a file Pi reads, since it strips the mark, reading here as broken.
        auth("﻿{\"openrouter\":" + API_KEY + "}");

        assertThat(signIns())
                .as("an auth.json with a byte order mark")
                .containsExactly(signedIn(true, "openrouter", Kind.API_KEY));
    }

    @Test
    void theProvidersComeInPisOrderForThoseItPrefers() throws IOException {

        // Prevents Porelo naming, from the first finding, a provider Pi would not start with.
        auth(
                "{\"made-up-provider\":"
                        + API_KEY
                        + ",\"openrouter\":"
                        + API_KEY
                        + ",\"github-copilot\":"
                        + OAUTH
                        + ",\"google\":"
                        + API_KEY
                        + ",\"openai\":"
                        + API_KEY
                        + ",\"anthropic\":"
                        + API_KEY
                        + "}");

        assertThat(signIns())
                .as("providers in Pi's order, one Porelo cannot use last")
                .extracting(Finding::provider)
                .containsExactly(
                        "anthropic", "openai", "google", "github-copilot", "openrouter", null);
    }

    @Test
    void theDefaultPiWasToldToUseComesFirst() throws IOException {

        // Prevents Porelo naming another provider than the one the person chose in Pi.
        auth("{\"anthropic\":" + API_KEY + ",\"openrouter\":" + API_KEY + "}");
        settings("{\"defaultProvider\":\"openrouter\",\"defaultModel\":\"some/model\"}");

        assertThat(signIns())
                .as("the default provider, with its model, first")
                .extracting(Finding::provider)
                .containsExactly("openrouter", "anthropic");
    }

    @Test
    void aDefaultPoreloDoesNotDeclareStillComesFirst() throws IOException {

        // Prevents Porelo saying the person's Pi starts with a provider when they chose another.
        auth("{\"anthropic\":" + API_KEY + ",\"made-up-provider\":" + API_KEY + "}");
        settings("{\"defaultProvider\":\"made-up-provider\",\"defaultModel\":\"some-model\"}");

        assertThat(signIns())
                .as("the person's default, though Porelo cannot use it")
                .extracting(Finding::provider)
                .containsExactly(null, "anthropic");
    }

    @Test
    void aDefaultWithoutItsModelIsNoDefault() throws IOException {

        // Prevents an order Pi does not follow: it needs both to take a default.
        auth("{\"anthropic\":" + API_KEY + ",\"openrouter\":" + API_KEY + "}");
        settings("{\"defaultProvider\":\"openrouter\"}");

        assertThat(signIns())
                .as("a default provider alone")
                .extracting(Finding::provider)
                .containsExactly("anthropic", "openrouter");
    }

    @Test
    void settingsThatCannotBeUnderstoodLeaveTheFindings() throws IOException {

        // Prevents a broken settings file hiding a sign-in that is there.
        auth("{\"openrouter\":" + API_KEY + "}");
        settings("{nope");

        assertThat(signIns())
                .as("a sign-in beside broken settings")
                .containsExactly(signedIn(true, "openrouter", Kind.API_KEY));
    }

    @Test
    void aKeyInTheModelsFileIsASignInTheImportCannotTake() throws IOException {

        // Prevents a Pi that works reading as not signed in, its key being in its models file.
        models("{\"providers\":{\"openrouter\":{\"apiKey\":\"" + SECRET + "\"}}}");

        assertThat(signIns())
                .as("a key in models.json")
                .containsExactly(
                        new Finding(
                                What.SIGN_IN,
                                path("models.json"),
                                SignedIn.SIGNED_IN,
                                false,
                                "openrouter",
                                Kind.API_KEY));
    }

    @Test
    void aKeyInTheModelsFileForAnotherAddressIsNoProviderOfPorelo() throws IOException {

        // Prevents a gateway being named as the provider it stands in for.
        models(
                "{\"providers\":{\"openrouter\":{\"baseUrl\":\"http://localhost\",\"apiKey\":\""
                        + SECRET
                        + "\"}}}");

        assertThat(signIns())
                .as("a key in models.json for another address")
                .extracting(Finding::signedIn, Finding::provider)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(SignedIn.SIGNED_IN, null));
    }

    @Test
    void aReferenceInTheModelsFileCannotSay() throws IOException {

        // Prevents a Pi whose variable may be unset reading as signed in.
        models("{\"providers\":{\"openrouter\":{\"apiKey\":\"$OPENROUTER_KEY\"}}}");

        assertThat(signIns())
                .as("a reference in models.json")
                .containsExactly(cannotSay("models.json"));
    }

    @Test
    void aProviderInAuthJsonIsNotReportedAgainFromTheModelsFile() throws IOException {

        // Prevents one provider counting twice: Pi takes the stored credential first.
        auth("{\"openrouter\":" + API_KEY + "}");
        models("{\"providers\":{\"openrouter\":{\"apiKey\":\"" + SECRET + "\"}}}");

        assertThat(signIns())
                .as("a provider in both files")
                .containsExactly(signedIn(true, "openrouter", Kind.API_KEY));
    }

    @Test
    void aStoredKeyForAProviderMovedToAnotherAddressIsNoProviderOfPorelo() throws IOException {

        // Prevents a gateway's key being imported as the provider's and sent to the provider.
        auth("{\"openrouter\":" + API_KEY + "}");
        models("{\"providers\":{\"openrouter\":{\"baseUrl\":\"http://localhost\"}}}");

        assertThat(signIns())
                .as("a key in auth.json for a provider models.json moves")
                .containsExactly(signedIn(false, null, Kind.API_KEY));
    }

    @Test
    void aStoredKeyIsNoProviderOfPoreloWhenTheModelsFileCannotBeRead() throws IOException {

        // Prevents a gateway's key being imported as the provider's when the file that moves it
        // holds a comment: Pi reads one, and the reader here does not.
        auth("{\"openrouter\":" + API_KEY + "}");
        models("// a gateway\n{\"providers\":{\"openrouter\":{\"baseUrl\":\"http://localhost\"}}}");

        assertThat(signIns())
                .as("a key in auth.json beside a models.json that cannot be read")
                .containsExactly(signedIn(false, null, Kind.API_KEY), cannotSay("models.json"));
    }

    @Test
    void keysInBothFilesComeInPisOrder() throws IOException {

        // Prevents the file a key sits in deciding what Porelo names first: Pi does not ask.
        auth("{\"openrouter\":" + API_KEY + "}");
        models(
                "{\"providers\":{\"google\":{\"apiKey\":\""
                        + SECRET
                        + "\"},\"anthropic\":{\"apiKey\":\""
                        + SECRET
                        + "\"}}}");

        assertThat(signIns())
                .as("providers from both files")
                .extracting(Finding::provider)
                .containsExactly("anthropic", "google", "openrouter");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"\"", "1"})
    void aKeyPiRefusesInTheModelsFileLeavesThatFileOpen(final String key) throws IOException {

        // Prevents a provider reading as signed in from a file Pi rejects as a whole.
        models(
                "{\"providers\":{\"openrouter\":{\"apiKey\":\""
                        + SECRET
                        + "\"},\"google\":{\"apiKey\":"
                        + key
                        + "}}}");

        assertThat(signIns())
                .as("a models.json with the key %s", key)
                .containsExactly(cannotSay("models.json"));
    }

    @Test
    void aBrokenModelsFileCannotSay() throws IOException {

        // Prevents a sign-in that may be in that file being passed over.
        models("{nope");

        assertThat(signIns()).as("a broken models.json").containsExactly(cannotSay("models.json"));
    }

    @Test
    void noFindingHoldsAValue() throws IOException {

        // Prevents a credential leaving the host by way of a finding.
        auth(
                "{\"openrouter\":"
                        + API_KEY
                        + ",\"anthropic\":"
                        + OAUTH
                        + ",\""
                        + SECRET
                        + "\":"
                        + API_KEY
                        + ",\"openai\":{\"type\":\"api_key\",\"key\":\"!"
                        + SECRET
                        + "\"}}");
        models("{\"providers\":{\"google\":{\"apiKey\":\"" + SECRET + "\"}}}");

        assertThat(find().toString()).as("every finding, as text").doesNotContain(SECRET);
    }
}
