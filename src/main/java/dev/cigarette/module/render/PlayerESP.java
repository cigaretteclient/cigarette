package dev.cigarette.module.render;

import dev.cigarette.gui.widget.BaseWidget;
import dev.cigarette.gui.widget.ColorDropdownWidget;
import dev.cigarette.gui.widget.TextWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.helper.GlowHelper;
import dev.cigarette.helper.WorldHelper;
import dev.cigarette.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerESP extends TickModule<ToggleWidget, Boolean> {
    public static final PlayerESP INSTANCE = new PlayerESP("render.playeresp", "PlayerESP", "Highlights all the players in the world.");

    private final ToggleWidget byTeamColor = new ToggleWidget("By Team Color", "ESP players in the color of their team.").withDefaultState(true);
    private final TextWidget nonTeamColorText = new TextWidget("Static Color", "The color of all players if coloring by team is disabled.").centered(false);
    private final ColorDropdownWidget<TextWidget, BaseWidget.Stateless> nonTeamColor = new ColorDropdownWidget<TextWidget, BaseWidget.Stateless>("", null).withAlpha(false).withDefaultColor(0xFFFFFFFF);

    private final GlowHelper.Context glowContext = new GlowHelper.Context();

    private PlayerESP(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        nonTeamColor.setHeader(nonTeamColorText);
        this.setChildren(byTeamColor, nonTeamColor);
        byTeamColor.registerConfigKey(id + ".byteam");
        nonTeamColor.registerConfigKey(id + ".static");
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        this.glowContext.removeAll();
        for (PlayerEntity playerEntity : WorldHelper.getRealPlayers()) {
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

    @Override
    public boolean inValidGame() {
        return !(dev.cigarette.module.murdermystery.PlayerESP.INSTANCE.isRunning() || dev.cigarette.module.pit.PlayerESP.INSTANCE.isRunning());
    }
}
