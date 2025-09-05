package dev.cigarette.gui.hud.bar.providers;

import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.gui.hud.bar.BarDisplay;
import dev.cigarette.gui.hud.bar.api.BarWidget;
import dev.cigarette.gui.hud.bar.api.BarWidgetProvider;
import dev.cigarette.gui.hud.bar.widgets.EntityChipWidget;
import dev.cigarette.module.combat.PlayerAimbot;
import dev.cigarette.module.zombies.Aimbot;
import dev.cigarette.agent.ZombiesAgent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;

import java.util.List;
import java.util.UUID;

public class AimbotTargetProvider implements BarWidgetProvider {
    @Override
    public void collect(MinecraftClient mc, ClientWorld world, TextRenderer tr, List<BarWidget> out) {
        if (mc == null || world == null || mc.player == null) return;

        boolean playerAimbotRunning = false;
        boolean zombiesAimbotRunning = false;
        try { playerAimbotRunning = PlayerAimbot.INSTANCE.isRunning(); } catch (Throwable ignored) {}
        try { zombiesAimbotRunning = Aimbot.INSTANCE.isRunning(); } catch (Throwable ignored) {}

        if (playerAimbotRunning) {
            AbstractClientPlayerEntity target = PlayerAimbot.getBestTargetFor(mc.player);
            if (target != null) {
                String label = target.getName().getString();
                double sortKey = 0d;
                try {
                    double dx = target.getX() - mc.player.getX();
                    double dz = target.getZ() - mc.player.getZ();
                    sortKey = BarDisplay.angle(mc.player.getYaw(), dx, dz);
                    label += " (" + Math.round(mc.player.distanceTo(target)) + "m)";
                } catch (Throwable ignored) {}
                float hp = Math.max(0f, Math.min(1f, target.getHealth() / target.getMaxHealth()));
                out.add(new EntityChipWidget.Progress("aimbot:player:" + target.getUuid(), target, label, sortKey, 0,
                        hp, CigaretteScreen.SECONDARY_COLOR, CigaretteScreen.BACKGROUND_COLOR));
            }
        }

        if (zombiesAimbotRunning) {
            ZombiesAgent.ZombieTarget zt = ZombiesAgent.getClosestZombie();
            if (zt != null && zt.uuid != null) {
                Entity ent = findEntityByUuid(world, zt.uuid);
                String label = zt.type.getName();
                double sortKey = 0d;
                if (ent == null) {
                    label = label + " [?]";
                    sortKey = 180.0;
                } else {
                    try {
                        double dx = ent.getX() - mc.player.getX();
                        double dz = ent.getZ() - mc.player.getZ();
                        sortKey = BarDisplay.angle(mc.player.getYaw(), dx, dz);
                        label += " (" + Math.round(mc.player.distanceTo(ent)) + "m)";
                    } catch (Throwable ignored) {}
                }
                out.add(new EntityChipWidget("aimbot:zombies:" + zt.uuid, ent, label, sortKey, zt.type.getColor()));
            }
        }
    }

    private static Entity findEntityByUuid(ClientWorld world, UUID uuid) {
        for (Entity e : world.getEntities()) if (uuid.equals(e.getUuid())) return e;
        return null;
    }
}
