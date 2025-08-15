package io.github.waqfs.module.combat;

import io.github.waqfs.agent.BedwarsAgent;
import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;


public class JumpReset extends TickModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "JumpReset";
    protected static final String MODULE_TOOLTIP = "Jumps upon taking damage to reduce knockback.";
    protected static final String MODULE_ID = "combat.jumpreset";

    public JumpReset() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        BedwarsAgent.jumpResetEnabled = true;
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        BedwarsAgent.jumpResetEnabled = false;
    }
}
