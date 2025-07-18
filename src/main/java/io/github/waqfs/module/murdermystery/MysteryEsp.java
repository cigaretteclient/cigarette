package io.github.waqfs.module.murdermystery;

import io.github.waqfs.GameDetector;
import io.github.waqfs.lib.Glow;
import io.github.waqfs.lib.PlayerEntityL;
import io.github.waqfs.lib.TextL;
import io.github.waqfs.module.BaseModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.HashMap;
import java.util.UUID;

public class MysteryEsp extends BaseModule {
    protected static final String MODULE_NAME = "PlayerESP";
    protected static final String MODULE_TOOLTIP = "Highlights all the players in ESP.";
    protected static final String MODULE_ID = "murdermystery.playeresp";
    private final Glow.Context glowContext = new Glow.Context();
    private final HashMap<UUID, Role> uuidStates = new HashMap<>();

    public MysteryEsp() {
        super(MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!this.isEnabled() || GameDetector.rootGame != GameDetector.ParentGame.MURDER_MYSTERY || GameDetector.subGame == GameDetector.ChildGame.NULL) {
                this.glowContext.removeAll();
                this.uuidStates.clear();
                return;
            }
            if (client.world == null) return;
            for (PlayerEntity playerEntity : client.world.getPlayers()) {
                if (playerEntity.getEyeY() - playerEntity.getY() <= 0.3) {
                    this.removeState(playerEntity);
                    continue;
                }
                ItemStack item = PlayerEntityL.getHeldItem(playerEntity);
                if (item != null) {
                    if (item.isOf(Items.FILLED_MAP)) continue;
                    if (item.isOf(Items.GOLD_INGOT)) continue;
                    if (item.isOf(Items.ARMOR_STAND)) continue;
                    if (item.isOf(Items.COMPASS)) continue;
                    if (item.isOf(Items.COMPARATOR)) continue;
                    if (item.isOf(Items.RED_BED)) continue;
                    if (item.isOf(Items.ARROW) || item.isOf(Items.BOW)) {
                        this.setDetective(playerEntity);
                        continue;
                    }
                    String itemName = TextL.toColorCodedString(item.getFormattedName());
                    if (itemName.startsWith("§r§a")) {
                        this.setMurderer(playerEntity);
                        continue;
                    }
                }
                this.setInnocent(playerEntity);
            }
        });
    }

    private void setInnocent(Entity entity) {
        UUID uuid = entity.getUuid();
        if (this.uuidStates.containsKey(uuid)) {
            return;
        }
        this.uuidStates.put(uuid, Role.NULL);
        this.glowContext.addGlow(uuid, 0xFFFFFF);
    }

    private void setMurderer(Entity entity) {
        UUID uuid = entity.getUuid();
        this.uuidStates.put(uuid, Role.MURDERER);
        this.glowContext.addGlow(uuid, 0xFF0000);
    }

    private void setDetective(Entity entity) {
        UUID uuid = entity.getUuid();
        if (this.uuidStates.containsKey(uuid)) {
            Role state = this.uuidStates.get(uuid);
            if (state == Role.MURDERER) return;
        }
        this.uuidStates.put(uuid, Role.DETECTIVE);
        this.glowContext.addGlow(uuid, 0x00FF00);
    }

    private void removeState(Entity entity) {
        UUID uuid = entity.getUuid();
        this.uuidStates.remove(uuid);
        this.glowContext.removeGlow(uuid);
    }

    private enum Role {
        NULL(0), MURDERER(1), DETECTIVE(2);
        private final int id;

        Role(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }
}
