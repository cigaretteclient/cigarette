package io.github.waqfs.module.zombies;

import io.github.waqfs.GameDetector;
import io.github.waqfs.gui.widget.ColorPickerWidget;
import io.github.waqfs.gui.widget.ToggleOptionsWidget;
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

public class PlayerESP extends TickModule<ToggleOptionsWidget> {
    protected static final String MODULE_NAME = "PlayerESP";
    protected static final String MODULE_TOOLTIP = "Highlights all the players in the game.";
    protected static final String MODULE_ID = "zombies.playeresp";
    private final Glow.Context glowContext = new Glow.Context();
    private final ColorPickerWidget color = new ColorPickerWidget(Text.literal("Color"), false).withDefaultColor(0xFFFCF805);

    public PlayerESP() {
        super(ToggleOptionsWidget.base, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        this.widget.setOptions(color);
        color.registerAsOption("zombies.playeresp.color");
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        this.glowContext.removeAll();
        for (PlayerEntity playerEntity : WorldL.getRealPlayers()) {
            UUID uuid = playerEntity.getUuid();
            this.glowContext.addGlow(uuid, color.getStateRGB());
        }
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        this.glowContext.removeAll();
    }

    @Override
    protected boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }

}
