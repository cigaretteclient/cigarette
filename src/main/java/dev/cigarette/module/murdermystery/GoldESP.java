package dev.cigarette.module.murdermystery;

import dev.cigarette.GameDetector;
import dev.cigarette.agent.MurderMysteryAgent;
import dev.cigarette.gui.widget.ColorDropdownWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.lib.Glow;
import dev.cigarette.module.TickModule;
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
        ((ColorDropdownWidget<?, ?>) this.wrapper).withAlpha(false).withDefaultColor(0xFFFFD800);
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
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.MURDER_MYSTERY && GameDetector.subGame != null;
    }
}
