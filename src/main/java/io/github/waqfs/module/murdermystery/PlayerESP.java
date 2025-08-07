package io.github.waqfs.module.murdermystery;

import io.github.waqfs.GameDetector;
import io.github.waqfs.agent.MurderMysteryAgent;
import io.github.waqfs.gui.widget.TextWidget;
import io.github.waqfs.gui.widget.ToggleColorWidget;
import io.github.waqfs.lib.Glow;
import io.github.waqfs.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

public class PlayerESP extends TickModule {
    protected static final String MODULE_NAME = "PlayerESP";
    protected static final String MODULE_TOOLTIP = "Highlights all the players in ESP.";
    protected static final String MODULE_ID = "murdermystery.playeresp";
    private final Glow.Context glowContext = new Glow.Context();
    private final ToggleColorWidget innocent = new ToggleColorWidget(Text.literal("Innocents"), Text.literal("The glow color of innocent and unknown players."), false).withDefaultColor(0xFFFFFFFF).withDefaultState(true);
    private final ToggleColorWidget detective = new ToggleColorWidget(Text.literal("Detective"), Text.literal("The glow color of the detective and bow holding players that aren't murderer."), false).withDefaultColor(0xFF00FF00).withDefaultState(true);
    private final ToggleColorWidget murderer = new ToggleColorWidget(Text.literal("Murderer"), Text.literal("The glow color of the murderer."), false).withDefaultColor(0xFFFF0000).withDefaultState(true);

    public PlayerESP() {
        super(MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        TextWidget header = new TextWidget(Text.literal("Types")).withUnderline();
        this.widget.setOptions(header, innocent, detective, murderer);
        innocent.registerAsOption("murdermystery.playeresp.innocent");
        detective.registerAsOption("murdermystery.playeresp.detective");
        murderer.registerAsOption("murdermystery.playeresp.murderer");
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

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        this.glowContext.removeAll();
    }

    @Override
    protected boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.MURDER_MYSTERY && GameDetector.subGame != null;
    }
}
