package io.github.waqfs.agent;

import io.github.waqfs.GameDetector;
import io.github.waqfs.Language;
import io.github.waqfs.lib.PlayerEntityL;
import io.github.waqfs.lib.TextL;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class MurderMysteryAgent implements ClientModInitializer {
    private static final HashMap<String, PersistentPlayer> persistentPlayers = new HashMap<>();
    private static final HashSet<AvailableGold> availableGold = new HashSet<>();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.world == null || GameDetector.rootGame != GameDetector.ParentGame.MURDER_MYSTERY || GameDetector.subGame == GameDetector.ChildGame.NULL) {
                this.unset();
                return;
            }

            this.cleanupAvailableGold();
            String[] knives = Language.getPhraseFromAll(Language.Phrase.MYSTERY_KNIFE);

            for (Entity entity : client.world.getEntities()) {
                if (entity instanceof PlayerEntity player) {
                    PersistentPlayer existingPlayer = this.getOrCreatePersistentPlayer(player);

                    ItemStack item = PlayerEntityL.getHeldItem(player);
                    if (item == null) continue;
                    if (this.isDetectiveItem(item)) {
                        existingPlayer.setDetective();
                        continue;
                    }

                    String itemName = TextL.toColorCodedString(item.getFormattedName());
                    if (!itemName.startsWith("§r§a")) continue;
                    String knifeLang = itemName.substring(4);
                    for (String knife : knives) {
                        if (knife.equals(knifeLang)) {
                            existingPlayer.setMurderer();
                            break;
                        }
                    }
                    continue;
                }
                if (entity instanceof ItemEntity item) {
                    ItemStack stack = item.getStack();
                    if (stack.isOf(Items.GOLD_INGOT)) {
                        if (this.goldExists(item)) continue;
                        this.createGold(item);
                        continue;
                    }
                }
            }
        });
    }

    public static HashSet<PersistentPlayer> getVisiblePlayers() {
        HashSet<PersistentPlayer> visiblePlayers = new HashSet<>();
        for (PersistentPlayer player : persistentPlayers.values()) {
            if (!player.playerEntity.isAlive()) continue;
            visiblePlayers.add(player);
        }
        return visiblePlayers;
    }

    public static HashSet<AvailableGold> getVisibleGold() {
        return new HashSet<>(availableGold);
    }

    private void cleanupAvailableGold() {
        HashSet<AvailableGold> scheduleGold = new HashSet<>();
        for (AvailableGold gold : availableGold) {
            if (gold.itemEntity.isAlive()) continue;
            scheduleGold.add(gold);
        }
        for (AvailableGold gold : scheduleGold) {
            availableGold.remove(gold);
        }
    }

    private boolean isDetectiveItem(ItemStack item) {
        if (item.isOf(Items.ARROW)) return true;
        if (item.isOf(Items.BOW)) return true;
        return false;
    }

    private PersistentPlayer getOrCreatePersistentPlayer(PlayerEntity player) {
        String playerName = player.getNameForScoreboard();
        PersistentPlayer persistPlayer = persistentPlayers.get(playerName);
        if (persistPlayer == null) {
            persistPlayer = new PersistentPlayer(player);
            persistentPlayers.put(playerName, persistPlayer);
        }
        persistPlayer.setPlayerEntity(player);
        return persistPlayer;
    }

    private boolean goldExists(ItemEntity item) {
        UUID uuid = item.getUuid();
        for (AvailableGold gold : availableGold) {
            if (gold.uuid == uuid) return true;
        }
        return false;
    }

    private void createGold(ItemEntity item) {
        AvailableGold gold = new AvailableGold(item);
        availableGold.add(gold);
    }

    private void unset() {
        persistentPlayers.clear();
        availableGold.clear();
    }

    public static class PersistentPlayer {
        public PlayerEntity playerEntity;
        public final String name;
        public Role role;

        public PersistentPlayer(PlayerEntity playerEntity) {
            this.playerEntity = playerEntity;
            this.name = playerEntity.getNameForScoreboard();
            this.role = Role.INNOCENT;
        }

        protected void setPlayerEntity(PlayerEntity playerEntity) {
            this.playerEntity = playerEntity;
        }

        public void setMurderer() {
            this.role = Role.MURDERER;
        }

        public void setDetective() {
            if (this.role == Role.MURDERER) return;
            this.role = Role.DETECTIVE;
        }

        public enum Role {
            INNOCENT(0), MURDERER(1), DETECTIVE(2);
            private final int id;

            Role(int id) {
                this.id = id;
            }

            public int getId() {
                return id;
            }
        }
    }

    public static class AvailableGold {
        public final ItemEntity itemEntity;
        public final UUID uuid;

        public AvailableGold(ItemEntity itemEntity) {
            this.itemEntity = itemEntity;
            this.uuid = itemEntity.getUuid();
        }
    }
}
