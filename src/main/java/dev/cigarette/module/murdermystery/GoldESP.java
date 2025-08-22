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
    public static final GoldESP INSTANCE = new GoldESP("murdermystery.goldesp", "GoldESP", "Highlights all the gold ingots on the ground.");

    private final Glow.Context glowContext = new Glow.Context();

    public GoldESP(String id, String name, String tooltip) {
        super(ColorDropdownWidget::module, id, name, tooltip);
        assert this.wrapper != null;
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
