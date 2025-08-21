package dev.cigarette.module.murdermystery;

import dev.cigarette.GameDetector;
import dev.cigarette.agent.MurderMysteryAgent;
import dev.cigarette.gui.widget.TextWidget;
import dev.cigarette.gui.widget.ColorDropdownWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.lib.Glow;
import dev.cigarette.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

import static dev.cigarette.gui.hud.bar.BarDisplay.angle;

public class PlayerESP extends TickModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "PlayerESP";
    protected static final String MODULE_TOOLTIP = "Highlights all the players in ESP.";
    protected static final String MODULE_ID = "murdermystery.playeresp";
    private final Glow.Context glowContext = new Glow.Context();
    private final ColorDropdownWidget<ToggleWidget, Boolean> innocent = ColorDropdownWidget.buildToggle(Text.literal("Innocents"), Text.literal("The glow color of innocent and unknown players.")).withAlpha(false).withDefaultColor(0xFFFFFFFF).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> detective = ColorDropdownWidget.buildToggle(Text.literal("Detective"), Text.literal("The glow color of the detective and bow holding players that aren't murderer.")).withAlpha(false).withDefaultColor(0xFF00FF00).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> murderer = ColorDropdownWidget.buildToggle(Text.literal("Murderer"), Text.literal("The glow color of the murderer.")).withAlpha(false).withDefaultColor(0xFFFF0000).withDefaultState(true);

    public PlayerESP() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        TextWidget header = new TextWidget(Text.literal("Types")).withUnderline();
        this.setChildren(header, innocent, detective, murderer);
        innocent.registerConfigKey("murdermystery.playeresp.innocent");
        detective.registerConfigKey("murdermystery.playeresp.detective");
        murderer.registerConfigKey("murdermystery.playeresp.murderer");
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        this.glowContext.removeAll();
        HashSet<MurderMysteryAgent.PersistentPlayer> persistentPlayers = MurderMysteryAgent.getVisiblePlayers();
        for (MurderMysteryAgent.PersistentPlayer persistentPlayer : persistentPlayers) {
            switch (persistentPlayer.role) {
                case INNOCENT -> {
                    if (innocent.getToggleState()) {
                        this.glowContext.addGlow(persistentPlayer.playerEntity.getUuid(), innocent.getStateRGB());
                    }
                }
                case DETECTIVE -> {
                    if (detective.getToggleState()) {
                        this.glowContext.addGlow(persistentPlayer.playerEntity.getUuid(), detective.getStateRGB());
                    }
                }
                case MURDERER -> {
                    if (murderer.getToggleState()) {
                        this.glowContext.addGlow(persistentPlayer.playerEntity.getUuid(), murderer.getStateRGB());
                    }
                }
            }
        }
    }

    public static float calculateRelativeYaw(PlayerEntity target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        assert mc.player != null;

        double dx = target.getX() - mc.player.getX();
        double dz = target.getZ() - mc.player.getZ();

        return angle(mc, dx, dz);
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
