package dev.cigarette;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.cigarette.config.FileSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class Language implements ClientModInitializer {
    private static final HashMap<Lang, LanguageMapping> LANGUAGE_MAP = new HashMap<>();
    private static final Lang[] SUPPORTED_LANGUAGES = new Lang[] { Lang.ENGLISH, Lang.SPANISH, Lang.CHINESE_SIMPLIFIED,
            Lang.CHINESE_TRADITIONAL, Lang.CZECH, Lang.DANISH, Lang.DUTCH, Lang.FINNISH, Lang.FRENCH, Lang.GERMAN,
            Lang.HUNGARIAN, Lang.ITALIAN, Lang.JAPANESE, Lang.KOREAN, Lang.NORWEGIAN, Lang.PIRATE, Lang.POLISH,
            Lang.PORTUGUESE_BR, Lang.PORTUGUESE_PT, Lang.ROMANIAN, Lang.RUSSIAN, Lang.SWEDISH, Lang.TURKISH,
            Lang.UKRAINIAN };
    private static Lang SELECTED_LANGUAGE = Lang.ENGLISH;

    private static void saveToConfig() {
        FileSystem.updateState("language", SELECTED_LANGUAGE.getId());
    }

    private static boolean trySetLang(String lang) {
        for (Lang supportedLang : SUPPORTED_LANGUAGES) {
            if (supportedLang.getId().equals(lang.toUpperCase())) {
                SELECTED_LANGUAGE = supportedLang;
                saveToConfig();
                return true;
            }
        }
        return false;
    }

    private static int setLang(CommandContext<FabricClientCommandSource> context) {
        String lang = StringArgumentType.getString(context, "lang");
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return 1;
        }
        boolean set = trySetLang(lang);
        if (set) {
            Cigarette.CHAT_LOGGER.info("Updated language to " + lang + ".");
        } else {
            Cigarette.CHAT_LOGGER.info("Language not supported.");
        }
        MinecraftClient.getInstance().player.networkHandler.sendChatCommand("language " + lang);
        return 1;
    }

    @Override
    public void onInitializeClient() {
        FileSystem.registerUpdate("language", newState -> {
            if (newState instanceof String) {
                trySetLang((String) newState);
            } else {
                saveToConfig();
            }
        });
        ClientLifecycleEvents.CLIENT_STARTED.register(event -> {
            ResourceManager manager = MinecraftClient.getInstance().getResourceManager();
            for (Lang supportedLang : SUPPORTED_LANGUAGES) {
                Optional<Resource> resource = manager
                        .getResource(Identifier.of("cigarette", supportedLang.getResourcePath()));
                if (resource.isEmpty())
                    continue;
                HashMap<Phrase, String> phrases = new HashMap<>();
                try (BufferedReader reader = resource.get().getReader()) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.contains("="))
                            continue;
                        String[] split = line.split("=");
                        if (split.length != 2)
                            continue;
                        Phrase phrase = Phrase.from(split[0]);
                        if (phrase != null)
                            phrases.put(phrase, split[1]);
                    }
                } catch (IOException ignored) {
                }
                LANGUAGE_MAP.put(supportedLang, new LanguageMapping(supportedLang, phrases));
            }
            ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
                dispatcher.register(
                        ClientCommandManager.literal("lang")
                                .then(
                                        ClientCommandManager.argument("lang", StringArgumentType.string())
                                                .suggests(new LanguageSuggestionProvider())
                                                .executes(Language::setLang)));
            });
        });
    }

    public static @Nullable String getPhrase(Phrase phrase) {
        LanguageMapping map = LANGUAGE_MAP.get(SELECTED_LANGUAGE);
        if (map == null)
            return null;
        return map.getPhrase(phrase);
    }

    public static @Nullable String[] getPhraseFromAll(Phrase phrase) {
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

    private static class LanguageSuggestionProvider implements SuggestionProvider<FabricClientCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context,
                SuggestionsBuilder builder) {
            for (Lang lang : SUPPORTED_LANGUAGES) {
                builder.suggest(lang.getId());
            }
            return builder.buildFuture();
        }
    }

    public enum Lang {
        ENGLISH("ENGLISH", "lang/english.lang"), SPANISH("SPANISH", "lang/spanish.lang"),
        CHINESE_SIMPLIFIED("CHINESE_SIMPLIFIED", "lang/chinese_simplified.lang"),
        CHINESE_TRADITIONAL("CHINESE_TRADITIONAL", "lang/chinese_traditional.lang"), CZECH("CZECH", "lang/czech.lang"),
        DANISH("DANISH", "lang/danish.lang"), DUTCH("DUTCH", "lang/dutch.lang"),
        FINNISH("FINNISH", "lang/finnish.lang"), FRENCH("FRENCH", "lang/french.lang"),
        GERMAN("GERMAN", "lang/german.lang"), HUNGARIAN("HUNGARIAN", "lang/hungarian.lang"),
        ITALIAN("ITALIAN", "lang/italian.lang"), JAPANESE("JAPANESE", "lang/japanese.lang"),
        KOREAN("KOREAN", "lang/korean.lang"), NORWEGIAN("NORWEGIAN", "lang/norwegian.lang"),
        PIRATE("PIRATE", "lang/pirate.lang"), POLISH("POLISH", "lang/polish.lang"),
        PORTUGUESE_BR("PORTUGUESE_BR", "lang/portuguese_br.lang"),
        PORTUGUESE_PT("PORTUGUESE_PT", "lang/portuguese_pt.lang"), ROMANIAN("ROMANIAN", "lang/romanian.lang"),
        RUSSIAN("RUSSIAN", "lang/russian.lang"), SWEDISH("SWEDISH", "lang/swedish.lang"),
        TURKISH("TURKISH", "lang/turkish.lang"), UKRAINIAN("UKRAINIAN", "lang/ukrainian.lang");

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
        BEDWARS_HEADER("bedwars.header"), DETECTOR_BEDWARS_RED("bedwars.red"), MYSTERY_HEADER("murdermystery.header"),
        DETECTOR_MYSTERY_TIMELEFT("murdermystery.timeleft"), DETECTOR_MYSTERY_DETECTIVE("murdermystery.detective"),
        DETECTOR_MYSTERY_BOW("murdermystery.bow"), DETECTOR_MYSTERY_INFECTED("murdermystery.infected"),
        DETECTOR_MYSTERY_BOW1("murdermystery.bow1"), MYSTERY_KNIFE("murdermystery.knife"),
        SKYBLOCK_HEADER("skyblock.header"), PIT_HEADER("pit.header"), ZOMBIES_HEADER("zombies.header"),
        ZOMBIES_ZOMBIESLEFT("zombies.zombiesleft");

        private final String key;

        Phrase(String key) {
            this.key = key;
        }

        public String getKey() {
            return this.key;
        }

        public static @Nullable Phrase from(String key) {
            for (Phrase phr : Phrase.values()) {
                if (phr.getKey().equals(key))
                    return phr;
            }
            return null;
        }
    }
}
