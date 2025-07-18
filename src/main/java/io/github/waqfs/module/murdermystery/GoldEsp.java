package io.github.waqfs.module.murdermystery;

import io.github.waqfs.GameDetector;
import io.github.waqfs.lib.Glow;
import io.github.waqfs.module.BaseModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Items;

import java.util.UUID;

public class GoldEsp extends BaseModule {
    protected static final String MODULE_NAME = "GoldESP";
    protected static final String MODULE_TOOLTIP = "Highlights all the gold ingots on the ground.";
    protected static final String MODULE_ID = "murdermystery.goldesp";
    private final Glow.Context glowContext = new Glow.Context();

    public GoldEsp() {
        super(MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!this.isEnabled() || GameDetector.rootGame != GameDetector.ParentGame.MURDER_MYSTERY || GameDetector.subGame == GameDetector.ChildGame.NULL) {
                this.glowContext.removeAll();
                return;
            }
            if (client.world == null) return;
            for (Entity entity : client.world.getEntities()) {
                if (!(entity instanceof ItemEntity item)) continue;
                if (item.getStack().isOf(Items.GOLD_INGOT)) {
                    UUID uuid = item.getUuid();
                    this.glowContext.addGlow(uuid, 0xFFD800);
                }
            }
        });
    }
}
