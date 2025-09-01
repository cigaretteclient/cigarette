package dev.cigarette.lib;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WorldL {
    public static List<AbstractClientPlayerEntity> getRealPlayers() {
        ClientPlayerEntity clientPlayer = MinecraftClient.getInstance().player;
        ClientWorld world = MinecraftClient.getInstance().world;
        assert clientPlayer != null;
        assert world != null;

        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler == null) return new ArrayList<>();

        List<AbstractClientPlayerEntity> realPlayers = new ArrayList<>();
        for (PlayerListEntry entry : networkHandler.getPlayerList()) {
            if (entry.getLatency() <= 0) continue;
            if (entry.getProfile() == clientPlayer.getGameProfile()) continue;
            for (AbstractClientPlayerEntity worldPlayer : world.getPlayers()) {
                if (entry.getProfile() == worldPlayer.getGameProfile()) {
                    realPlayers.add(worldPlayer);
                    break;
                }
            }
        }
        return realPlayers;
    }

    public static boolean isRealPlayer(PlayerEntity player) {
        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler == null) return false;

        for (PlayerListEntry entry : networkHandler.getPlayerList()) {
            if (entry.getProfile() != player.getGameProfile()) continue;
            if (entry.getLatency() <= 0) continue;
            return true;
        }
        return false;
    }

    public static boolean isRealPlayerByUsername(PlayerEntity player) {
        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler == null) return false;

        for (PlayerListEntry entry : networkHandler.getPlayerList()) {
            if (!Objects.equals(entry.getProfile().getName(), player.getNameForScoreboard())) continue;
            if (entry.getLatency() <= 0) continue;
            return true;
        }
        return false;
    }
}
