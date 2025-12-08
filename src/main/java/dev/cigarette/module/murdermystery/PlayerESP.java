package dev.cigarette.module.murdermystery;

import dev.cigarette.GameDetector;
import dev.cigarette.agent.MurderMysteryAgent;
import dev.cigarette.gui.hud.bar.providers.MurderMysteryProvider;
import dev.cigarette.gui.widget.ColorDropdownWidget;
import dev.cigarette.gui.widget.TextWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.helper.GlowHelper;
import dev.cigarette.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

public class PlayerESP extends TickModule<ToggleWidget, Boolean> {
    public static final PlayerESP INSTANCE = new PlayerESP("murdermystery.playeresp", "PlayerESP", "Highlights all the players in ESP.");

    private final GlowHelper.Context glowContext = new GlowHelper.Context();

    private final ColorDropdownWidget<ToggleWidget, Boolean> innocent = ColorDropdownWidget.buildToggle("Innocents", "The glow color of innocent and unknown players.").withAlpha(false).withDefaultColor(0xFFFFFFFF).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> detective = ColorDropdownWidget.buildToggle("Detective", "The glow color of the detective and bow holding players that aren't murderer.").withAlpha(false).withDefaultColor(0xFF00FF00).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> murderer = ColorDropdownWidget.buildToggle("Murderer", "The glow color of the murderer.").withAlpha(false).withDefaultColor(0xFFFF0000).withDefaultState(true);

    private PlayerESP(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        TextWidget header = new TextWidget("Types").withUnderline();
        this.setChildren(header, innocent, detective, murderer);
        innocent.registerConfigKey(id + ".innocent");
        detective.registerConfigKey(id + ".detective");
        murderer.registerConfigKey(id + ".murderer");
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        this.glowContext.removeAll();
        HashSet<MurderMysteryAgent.PersistentPlayer> persistentPlayers = MurderMysteryAgent.getVisiblePlayers();
        MurderMysteryProvider.COLOR_MURDERER = murderer.getToggleState() ? murderer.getStateARGB() : 0xFFFF0000;
        MurderMysteryProvider.COLOR_DETECTIVE = detective.getToggleState() ? detective.getStateARGB() : 0xFF00FF00;
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
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.MURDER_MYSTERY && GameDetector.subGame != null;
    }
}
