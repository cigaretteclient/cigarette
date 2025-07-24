package io.github.waqfs.module.bedwars;

import io.github.waqfs.GameDetector;
import io.github.waqfs.lib.Glow;
import io.github.waqfs.module.BaseModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;

public class PlayerESP extends BaseModule {
    protected static final String MODULE_NAME = "PlayerESP";
    protected static final String MODULE_TOOLTIP = "Highlights all the players in the game.";
    protected static final String MODULE_ID = "bedwars.playeresp";
    private final Glow.Context glowContext = new Glow.Context();

    public PlayerESP() {
        super(MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            this.glowContext.removeAll();

            if (!this.isEnabled() || GameDetector.rootGame != GameDetector.ParentGame.BEDWARS || GameDetector.subGame != GameDetector.ChildGame.INSTANCED_BEDWARS)
                return;
            if (client.world == null) return;

            for (PlayerEntity playerEntity : client.world.getPlayers()) {
                UUID uuid = playerEntity.getUuid();
                int teamColor = playerEntity.getTeamColorValue();
                this.glowContext.addGlow(uuid, teamColor);
            }
        });
    }
}
