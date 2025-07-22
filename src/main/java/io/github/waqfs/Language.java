package io.github.waqfs;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

public class Language implements ClientModInitializer {
    private static final HashMap<Lang, LanguageMapping> LANGUAGE_MAP = new HashMap<>();
    private static final Lang[] SUPPORTED_LANGUAGES = new Lang[]{Lang.ENGLISH};
    private static Lang SELECTED_LANGUAGE = Lang.ENGLISH;

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(event -> {
            ResourceManager manager = MinecraftClient.getInstance().getResourceManager();
            for (Lang supportedLang : SUPPORTED_LANGUAGES) {
                Optional<Resource> resource = manager.getResource(Identifier.of("cigarette", supportedLang.getResourcePath()));
                if (resource.isEmpty()) continue;
                HashMap<Phrase, String> phrases = new HashMap<>();
                try (BufferedReader reader = resource.get().getReader()) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.contains("=")) continue;
                        String[] split = line.split("=");
                        Phrase phrase = Phrase.from(split[0]);
                        if (phrase != null) phrases.put(phrase, split[1]);
                    }
                } catch (IOException ignored) {
                }
                LANGUAGE_MAP.put(supportedLang, new LanguageMapping(supportedLang, phrases));
            }
        });
    }

    public static String getPhrase(Phrase phrase) {
        LanguageMapping map = LANGUAGE_MAP.get(SELECTED_LANGUAGE);
        if (map == null) return "undefined";
        return map.getPhrase(phrase);
    }

    public static String[] getPhraseFromAll(Phrase phrase) {
        String[] phrases = new String[LANGUAGE_MAP.size()];
        int i = 0;
        for (LanguageMapping map : LANGUAGE_MAP.values()) {
            phrases[i++] = map.getPhrase(phrase);
        }
        return phrases;
    }

    public static class LanguageMapping {
        public final Lang lang;
        private final HashMap<Phrase, String> map;

        public LanguageMapping(Lang languageKey, HashMap<Phrase, String> phrases) {
            this.lang = languageKey;
            this.map = phrases;
        }

        public String getPhrase(Phrase phrase) {
            return this.map.getOrDefault(phrase, "undefined");
        }
    }

    public enum Lang {
        ENGLISH("ENGLISH", "lang/english.lang");
        private final String id;
        private final String resourcePath;

        Lang(String id, String resourcePath) {
            this.id = id;
            this.resourcePath = resourcePath;
        }

        public String getId() {
            return id;
        }

        public String getResourcePath() {
            return resourcePath;
        }
    }

    public enum Phrase {
        DETECTOR_BEDWARS_RED("gamedetector.bedwars.red"), DETECTOR_MYSTERY_TIMELEFT("gamedetector.murdermystery.timeleft"), DETECTOR_MYSTERY_DETECTIVE("gamedetector.murdermystery.detective"), DETECTOR_MYSTERY_BOW("gamedetector.murdermystery.bow"), DETECTOR_MYSTERY_INFECTED("gamedetector.murdermystery.infected"), DETECTOR_MYSTERY_BOW1("gamedetector.murdermystery.bow1"), MYSTERY_KNIFE("murdermystery.knife");
        private final String key;

        Phrase(String key) {
            this.key = key;
        }

        public String getKey() {
            return this.key;
        }

        public static @Nullable Phrase from(String key) {
            for (Phrase phr : Phrase.values()) {
                if (phr.getKey().equals(key)) return phr;
            }
            return null;
        }
    }
}
