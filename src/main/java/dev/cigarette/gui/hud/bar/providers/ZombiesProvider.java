package dev.cigarette.gui.hud.bar.providers;

import dev.cigarette.agent.ZombiesAgent;
import dev.cigarette.gui.hud.bar.BarDisplay;
import dev.cigarette.gui.hud.bar.api.BarWidget;
import dev.cigarette.gui.hud.bar.api.BarWidgetProvider;
import dev.cigarette.gui.hud.bar.widgets.EntityChipWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;

import java.util.List;
import java.util.UUID;

public class ZombiesProvider implements BarWidgetProvider {
    @Override
    public void collect(MinecraftClient mc, ClientWorld world, TextRenderer tr, List<BarWidget> out) {
        if (mc == null || world == null) return;
        for (ZombiesAgent.ZombieTarget zt : ZombiesAgent.getZombies()) {
            if (zt == null || zt.uuid == null) continue;
            UUID uuid = zt.uuid;
            String label = ZombiesAgent.PrettyMobs.labelOf(zt.type);
            double sortKey = 0d;
            try {
                if (zt.getEndVec() != null && mc.player != null) {
                    double dx = zt.getEndVec().x - mc.player.getX();
                    double dz = zt.getEndVec().z - mc.player.getZ();
                    sortKey = BarDisplay.angle(mc.player.getYaw(), dx, dz);
                }
            } catch (Throwable ignored) {}
            Entity ent = findEntityByUuid(world, uuid);
            int color = ZombiesAgent.PrettyMobs.colorOf(zt.type);
            if (ent == null) {
                label = label + " [?]";
                sortKey = 180.0;
            } else if (mc.player != null) {
                float dist = mc.player.distanceTo(ent);
                label = label + " (" + Math.round(dist) + "m)";
                sortKey += dist;
            }
            out.add(new EntityChipWidget("uuid:" + uuid, ent, label, sortKey, color));
        }
    }

    private static Entity findEntityByUuid(ClientWorld world, UUID uuid) {
        for (Entity e : world.getEntities()) if (uuid.equals(e.getUuid())) return e;
        return null;
    }
}

