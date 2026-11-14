package org.fuin.porelo.agent.impl.pi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.fuin.porelo.agent.api.Finding;
import org.fuin.porelo.agent.api.Finding.Kind;
import org.fuin.porelo.agent.api.Finding.SignedIn;
import org.fuin.porelo.agent.api.Finding.What;
import org.fuin.porelo.agent.impl.pi.PiFindings.JsonFile;
import org.jspecify.annotations.Nullable;

/**
 * What a sign-in Pi keeps in its {@code auth.json} says of itself beside its finding, for a person
 * who chooses which one to carry to a host: the account, when it expires and when it last changed.
 * Never its value; the value goes to Porelo through {@link PiCredentialExtractor} alone.
 *
 * <p>The file holds one entry per provider, so the facts are the entry of the finding's provider.
 * None of the entries pi-ai writes for Anthropic, OpenRouter or GitHub Copilot names an account,
 * and a token is never decoded for one, so the account is always empty; it stays in the record so
 * the facts have the same fields for every agent. The expiry is the entry's {@code expires}, in
 * epoch milliseconds, and only where the import carries it: Anthropic's subscription. OpenRouter's
 * key never expires, and Copilot's expiry is its session token's, not the GitHub token's that is
 * carried. The last change is the file's own time, which a renewal moves.
 *
 * @param finding The finding these facts belong to.
 * @param account The account the sign-in is for, or {@code null} since no entry names one.
 * @param expires When the carried token expires, or {@code null} where none is held.
 * @param changed When {@code auth.json} was last written, or {@code null} where that cannot be
 *     read.
 */
record PiSignInFacts(
        Finding finding,
        @Nullable String account,
        @Nullable Instant expires,
        @Nullable Instant changed) {

    private static final String AUTH_FILE = "auth.json";
    private static final String COPILOT = "github-copilot";

    /**
     * Returns the facts of each finding the import can take, in the findings' order. A key given as
     * a command or a variable, one in {@code models.json} and a sign-in without a provider of
     * Porelo's are left out: an import does not take them.
     *
     * @param findings What {@link PiFindings} found in the directory.
     * @param configDirectory Pi's configuration directory.
     * @return The facts, one per sign-in the import can take.
     */
    static List<PiSignInFacts> of(final List<Finding> findings, final Path configDirectory) {
        final Path file = configDirectory.toAbsolutePath().normalize().resolve(AUTH_FILE);
        final JsonFile auth = JsonFile.read(file);
        if (auth.state() != JsonFile.State.OBJECT) {
            return List.of();
        }
        final @Nullable Instant changed = changed(file);
        return findings.stream()
                .filter(finding -> finding.what() == What.SIGN_IN)
                .filter(finding -> finding.signedIn() == SignedIn.SIGNED_IN)
                .filter(finding -> Boolean.TRUE.equals(finding.importable()))
                .filter(finding -> file.toString().equals(finding.path()))
                .<PiSignInFacts>mapMulti(
                        (finding, out) -> {
                            final @Nullable String provider = finding.provider();
                            if (provider != null
                                    && auth.root().get(provider) instanceof Map<?, ?> entry) {
                                out.accept(
                                        new PiSignInFacts(
                                                finding, null, expires(finding, entry), changed));
                            }
                        })
                .toList();
    }

    private static @Nullable Instant expires(final Finding finding, final Map<?, ?> entry) {
        if (finding.kind() == Kind.SUBSCRIPTION
                && !COPILOT.equals(finding.provider())
                && "oauth".equals(entry.get("type"))
                && entry.get("expires") instanceof Number millis
                && millis.longValue() > 0) {
            return Instant.ofEpochMilli(millis.longValue());
        }
        return null;
    }

    private static @Nullable Instant changed(final Path file) {
        try {
            return Files.getLastModifiedTime(file).toInstant();
        } catch (IOException ex) {
            return null;
        }
    }
}
