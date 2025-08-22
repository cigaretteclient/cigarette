package dev.cigarette.module.render;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.widget.BaseWidget;
import dev.cigarette.gui.widget.ColorDropdownWidget;
import dev.cigarette.gui.widget.TextWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.lib.Glow;
import dev.cigarette.lib.WorldL;
import dev.cigarette.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerESP extends TickModule<ToggleWidget, Boolean> {
    public static final PlayerESP INSTANCE = Cigarette.CONFIG.constructModule(new PlayerESP("render.playeresp", "PlayerESP", "Highlights all the players in the world."), "Render");

    private final ToggleWidget byTeamColor = new ToggleWidget(Text.literal("By Team Color"), Text.literal("ESP players in the color of their team.")).withDefaultState(true);
    private final TextWidget nonTeamColorText = new TextWidget(Text.literal("Static Color"), Text.literal("The color of all players if coloring by team is disabled.")).centered(false);
    private final ColorDropdownWidget<TextWidget, BaseWidget.Stateless> nonTeamColor = new ColorDropdownWidget<TextWidget, BaseWidget.Stateless>(Text.empty(), null).withAlpha(false).withDefaultColor(0xFFFFFFFF);

    private final Glow.Context glowContext = new Glow.Context();

    public PlayerESP(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        nonTeamColor.setHeader(nonTeamColorText);
        this.setChildren(byTeamColor, nonTeamColor);
        byTeamColor.registerConfigKey(id + ".byteam");
        nonTeamColor.registerConfigKey(id + ".static");
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

    @Override
    public boolean inValidGame() {
        return !Cigarette.CONFIG.MYSTERY_PLAYERESP.isRunning();
    }
}
