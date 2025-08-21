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
            String label = pretty(zt.type);
            double sortKey = 0d;
            try {
                if (zt.getEndVec() != null && mc.player != null) {
                    double dx = zt.getEndVec().x - mc.player.getX();
                    double dz = zt.getEndVec().z - mc.player.getZ();
                    sortKey = BarDisplay.angle(mc.player.getYaw(), dx, dz);
                }
            } catch (Throwable ignored) {}
            Entity ent = findEntityByUuid(world, uuid);
            int color = colorOf(zt.type);
            out.add(new EntityChipWidget("uuid:" + uuid, ent, label, sortKey, color));
        }
    }

    private static Entity findEntityByUuid(ClientWorld world, UUID uuid) {
        for (Entity e : world.getEntities()) if (uuid.equals(e.getUuid())) return e;
        return null;
    }

    private static String pretty(ZombiesAgent.ZombieType type) {
        if (type == null) return "Zombie";
        return switch (type) {
            case ZOMBIE -> "Zombie";
            case BLAZE -> "Blaze";
            case WOLF -> "Wolf";
            case SKELETON -> "Skeleton";
            case CREEPER -> "Creeper";
            case MAGMACUBE -> "Magma Cube";
            case SLIME -> "Slime";
            case WITCH -> "Witch";
            case ENDERMITE -> "Endermite";
            case SILVERFISH -> "Silverfish";
            case UNKNOWN -> "Unknown";
        };
    }

    private static int colorOf(ZombiesAgent.ZombieType type) {
        if (type == null) return 0xFFFFFFFF;
        return switch (type) {
            case ZOMBIE -> 0xFF2C936C;
            case BLAZE -> 0xFFFCA50F;
            case WOLF -> 0xFF3FE6FC;
            case SKELETON -> 0xFFE0E0E0;
            case CREEPER, SLIME -> 0xFF155B0D;
            case MAGMACUBE -> 0xFFFC4619;
            case WITCH, ENDERMITE -> 0xFFA625F7;
            case SILVERFISH -> 0xFF3F3F3F;
            case UNKNOWN -> 0xFFFFFFFF;
        };
    }
}

