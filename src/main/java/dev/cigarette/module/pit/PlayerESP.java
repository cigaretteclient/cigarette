package dev.cigarette.module.pit;

import dev.cigarette.GameDetector;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.helper.GlowHelper;
import dev.cigarette.helper.WorldHelper;
import dev.cigarette.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerESP extends TickModule<ToggleWidget, Boolean> {
    public static final PlayerESP INSTANCE = new PlayerESP("pit.playeresp", "PlayerESP", "Highlights all the players in the world by prestige color.");

    private final GlowHelper.Context glowContext = new GlowHelper.Context();

    private PlayerESP(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        this.glowContext.removeAll();
        for (PlayerEntity playerEntity : WorldHelper.getRealPlayers()) {
            UUID uuid = playerEntity.getUuid();
            Text displayName = playerEntity.getStyledDisplayName();
            if (displayName == null) continue;
            TextColor color = null;
            for (Text sibling : displayName.getWithStyle(displayName.getStyle())) {
                if (sibling.getString().equals("[")) {
                    color = sibling.getStyle().getColor();
                    break;
                }
            }
            if (color == null) {
                this.glowContext.addGlow(uuid, 0);
            } else {
                this.glowContext.addGlow(uuid, color.getRgb());
            }
        }
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        this.glowContext.removeAll();
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.PIT;
    }
}
