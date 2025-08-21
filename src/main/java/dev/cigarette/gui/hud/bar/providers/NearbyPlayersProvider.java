package dev.cigarette.gui.hud.bar.providers;

import dev.cigarette.gui.hud.bar.api.BarWidget;
import dev.cigarette.gui.hud.bar.api.BarWidgetProvider;
import dev.cigarette.gui.hud.bar.widgets.EntityChipWidget;
import dev.cigarette.lib.WorldL;
import dev.cigarette.module.murdermystery.PlayerESP;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;

import java.util.List;
import java.util.UUID;

public class NearbyPlayersProvider implements BarWidgetProvider {
    @Override
    public void collect(MinecraftClient mc, ClientWorld world, TextRenderer tr, List<BarWidget> out) {
        if (mc == null || world == null || mc.player == null) return;
        for (AbstractClientPlayerEntity p : WorldL.getRealPlayers()) {
            if (p == null || p == mc.player) continue;
            if (!WorldL.isRealPlayer(p)) continue;
            if (p.isRemoved()) continue;
            if (mc.player.distanceTo(p) > 15.0f) continue;
            UUID uuid = p.getUuid();
            if (uuid == null) continue;
            String label = p.getName().getString();
            double sortKey;
            try { sortKey = PlayerESP.calculateRelativeYaw(p); } catch (Throwable t) { sortKey = 0; }
            out.add(new EntityChipWidget("uuid:" + uuid, p, label, sortKey, 0));
        }
    }
}

