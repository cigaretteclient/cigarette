package io.github.waqfs.agent;

import io.github.waqfs.GameDetector;
import io.github.waqfs.lib.PlayerEntityL;
import io.github.waqfs.lib.TextL;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class MurderMysteryAgent implements ClientModInitializer {
    private static final HashMap<String, AlivePlayer.Role> persistentRoles = new HashMap<>();
    private static final HashSet<AlivePlayer> alivePlayers = new HashSet<>();
    private static final HashSet<AvailableGold> availableGold = new HashSet<>();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.world == null || GameDetector.rootGame != GameDetector.ParentGame.MURDER_MYSTERY || GameDetector.subGame == GameDetector.ChildGame.NULL) {
                this.unset();
                return;
            }

            this.cleanupAlivePlayers();
            this.cleanupAvailableGold();

            for (Entity entity : client.world.getEntities()) {
                if (entity instanceof PlayerEntity player) {
                    AlivePlayer existingPlayer = this.getAlivePlayer(player);
                    if (existingPlayer == null) {
                        existingPlayer = this.createAlivePlayer(player);
                    }

                    ItemStack item = PlayerEntityL.getHeldItem(player);
                    if (item == null) continue;
                    if (this.isNeutralItem(item)) continue;
                    if (this.isDetectiveItem(item)) {
                        existingPlayer.setDetective();
                        continue;
                    }

                    String itemName = TextL.toColorCodedString(item.getFormattedName());
                    if (itemName.startsWith("§r§a")) {
                        existingPlayer.setMurderer();
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

    public static HashSet<AlivePlayer> getAlivePlayers() {
        return new HashSet<>(alivePlayers);
    }

    public static HashSet<AvailableGold> getAvailableGold() {
        return new HashSet<>(availableGold);
    }

    private void cleanupAlivePlayers() {
        HashSet<AlivePlayer> scheduleDeadPlayers = new HashSet<>();
        for (AlivePlayer alivePlayer : alivePlayers) {
            if (alivePlayer.playerEntity.isAlive()) continue;
            scheduleDeadPlayers.add(alivePlayer);
        }
        for (AlivePlayer alivePlayer : scheduleDeadPlayers) {
            alivePlayers.remove(alivePlayer);
            alivePlayer.destroy();
        }
    }

    private void cleanupAvailableGold() {
        HashSet<AvailableGold> scheduleGold = new HashSet<>();
        for (AvailableGold gold : availableGold) {
            if (gold.itemEntity.isAlive()) continue;
            scheduleGold.add(gold);
        }
        for (AvailableGold gold : scheduleGold) {
            availableGold.remove(gold);
            gold.destroy();
        }
    }

    private boolean isNeutralItem(ItemStack item) {
        if (item.isOf(Items.FILLED_MAP)) return true;
        if (item.isOf(Items.GOLD_INGOT)) return true;
        if (item.isOf(Items.ARMOR_STAND)) return true;
        if (item.isOf(Items.FIREWORK_STAR)) return true;
        if (item.isOf(Items.BLUE_STAINED_GLASS)) return true;
        if (item.isOf(Items.COMPASS)) return true;
        if (item.isOf(Items.COMPARATOR)) return true;
        if (item.isOf(Items.RED_BED)) return true;
        return false;
    }

    private boolean isDetectiveItem(ItemStack item) {
        if (item.isOf(Items.ARROW)) return true;
        if (item.isOf(Items.BOW)) return true;
        return false;
    }

    private @Nullable AlivePlayer getAlivePlayer(PlayerEntity player) {
        for (AlivePlayer remPlayer : alivePlayers) {
            if (remPlayer.playerEntity == player) return remPlayer;
        }
        return null;
    }

    private boolean goldExists(ItemEntity item) {
        UUID uuid = item.getUuid();
        for (AvailableGold gold : availableGold) {
            if (gold.uuid == uuid) return true;
        }
        return false;
    }

    private AlivePlayer createAlivePlayer(PlayerEntity player) {
        AlivePlayer remPlayer = new AlivePlayer(player);
        alivePlayers.add(remPlayer);
        return remPlayer;
    }

    private void createGold(ItemEntity item) {
        AvailableGold gold = new AvailableGold(item);
        availableGold.add(gold);
    }

    private void unset() {
        persistentRoles.clear();

        for (AlivePlayer alivePlayer : alivePlayers) {
            alivePlayer.destroy();
        }
        alivePlayers.clear();

        for (AvailableGold gold : availableGold) {
            gold.destroy();
        }
        availableGold.clear();
    }

    public class AlivePlayer {
        public final PlayerEntity playerEntity;
        public final UUID uuid;
        public final String name;
        public Role role;
        private boolean exists;

        public AlivePlayer(PlayerEntity playerEntity) {
            this.playerEntity = playerEntity;
            this.uuid = playerEntity.getUuid();
            this.name = playerEntity.getNameForScoreboard();
            this.role = persistentRoles.getOrDefault(this.name, Role.INNOCENT);
            this.exists = true;
        }

        public void setMurderer() {
            this.role = Role.MURDERER;
            persistentRoles.put(this.name, Role.MURDERER);
        }

        public void setDetective() {
            if (this.role == Role.MURDERER) return;
            this.role = Role.DETECTIVE;
            persistentRoles.put(this.name, Role.DETECTIVE);
        }

        public boolean exists() {
            return this.exists;
        }

        public void destroy() {
            this.exists = false;
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

    public class AvailableGold {
        public final ItemEntity itemEntity;
        public final UUID uuid;
        private boolean exists;

        public AvailableGold(ItemEntity itemEntity) {
            this.itemEntity = itemEntity;
            this.uuid = itemEntity.getUuid();
            this.exists = true;
        }

        public boolean exists() {
            return this.exists;
        }

        public void destroy() {
            this.exists = false;
        }
    }
}
