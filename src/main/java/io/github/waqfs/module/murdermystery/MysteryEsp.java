package io.github.waqfs.module.murdermystery;

import io.github.waqfs.GameDetector;
import io.github.waqfs.agent.MurderMysteryAgent;
import io.github.waqfs.lib.Glow;
import io.github.waqfs.module.BaseModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.HashSet;

public class MysteryEsp extends BaseModule {
    protected static final String MODULE_NAME = "PlayerESP";
    protected static final String MODULE_TOOLTIP = "Highlights all the players in ESP.";
    protected static final String MODULE_ID = "murdermystery.playeresp";
    private final Glow.Context glowContext = new Glow.Context();

    public MysteryEsp() {
        super(MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            this.glowContext.removeAll();
            if (!this.isEnabled() || GameDetector.rootGame != GameDetector.ParentGame.MURDER_MYSTERY || GameDetector.subGame == GameDetector.ChildGame.NULL)
                return;
            if (client.world == null) return;

            HashSet<MurderMysteryAgent.PersistentPlayer> persistentPlayers = MurderMysteryAgent.getVisiblePlayers();
            for (MurderMysteryAgent.PersistentPlayer player : persistentPlayers) {
                switch (player.role) {
                    case INNOCENT -> this.glowContext.addGlow(player.playerEntity.getUuid(), 0xFFFFFF);
                    case DETECTIVE -> this.glowContext.addGlow(player.playerEntity.getUuid(), 0x00FF00);
                    case MURDERER -> this.glowContext.addGlow(player.playerEntity.getUuid(), 0xFF0000);
                }
            }
        });
    }
}
