package org.fuin.porelo.agent.impl.pi;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.fuin.porelo.agent.api.AgentException;
import org.fuin.porelo.agent.api.Credential;
import org.fuin.porelo.agent.api.CredentialExtractor;
import org.fuin.porelo.agent.api.Finding;
import org.fuin.porelo.agent.api.Finding.Kind;
import org.fuin.porelo.agent.impl.pi.PiFindings.JsonFile;
import org.jspecify.annotations.Nullable;

/**
 * Takes a sign-in out of Pi's {@code auth.json}, the one {@link PiFindings} says an import can
 * take.
 *
 * <p>One file holds a sign-in per provider, so the provider asked for decides. What each provider's
 * entry holds is read in pi-ai 1.4.0: Anthropic's {@code oauth} entry renews like Claude Code's,
 * with the same client; OpenRouter's flow ends in a key, kept as {@code access}; Copilot's {@code
 * access} is a session token and its {@code refresh} the GitHub token Porelo keeps for Copilot.
 */
class PiCredentialExtractor implements CredentialExtractor {

    private static final String AUTH_FILE = "auth.json";
    private static final String API_KEY = "api-key";
    private static final String OAUTH = "oauth";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String ANTHROPIC = "anthropic";
    private static final String COPILOT = "github-copilot";

    /** Where pi-ai 1.4.0 renews an Anthropic sign-in, {@code dist/auth/oauth/anthropic.js}. */
    private static final String ANTHROPIC_TOKEN_URL = "https://platform.claude.com/v1/oauth/token";

    /** The client pi-ai 1.4.0 signs in to Anthropic as, in the same file. */
    private static final String ANTHROPIC_CLIENT_ID = "9d1c250a-e61b-44d9-88ed-5944d1962f5e";

    @Override
    public Optional<Credential> extract(final Path configDirectory) {
        return extract(configDirectory, null);
    }

    @Override
    public Optional<Credential> extract(
            final Path configDirectory, final @Nullable String provider) {

        final Path directory = configDirectory.toAbsolutePath().normalize();
        final Path file = directory.resolve(AUTH_FILE);
        final JsonFile auth = JsonFile.read(file);
        if (auth.state() == JsonFile.State.ABSENT) {
            return Optional.empty();
        }
        if (auth.state() != JsonFile.State.OBJECT
                || !auth.root().values().stream().allMatch(PiFindings::isValid)) {
            throw new AgentException(file + " cannot be read, or Pi would refuse it");
        }
        return new PiFindings()
                .find(directory).stream()
                        .filter(finding -> Boolean.TRUE.equals(finding.importable()))
                        .filter(finding -> file.toString().equals(finding.path()))
                        .filter(finding -> provider == null || provider.equals(finding.provider()))
                        .findFirst()
                        .map(finding -> credential(finding, auth.root()));
    }

    private static Credential credential(final Finding finding, final Map<?, ?> entries) {
        final String provider = String.valueOf(finding.provider());
        final Map<?, ?> entry = entries.get(provider) instanceof Map<?, ?> map ? map : Map.of();
        if (!OAUTH.equals(entry.get("type"))) {
            return new Credential(API_KEY, String.valueOf(entry.get("key")), Map.of(), provider);
        }
        final String access = String.valueOf(entry.get("access"));
        if (provider.equals(COPILOT)) {
            return new Credential(
                    ACCESS_TOKEN, String.valueOf(entry.get("refresh")), Map.of(), provider);
        }
        if (finding.kind() == Kind.API_KEY) {
            return new Credential(API_KEY, access, Map.of(), provider);
        }
        return new Credential(OAUTH, access, renewal(provider, entry), provider);
    }

    private static Map<String, String> renewal(final String provider, final Map<?, ?> entry) {
        final Map<String, String> attributes = new LinkedHashMap<>();
        if (provider.equals(ANTHROPIC)
                && entry.get("refresh") instanceof String refresh
                && !refresh.isBlank()) {
            attributes.put(Credential.REFRESH_TOKEN, refresh);
            attributes.put(Credential.TOKEN_URL, ANTHROPIC_TOKEN_URL);
            attributes.put(Credential.CLIENT_ID, ANTHROPIC_CLIENT_ID);
        }
        if (entry.get("expires") instanceof Number expires) {
            attributes.put(Credential.EXPIRES_AT, String.valueOf(expires.longValue()));
        }
        return attributes;
    }
}
