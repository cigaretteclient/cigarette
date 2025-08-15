package io.github.waqfs.module.render;

import io.github.waqfs.gui.widget.BaseWidget;
import io.github.waqfs.gui.widget.ColorDropdownWidget;
import io.github.waqfs.gui.widget.TextWidget;
import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.lib.Glow;
import io.github.waqfs.lib.WorldL;
import io.github.waqfs.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerESP extends TickModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "PlayerESP";
    protected static final String MODULE_TOOLTIP = "Highlights all the players in the world.";
    protected static final String MODULE_ID = "render.playeresp";
    private final ToggleWidget byTeamColor = new ToggleWidget(Text.literal("By Team Color"), Text.literal("ESP players in the color of their team.")).withDefaultState(true);
    private final TextWidget nonTeamColorText = new TextWidget(Text.literal("Static Color"), Text.literal("The color of all players if coloring by team is disabled.")).centered(false);
    private final ColorDropdownWidget<TextWidget, BaseWidget.Stateless> nonTeamColor = new ColorDropdownWidget<TextWidget, BaseWidget.Stateless>(Text.empty(), null).withAlpha(false).withDefaultColor(0xFFFFFFFF);
    private final Glow.Context glowContext = new Glow.Context();

    public PlayerESP() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        nonTeamColor.setHeader(nonTeamColorText);
        this.setChildren(byTeamColor, nonTeamColor);
        byTeamColor.registerConfigKey("render.playeresp.byteam");
        nonTeamColor.registerConfigKey("render.playeresp.static");
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        this.glowContext.removeAll();
        for (PlayerEntity playerEntity : WorldL.getRealPlayers()) {
            UUID uuid = playerEntity.getUuid();
            if (byTeamColor.getRawState()) {
                int teamColor = playerEntity.getTeamColorValue();
                this.glowContext.addGlow(uuid, teamColor);
            } else {
                this.glowContext.addGlow(uuid, nonTeamColor.getStateRGB());
            }
        }
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        this.glowContext.removeAll();
    }
}
