package dev.fembyte.mirage;

import com.destroystokyo.paper.VersionHistoryManager;
import com.destroystokyo.paper.util.VersionFetcher;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.papermc.paper.ServerBuildInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

public final class MirageVersionFetcher implements VersionFetcher {

    private static final Logger LOGGER = Logger.getLogger(MirageVersionFetcher.class.getName());
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();
    private static final ServerBuildInfo BUILD_INFO = ServerBuildInfo.buildInfo();
    private static final String USER_AGENT = BUILD_INFO.brandName() + "/" + BUILD_INFO.asString(ServerBuildInfo.StringRepresentation.VERSION_SIMPLE) + " (https://github.com/dractical/mirage)";
    private static final String COMPARE_URL_FORMAT = "https://api.github.com/repos/dractical/mirage/compare/%s...%s";
    private static final HttpResponse.BodyHandler<JsonObject> JSON_BODY_HANDLER = responseInfo ->
            HttpResponse.BodySubscribers.mapping(
                    HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8),
                    body -> GSON.fromJson(body, JsonObject.class)
            );

    @Override
    public long getCacheTime() {
        return TimeUnit.MINUTES.toMillis(30);
    }

    @Override
    public @NotNull Component getVersionMessage() {
        final Optional<String> gitCommit = BUILD_INFO.gitCommit();
        final String gitBranch = BUILD_INFO.gitBranch().orElse("main");
        final Component status = gitCommit
                .map(commit -> fetchStatusFromGitHub(gitBranch, commit))
                .orElseGet(() -> text("Unknown server version.", RED));
        final @Nullable Component history = previousVersionComponent();
        return (history == null) ? status : Component.join(JoinConfiguration.noSeparators(), status, Component.newline(), history);
    }

    private @NotNull Component fetchStatusFromGitHub(final @NotNull String branch, final @NotNull String commitHash) {
        final URI uri = URI.create(String.format(COMPARE_URL_FORMAT, branch, commitHash));
        final HttpRequest request = HttpRequest.newBuilder(uri)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build();

        try {
            final HttpResponse<JsonObject> response = HTTP.send(request, JSON_BODY_HANDLER);

            if (response.statusCode() != 200) {
                if (response.statusCode() == 404) {
                    return text("Unknown version.", NamedTextColor.YELLOW);
                }
                return text("Received invalid status code (" + response.statusCode() + ") from server.", RED);
            }

            final JsonObject body = response.body();
            final int behindBy = body.get("behind_by").getAsInt();

            return versionStatusMessage(behindBy);
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.WARNING, "Failed to look up version from GitHub", e);

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            return text("Failed to retrieve version from server.", RED);
        }
    }

    private static @NotNull Component versionStatusMessage(final int behindBy) {
        final int state = Integer.compare(behindBy, 0);

        return switch (state) {
            case -1 -> text("You are running an unsupported version of Mirage.", RED);
            case 0 -> text("You are on the latest version!", GREEN);
            default -> text(
                    "You are running behind by " + behindBy + " version" + (behindBy == 1 ? "" : "s") + ". "
                            + "Please update your server when possible!", RED
            );
        };
    }

    private static @Nullable Component previousVersionComponent() {
        final VersionHistoryManager.VersionData data = VersionHistoryManager.INSTANCE.getVersionData();
        if (data == null) return null;

        final String oldVersion = data.getOldVersion();
        if (oldVersion == null) return null;

        return Component.text("Previous version: " + oldVersion, NamedTextColor.GRAY, TextDecoration.ITALIC);
    }

    public static void getUpdateStatusStartupMessage() {
        final Component message = new MirageVersionFetcher().getVersionMessage();
        LOGGER.info(PlainTextComponentSerializer.plainText().serialize(message));
    }
}
