package io.github.waqfs;

import io.github.waqfs.lib.ScoreboardL;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;

public class GameDetector {
    public static ParentGame rootGame = ParentGame.NULL;
    public static ChildGame subGame = ChildGame.NULL;

    public static void detect() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            GameDetector.unset();
            return;
        }

        final String BEDWARS_RED = Language.getPhrase(Language.Phrase.DETECTOR_BEDWARS_RED);
        final String MYSTERY_TIMELEFT = Language.getPhrase(Language.Phrase.DETECTOR_MYSTERY_TIMELEFT);
        final String MYSTERY_DETECTIVE = Language.getPhrase(Language.Phrase.DETECTOR_MYSTERY_DETECTIVE);
        final String MYSTERY_BOW = Language.getPhrase(Language.Phrase.DETECTOR_MYSTERY_BOW);
        final String MYSTERY_INFECTED = Language.getPhrase(Language.Phrase.DETECTOR_MYSTERY_INFECTED);
        final String MYSTERY_BOW1 = Language.getPhrase(Language.Phrase.DETECTOR_MYSTERY_BOW1);
        final String ZOMBIES_ZOMBIESLEFT = Language.getPhrase(Language.Phrase.ZOMBIES_ZOMBIESLEFT);

        String headerText = ScoreboardL.getUnformattedHeader(client);
        String[] rowsText = ScoreboardL.getUnformattedRows(client);
        switch (headerText) {
            case "SKYBLOCK" -> {
                GameDetector.rootGame = ParentGame.SKYBLOCK;
                GameDetector.subGame = ChildGame.NULL;
            }
            case "BED WARS" -> {
                GameDetector.rootGame = ParentGame.BEDWARS;
                if (GameDetector.some(rowsText, BEDWARS_RED)) {
                    GameDetector.subGame = ChildGame.INSTANCED_BEDWARS;
                } else {
                    GameDetector.subGame = ChildGame.NULL;
                }
            }
            case "MURDER MYSTERY" -> {
                GameDetector.rootGame = ParentGame.MURDER_MYSTERY;
                if (GameDetector.some(rowsText, MYSTERY_TIMELEFT)) {
                    if (GameDetector.some(rowsText, MYSTERY_DETECTIVE) || GameDetector.some(rowsText, MYSTERY_BOW)) {
                        GameDetector.subGame = ChildGame.CLASSIC_MYSTERY;
                    } else if (GameDetector.some(rowsText, MYSTERY_INFECTED)) {
                        GameDetector.subGame = ChildGame.INFECTION_MYSTERY;
                    } else if (GameDetector.some(rowsText, MYSTERY_BOW1)) {
                        GameDetector.subGame = ChildGame.DOUBLE_UP_MYSTERY;
                    } else {
                        GameDetector.subGame = ChildGame.NULL;
                    }
                } else {
                    GameDetector.subGame = ChildGame.NULL;
                }
            }
            case "ZOMBIES" -> {
                GameDetector.rootGame = ParentGame.ZOMBIES;
                if (GameDetector.some(rowsText, ZOMBIES_ZOMBIESLEFT)) {
                    GameDetector.subGame = ChildGame.INSTANCED_ZOMBIES;
                } else {
                    GameDetector.subGame = ChildGame.NULL;
                }
            }
            default -> {
                GameDetector.rootGame = ParentGame.NULL;
                GameDetector.subGame = ChildGame.NULL;
            }
        }
    }

    private static void unset() {
        GameDetector.rootGame = ParentGame.NULL;
        GameDetector.subGame = ChildGame.NULL;
    }

    private static boolean some(String[] target, String source) {
        for (String str : target) {
            if (str.contains(source)) return true;
        }
        return false;
    }

    private static boolean some(String target, String[] sources) {
        for (String str : sources) {
            if (target.contains(str)) return true;
        }
        return false;
    }

    private static @Nullable String find(String[] target, String source) {
        for (String str : target) {
            if (str.contains(source)) return str;
        }
        return null;
    }

    public enum ParentGame {
        NULL(0), SKYBLOCK(1), BEDWARS(2), MURDER_MYSTERY(3), ZOMBIES(4);

        private final int id;

        ParentGame(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    public enum ChildGame {
        NULL(0), INSTANCED_BEDWARS(1), CLASSIC_MYSTERY(2), DOUBLE_UP_MYSTERY(3), INFECTION_MYSTERY(4), INSTANCED_ZOMBIES(5);

        private final int id;

        ChildGame(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }
}
