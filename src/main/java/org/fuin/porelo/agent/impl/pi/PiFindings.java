package org.fuin.porelo.agent.impl.pi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.fuin.porelo.agent.api.Finder;
import org.fuin.porelo.agent.api.Finding;
import org.fuin.porelo.agent.api.Finding.Kind;
import org.fuin.porelo.agent.api.Finding.SignedIn;
import org.fuin.porelo.agent.api.Finding.What;
import org.fuin.porelo.wire.Json;
import org.fuin.porelo.wire.JsonException;
import org.jspecify.annotations.Nullable;

/**
 * What Pi finds of itself on the host, from its {@code auth.json} and {@code models.json}.
 *
 * <p>Only reads files: it runs no command a key names, resolves no variable, starts nothing and
 * reaches no network, and reads no environment, since an agent the daemon spawns does not share the
 * person's shell. So a key that depends on Pi's environment cannot be told. What each entry means
 * is read in Pi 1.4.0's package and its pi-ai 1.4.0.
 *
 * <p>Pi's provider ids are the names Porelo gives the same providers. The default the person gave
 * Pi comes first, then the ones Porelo has in the order Pi prefers them, since the first finding is
 * what Porelo names; any other sign-in goes last, whatever Pi would do with it.
 */
final class PiFindings implements Finder {

    private static final String AUTH_FILE = "auth.json";
    private static final String MODELS_FILE = "models.json";
    private static final String SETTINGS_FILE = "settings.json";
    private static final String COPILOT = "github-copilot";
    private static final String OPENROUTER = "openrouter";
    private static final String ANTHROPIC = "anthropic";

    /** Providers Porelo declares under Pi's same id, in the order of Pi's own default models. */
    private static final List<String> PROVIDERS =
            List.of(ANTHROPIC, "openai", "google", COPILOT, OPENROUTER);

    /** The ones of them whose Pi sign-in Porelo can use: the others' entries are API keys. */
    private static final Set<String> SIGN_IN_PROVIDERS = Set.of(ANTHROPIC, COPILOT, OPENROUTER);

    /** Reads a file's text; what fails to read is a file Porelo cannot say anything about. */
    @FunctionalInterface
    interface FileReader {

        /**
         * Returns a file's text.
         *
         * @param path The file.
         * @return Its content, as UTF-8.
         * @throws IOException If it cannot be read.
         */
        String read(Path path) throws IOException;
    }

    /** Reads a file from the file system. */
    static final FileReader FILE_SYSTEM = path -> Files.readString(path, StandardCharsets.UTF_8);

    private final FileReader reader;

    /** Reads the files from the file system. */
    PiFindings() {
        this(FILE_SYSTEM);
    }

    /**
     * Reads the files with the reader given, so a file no account can read is made for any account.
     *
     * @param reader How a file's text is read.
     */
    PiFindings(final FileReader reader) {
        this.reader = reader;
    }

    @Override
    public List<Finding> find(final Path configDirectory) {

        final Path directory = configDirectory.toAbsolutePath().normalize();
        final List<Finding> findings = new ArrayList<>();
        if (Files.isDirectory(directory)) {
            findings.add(
                    new Finding(What.CONFIGURATION, directory.toString(), null, null, null, null));
        }

        final JsonFile auth = JsonFile.read(reader, directory.resolve(AUTH_FILE));
        final JsonFile models = JsonFile.read(reader, directory.resolve(MODELS_FILE));
        final String preferred =
                preferredProvider(JsonFile.read(reader, directory.resolve(SETTINGS_FILE)));
        final Map<?, ?> configured = configuredProviders(models);

        final boolean authUsable =
                auth.state() == JsonFile.State.OBJECT
                        && auth.root().values().stream().allMatch(PiFindings::isValid);
        final boolean modelsUsable =
                models.state() == JsonFile.State.OBJECT
                        && configured.values().stream().allMatch(PiFindings::isValidProvider);
        // A file that cannot be read may move any provider elsewhere, so no key beside it is sure.
        final boolean modelsKnown =
                models.state() == JsonFile.State.OBJECT || models.state() == JsonFile.State.ABSENT;
        final List<Route> routes = new ArrayList<>();
        if (authUsable) {
            auth.root()
                    .forEach(
                            (provider, entry) ->
                                    routes.add(
                                            new Route(
                                                    provider,
                                                    fromAuth(
                                                            auth.path(),
                                                            modelsKnown
                                                                    ? portable(provider, configured)
                                                                    : "",
                                                            entry))));
        }
        if (modelsUsable) {
            configured.forEach(
                    (provider, entry) -> {
                        if (!auth.root().containsKey(provider)
                                && entry instanceof Map<?, ?> map
                                && map.get("apiKey") instanceof String key) {
                            routes.add(
                                    new Route(
                                            provider,
                                            fromKey(
                                                    models.path(),
                                                    portable(provider, configured),
                                                    key,
                                                    false)));
                        }
                    });
        }
        routes.stream()
                .sorted(Comparator.comparingInt(route -> rank(route.provider(), preferred)))
                .map(Route::finding)
                .forEach(findings::add);
        if (auth.state() != JsonFile.State.ABSENT && !authUsable) {
            findings.add(cannotSay(auth.path()));
        }
        if (models.state() != JsonFile.State.ABSENT && !modelsUsable) {
            findings.add(cannotSay(models.path()));
        }
        return List.copyOf(findings);
    }

    private static Map<?, ?> configuredProviders(final JsonFile models) {
        return models.root().get("providers") instanceof Map<?, ?> providers ? providers : Map.of();
    }

    /** Pi rejects the whole file for a key that is empty or no text. */
    private static boolean isValidProvider(final Object entry) {
        return !(entry instanceof Map<?, ?> map)
                || !map.containsKey("apiKey")
                || isSet(map.get("apiKey"));
    }

    /**
     * Returns the name a credential goes by: a provider moved to another address is none Porelo
     * has, whichever file holds its key, which the empty name says.
     */
    private static Object portable(final Object provider, final Map<?, ?> configured) {
        return configured.get(provider) instanceof Map<?, ?> map && map.containsKey("baseUrl")
                ? ""
                : provider;
    }

    /** Pi takes a default only with its model. */
    private static @Nullable String preferredProvider(final JsonFile settings) {
        return settings.root().get("defaultProvider") instanceof String provider
                        && settings.root().get("defaultModel") instanceof String
                ? provider
                : null;
    }

    /** One entry that fails this makes Pi refuse the whole file, measured on 1.4.0. */
    static boolean isValid(final Object entry) {
        if (!(entry instanceof Map<?, ?> map)) {
            return false;
        }
        return switch (String.valueOf(map.get("type"))) {
            case "api_key" ->
                    (map.get("key") == null || map.get("key") instanceof String)
                            && (map.get("env") == null
                                    || map.get("env") instanceof Map<?, ?> env
                                            && env.values().stream()
                                                    .allMatch(String.class::isInstance));
            case "oauth" ->
                    map.get("access") instanceof String
                            && map.get("refresh") instanceof String
                            && map.get("expires") instanceof Number;
            default -> false;
        };
    }

    private static int rank(final Object provider, final @Nullable String preferred) {
        if (provider.equals(preferred)) {
            return -1;
        }
        final int index = PROVIDERS.indexOf(provider);
        return index < 0 ? PROVIDERS.size() : index;
    }

    private static Finding fromAuth(final Path file, final Object provider, final Object entry) {
        if (entry instanceof Map<?, ?> map) {
            if ("api_key".equals(map.get("type")) && map.get("key") instanceof String key) {
                return fromKey(file, provider, key, true);
            }
            if ("oauth".equals(map.get("type")) && isSet(map.get("access"))) {
                return fromSignIn(file, provider, map);
            }
        }
        return cannotSay(file);
    }

    private static Finding fromKey(
            final Path file, final Object provider, final String key, final boolean stored) {
        final boolean command = key.startsWith("!");
        if (key.isBlank() || !command && key.indexOf('$') >= 0) {
            return cannotSay(file);
        }
        final boolean known = PROVIDERS.contains(provider);
        return signedIn(
                file,
                stored && known && !command,
                known ? provider.toString() : null,
                Kind.API_KEY);
    }

    private static Finding fromSignIn(
            final Path file, final Object provider, final Map<?, ?> entry) {
        if (!SIGN_IN_PROVIDERS.contains(provider)) {
            return signedIn(file, false, null, null);
        }
        if (provider.equals(OPENROUTER)) {
            return signedIn(file, true, OPENROUTER, Kind.API_KEY);
        }
        if (provider.equals(COPILOT)) {
            if (isSet(entry.get("enterpriseUrl"))) {
                return signedIn(file, false, null, Kind.SUBSCRIPTION);
            }
            return signedIn(file, isSet(entry.get("refresh")), COPILOT, Kind.SUBSCRIPTION);
        }
        return signedIn(file, true, provider.toString(), Kind.SUBSCRIPTION);
    }

    private static Finding signedIn(
            final Path file,
            final boolean importable,
            final @Nullable String provider,
            final @Nullable Kind kind) {
        return new Finding(
                What.SIGN_IN, file.toString(), SignedIn.SIGNED_IN, importable, provider, kind);
    }

    private static Finding cannotSay(final Path file) {
        return new Finding(What.SIGN_IN, file.toString(), SignedIn.CANNOT_SAY, null, null, null);
    }

    private static boolean isSet(final @Nullable Object value) {
        return value instanceof String text && !text.isBlank();
    }

    /** One provider's finding, kept with its name only to put it in order. */
    private record Route(Object provider, Finding finding) {}

    /** A JSON file, read once; a value it holds never leaves this class. */
    record JsonFile(Path path, State state, Map<?, ?> root) {

        enum State {
            ABSENT,
            UNREADABLE,
            BROKEN,
            OBJECT
        }

        static JsonFile read(final Path path) {
            return read(FILE_SYSTEM, path);
        }

        static JsonFile read(final FileReader reader, final Path path) {
            final String content;
            try {
                content = reader.read(path);
            } catch (NoSuchFileException ex) {
                return new JsonFile(path, State.ABSENT, Map.of());
            } catch (IOException ex) {
                return new JsonFile(path, State.UNREADABLE, Map.of());
            }
            final String text = content.startsWith("﻿") ? content.substring(1) : content;
            try {
                return Json.parse(text) instanceof Map<?, ?> map
                        ? new JsonFile(path, State.OBJECT, map)
                        : new JsonFile(path, State.BROKEN, Map.of());
            } catch (JsonException ex) {
                return new JsonFile(path, State.BROKEN, Map.of());
            }
        }
    }
}
