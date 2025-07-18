package io.github.waqfs;

import io.github.waqfs.lib.ScoreboardL;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.jetbrains.annotations.Nullable;

public class GameDetector implements ClientModInitializer {
    public static ParentGame rootGame = ParentGame.NULL;
    public static ChildGame subGame = ChildGame.NULL;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                GameDetector.rootGame = ParentGame.NULL;
                GameDetector.subGame = ChildGame.NULL;
                return;
            }

//            ServerInfo serverInfo = client.getNetworkHandler().getServerInfo();
//            if (serverInfo == null) return;

            String headerText = ScoreboardL.getUnformattedHeader(client);
            String[] rowsText = ScoreboardL.getUnformattedRows(client);
            switch (headerText) {
                case "SKYBLOCK" -> {
                    GameDetector.rootGame = ParentGame.SKYBLOCK;
                    GameDetector.subGame = ChildGame.NULL;
                }
                case "BED WARS" -> {
                    GameDetector.rootGame = ParentGame.BEDWARS;
                    if (this.some(rowsText, "Red")) {
                        GameDetector.subGame = ChildGame.INSTANCED_BEDWARS;
                    } else {
                        GameDetector.subGame = ChildGame.NULL;
                    }
                }
                case "MURDER MYSTERY" -> {
                    GameDetector.rootGame = ParentGame.MURDER_MYSTERY;
                    if (this.some(rowsText, "Time Left")) {
                        if (this.some(rowsText, "Detective:") || this.some(rowsText, "Bow:")) {
                            GameDetector.subGame = ChildGame.CLASSIC_MYSTERY;
                        } else if (this.some(rowsText, "Infected")) {
                            GameDetector.subGame = ChildGame.INFECTION_MYSTERY;
                        } else if (this.some(rowsText, "Bow #1")) {
                            GameDetector.subGame = ChildGame.DOUBLE_UP_MYSTERY;
                        } else {
                            GameDetector.subGame = ChildGame.NULL;
                        }
                    } else {
                        GameDetector.subGame = ChildGame.NULL;
                    }
                }
                default -> {
                    GameDetector.rootGame = ParentGame.NULL;
                    GameDetector.subGame = ChildGame.NULL;
                }
            }
        });
    }

    private boolean some(String[] target, String source) {
        for (String str : target) {
            if (str.contains(source)) return true;
        }
        return false;
    }

    private @Nullable String find(String[] target, String source) {
        for (String str : target) {
            if (str.contains(source)) return str;
        }
        return null;
    }

    public enum ParentGame {
        NULL(0), SKYBLOCK(1), BEDWARS(2), MURDER_MYSTERY(3);

        private final int id;

        ParentGame(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    public enum ChildGame {
        NULL(0), INSTANCED_BEDWARS(1), CLASSIC_MYSTERY(2), DOUBLE_UP_MYSTERY(3), INFECTION_MYSTERY(4);

        private final int id;

        ChildGame(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }
}
