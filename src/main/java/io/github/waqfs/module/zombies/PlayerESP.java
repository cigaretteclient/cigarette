package io.github.waqfs.module.zombies;

import io.github.waqfs.GameDetector;
import io.github.waqfs.lib.Glow;
import io.github.waqfs.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerESP extends TickModule {
    protected static final String MODULE_NAME = "PlayerESP";
    protected static final String MODULE_TOOLTIP = "Highlights all the players in the game.";
    protected static final String MODULE_ID = "zombies.playeresp";
    private final Glow.Context glowContext = new Glow.Context();

    public PlayerESP() {
        super(MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        this.glowContext.removeAll();
        for (PlayerEntity playerEntity : world.getPlayers()) {
            UUID uuid = playerEntity.getUuid();
            this.glowContext.addGlow(uuid, 0xFCF805);
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
