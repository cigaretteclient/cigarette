package dev.cigarette.lib;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.cigarette.Cigarette;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.SharedConstants;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static dev.cigarette.Cigarette.MOD_ID;

public class VersionManager {
    private static final String FORGEJO_API = "https://git.okr765.com/api/v1/repos/cigarette/client";
    private static final String MC_VERSION = SharedConstants.getGameVersion().name();
    private static final String DISABLED_FILENAME = "cigarette-old.disabled.autoupdate";

    private static boolean updated = false;

    private static class Asset {
        private final String name;
        private final String browserDownloadUrl;

        public Asset(String name, String browserDownloadUrl) {
            this.name = name;
            this.browserDownloadUrl = browserDownloadUrl;
        }

        public String getName() {
            return name;
        }
        public String getBrowserDownloadUrl() {
            return browserDownloadUrl;
        }

        public static final Codec<Asset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(Asset::getName),
                Codec.STRING.fieldOf("browser_download_url").forGetter(Asset::getBrowserDownloadUrl)
        ).apply(instance, Asset::new));
    }

    private static class Release {
        private final String tagName;
        private final boolean prerelease;
        private final List<Asset> assets;

        public Release(String tagName, boolean prerelease, List<Asset> assets) {
            this.tagName = tagName;
            this.prerelease = prerelease;
            this.assets = assets;
        }

        public String getTagName() {
            return tagName;
        }
        public boolean isPrerelease() {
            return prerelease;
        }
        public List<Asset> getAssets() {
            return assets;
        }
        
        public static final Codec<Release> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("tag_name").forGetter(Release::getTagName),
                Codec.BOOL.fieldOf("prerelease").forGetter(Release::isPrerelease),
                Asset.CODEC.listOf().fieldOf("assets").forGetter(Release::getAssets)
        ).apply(instance, Release::new));
    }

    public static void update(String target, boolean ignoreMcVersion) {
        if (updated) {
            Cigarette.CHAT_LOGGER.info("Already updated, restart to take effect");
            return;
        }
        FabricLoader loader = FabricLoader.getInstance();
        ModContainer modContainer = loader.getModContainer(MOD_ID).orElseThrow();
        if (!modContainer.getOrigin().getPaths().getFirst().toString().endsWith(".jar")) {
            Cigarette.CHAT_LOGGER.info("Not currently running from a .jar file, cannot update");
            return;
        }
        if (target.isEmpty()) target = "latest";
        String finalTarget = target;
        new Thread(() -> {
            try (HttpClient client = HttpClient.newHttpClient()) {
                if (finalTarget.equals("latest") || finalTarget.equals("stable")) {
                    if (finalTarget.equals("latest")) {
                        Cigarette.CHAT_LOGGER.info("Updating to latest release");
                    } else {
                        Cigarette.CHAT_LOGGER.info("Updating to latest stable release");
                    }
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(FORGEJO_API + "/releases"))
                            .build();
                    client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                            .thenApply(HttpResponse::body)
                            .thenAccept((body) -> {
                                JsonElement json = JsonParser.parseString(body);
                                List<Release> releases = Release.CODEC.listOf().parse(JsonOps.INSTANCE, json).getOrThrow();
                                update(finalTarget, releases, ignoreMcVersion);
                            })
                            .exceptionally((err) -> {
                                Cigarette.CHAT_LOGGER.info("An error occurred fetching releases, check logs for details");
                                err.printStackTrace();
                                return null;
                            })
                            .join();
                } else if (finalTarget.startsWith("v") && finalTarget.contains(".")) {
                    Cigarette.CHAT_LOGGER.info("Updating to release " + finalTarget);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(FORGEJO_API + "/releases/tags/" + finalTarget))
                            .build();
                    client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                            .thenApply(HttpResponse::body)
                            .thenAccept((body) -> {
                                JsonElement json = JsonParser.parseString(body);
                                Release release = Release.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
                                List<Release> releases = new ArrayList<>();
                                releases.add(release);
                                update(finalTarget, releases, ignoreMcVersion);
                            })
                            .exceptionally((err) -> {
                                Cigarette.CHAT_LOGGER.info("Could not find release " + finalTarget);
                                return null;
                            })
                            .join();
                } else {
                    // the goal is to eventually add pulling an artifact,
                    // but we're waiting on the forgejo api to support this
                    //
                    // Probably could hack it in but just waiting for now
                    Cigarette.CHAT_LOGGER.info("Unknown update target " + finalTarget);
                }
            }
        }).start();
    }

    private static void update(String target, List<Release> releases, boolean ignoreMcVersion) {
        String downloadUrl = null;
        String assetName = null;
        outer: for (Release release : releases) {
            if (release.isPrerelease() && target.equals("stable")) continue;
            for (Asset asset : release.getAssets()) {
                if (ignoreMcVersion || asset.getName().endsWith("+" + MC_VERSION + ".jar")) {
                    downloadUrl = asset.getBrowserDownloadUrl();
                    assetName = asset.getName();
                    break outer;
                }
            }
        }
        if (downloadUrl == null) {
            Cigarette.CHAT_LOGGER.info("Could not find a compatible release for " + target);
            return;
        }
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .build();
            FabricLoader loader = FabricLoader.getInstance();
            ModContainer modContainer = loader.getModContainer(MOD_ID).orElseThrow();
            Path currentJar = modContainer.getOrigin().getPaths().getFirst();
            File disabled = currentJar.resolveSibling(DISABLED_FILENAME).toFile();
            if (!((!disabled.exists() || disabled.delete()) && currentJar.toFile().renameTo(disabled))) {
                Cigarette.CHAT_LOGGER.info("Failed to remove old version, cannot proceed");
                return;
            }
            String finalAssetName = assetName;
            client.sendAsync(request, HttpResponse.BodyHandlers.ofFile(currentJar.resolveSibling(assetName)))
                    .thenAccept((path) -> {
                        disabled.deleteOnExit();
                        updated = true;
                        Cigarette.CHAT_LOGGER.info("Successfully downloaded " + finalAssetName);
                        Cigarette.CHAT_LOGGER.info("Restart your game to complete the update");
                    })
                    .exceptionally((err) -> {
                        Cigarette.CHAT_LOGGER.info("Failed to download new version");
                        err.printStackTrace();
                        return null;
                    })
                    .join();
        }
    }
}
