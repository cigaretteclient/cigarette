package io.github.waqfs.module.murdermystery;

import io.github.waqfs.GameDetector;
import io.github.waqfs.agent.MurderMysteryAgent;
import io.github.waqfs.lib.Glow;
import io.github.waqfs.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

public class PlayerESP extends TickModule {
    protected static final String MODULE_NAME = "PlayerESP";
    protected static final String MODULE_TOOLTIP = "Highlights all the players in ESP.";
    protected static final String MODULE_ID = "murdermystery.playeresp";
    private final Glow.Context glowContext = new Glow.Context();

    public PlayerESP() {
        super(MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        this.glowContext.removeAll();
        HashSet<MurderMysteryAgent.PersistentPlayer> persistentPlayers = MurderMysteryAgent.getVisiblePlayers();
        for (MurderMysteryAgent.PersistentPlayer persistentPlayer : persistentPlayers) {
            switch (persistentPlayer.role) {
                case INNOCENT -> this.glowContext.addGlow(persistentPlayer.playerEntity.getUuid(), 0xFFFFFF);
                case DETECTIVE -> this.glowContext.addGlow(persistentPlayer.playerEntity.getUuid(), 0x00FF00);
                case MURDERER -> this.glowContext.addGlow(persistentPlayer.playerEntity.getUuid(), 0xFF0000);
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
