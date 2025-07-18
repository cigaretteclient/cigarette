package io.github.waqfs.module.murdermystery;

import io.github.waqfs.GameDetector;
import io.github.waqfs.agent.MurderMysteryAgent;
import io.github.waqfs.lib.Glow;
import io.github.waqfs.module.BaseModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.HashSet;

public class GoldEsp extends BaseModule {
    protected static final String MODULE_NAME = "GoldESP";
    protected static final String MODULE_TOOLTIP = "Highlights all the gold ingots on the ground.";
    protected static final String MODULE_ID = "murdermystery.goldesp";
    private final Glow.Context glowContext = new Glow.Context();

    public GoldEsp() {
        super(MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        this.glowContext.removeAll();
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!this.isEnabled() || GameDetector.rootGame != GameDetector.ParentGame.MURDER_MYSTERY || GameDetector.subGame == GameDetector.ChildGame.NULL)
                return;
            if (client.world == null) return;

            HashSet<MurderMysteryAgent.AvailableGold> availableGold = MurderMysteryAgent.getVisibleGold();
            for (MurderMysteryAgent.AvailableGold gold : availableGold) {
                this.glowContext.addGlow(gold.uuid, 0xFFD800);
            }
        });
    }
}
