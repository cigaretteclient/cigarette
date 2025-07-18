package io.github.waqfs.module.murdermystery;

import io.github.waqfs.GameDetector;
import io.github.waqfs.lib.Glow;
import io.github.waqfs.module.BaseModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

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
            if (GameDetector.subGame != GameDetector.ChildGame.CLASSIC_MYSTERY) {
                this.glowContext.removeAll();
                this.uuidStates.clear();
                return;
            }
            if (client.world == null) return;
            for (PlayerEntity playerEntity : client.world.getPlayers()) {
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
