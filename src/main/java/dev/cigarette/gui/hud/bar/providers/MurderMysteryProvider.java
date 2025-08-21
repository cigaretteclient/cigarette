package dev.cigarette.gui.hud.bar.providers;

import dev.cigarette.agent.MurderMysteryAgent;
import dev.cigarette.gui.hud.bar.api.BarWidget;
import dev.cigarette.gui.hud.bar.api.BarWidgetProvider;
import dev.cigarette.gui.hud.bar.widgets.EntityChipWidget;
import dev.cigarette.module.murdermystery.PlayerESP;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.world.ClientWorld;

import java.util.List;
import java.util.UUID;

public class MurderMysteryProvider implements BarWidgetProvider {
    @Override
    public void collect(MinecraftClient mc, ClientWorld world, TextRenderer tr, List<BarWidget> out) {
        if (world == null || mc == null || mc.player == null) return;
        for (MurderMysteryAgent.PersistentPlayer pp : MurderMysteryAgent.getVisiblePlayers()) {
            if (pp == null || pp.playerEntity == null) continue;
            if (pp.role != MurderMysteryAgent.PersistentPlayer.Role.MURDERER) continue;
            UUID uuid = pp.playerEntity.getUuid();
            if (uuid == null) continue;
            String baseName = pp.playerEntity.getName().getString();
            float dist = mc.player.distanceTo(pp.playerEntity);
            String label = baseName + " (" + Math.round(dist) + "m)";
            double sortKey;
            try { sortKey = PlayerESP.calculateRelativeYaw(pp.playerEntity); } catch (Throwable t) { sortKey = 0; }
            out.add(new EntityChipWidget("uuid:" + uuid, pp.playerEntity, label, sortKey, 0));
        }
    }
}

