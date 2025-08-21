package dev.cigarette.gui.hud.bar.providers;

import dev.cigarette.agent.MurderMysteryAgent;
import dev.cigarette.gui.hud.bar.api.BarWidget;
import dev.cigarette.gui.hud.bar.api.BarWidgetProvider;
import dev.cigarette.gui.hud.bar.widgets.EntityChipWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.world.ClientWorld;

import java.util.List;
import java.util.UUID;

public class MurderMysteryProvider implements BarWidgetProvider {
    private static final int COLOR_MURDERER = 0xFFDC3545;
    private static final int COLOR_DETECTIVE = 0xFF28A745;

    @Override
    public void collect(MinecraftClient mc, ClientWorld world, TextRenderer tr, List<BarWidget> out) {
        if (world == null || mc == null || mc.player == null) return;

        MurderMysteryAgent.PersistentPlayer nearestDetective = null;
        float nearestDetectiveDist = Float.MAX_VALUE;

        for (MurderMysteryAgent.PersistentPlayer pp : MurderMysteryAgent.getVisiblePlayers()) {
            if (pp == null || pp.playerEntity == null) continue;
            if (pp.role == MurderMysteryAgent.PersistentPlayer.Role.MURDERER) {
                UUID uuid = pp.playerEntity.getUuid();
                if (uuid == null) continue;
                float dist = mc.player.distanceTo(pp.playerEntity);
                String baseName = pp.playerEntity.getName().getString();
                String label = baseName + " [mur] (" + Math.round(dist) + "m)";
                double sortKey = -1_000_000.0 + dist;
                out.add(new EntityChipWidget("uuid:" + uuid, pp.playerEntity, label, sortKey, COLOR_MURDERER));
            } else if (pp.role == MurderMysteryAgent.PersistentPlayer.Role.DETECTIVE) {
                float dist = mc.player.distanceTo(pp.playerEntity);
                if (dist < nearestDetectiveDist) {
                    nearestDetectiveDist = dist;
                    nearestDetective = pp;
                }
            }
        }

        if (nearestDetective != null) {
            UUID uuid = nearestDetective.playerEntity.getUuid();
            if (uuid != null) {
                String baseName = nearestDetective.playerEntity.getName().getString();
                float dist = mc.player.distanceTo(nearestDetective.playerEntity);
                String label = baseName + " [det] (" + Math.round(dist) + "m)";
                double sortKey = -500_000.0 + dist;
                out.add(new EntityChipWidget("uuid:" + uuid, nearestDetective.playerEntity, label, sortKey, COLOR_DETECTIVE));
            }
        }
    }
}
