package io.github.waqfs.module.murdermystery;

import io.github.waqfs.GameDetector;
import io.github.waqfs.agent.MurderMysteryAgent;
import io.github.waqfs.gui.widget.ColorDropdownWidget;
import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.lib.Glow;
import io.github.waqfs.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

public class GoldESP extends TickModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "GoldESP";
    protected static final String MODULE_TOOLTIP = "Highlights all the gold ingots on the ground.";
    protected static final String MODULE_ID = "murdermystery.goldesp";
    private final Glow.Context glowContext = new Glow.Context();

    public GoldESP() {
        super(ColorDropdownWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        this.glowContext.removeAll();
        HashSet<MurderMysteryAgent.AvailableGold> availableGold = MurderMysteryAgent.getVisibleGold();
        for (MurderMysteryAgent.AvailableGold gold : availableGold) {
            this.glowContext.addGlow(gold.uuid, ((ColorDropdownWidget<ToggleWidget, Boolean>) this.wrapper).getStateRGB());
        }
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        this.glowContext.removeAll();
    }

    @Override
    protected boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.MURDER_MYSTERY && GameDetector.subGame != null;
    }
}
