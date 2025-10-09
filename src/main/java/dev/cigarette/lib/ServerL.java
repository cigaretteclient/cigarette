package dev.cigarette.lib;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.util.Objects;

public class ServerL {
    /*
     * Returns true if the two players are on the same team.
     * Uses common Minecraft server logic.
     * All individual checks are also exposed in ServerL.
     */
    public static boolean playerOnSameTeam(PlayerEntity player, PlayerEntity other) {
        if (player == null || other == null) return false;
        if (player.getScoreboardTeam() != null && other.getScoreboardTeam() != null && player.getScoreboardTeam().isEqual(other.getScoreboardTeam()))
            return true;
        return getScoreboardColor(player) != -1 && getScoreboardColor(player) == getScoreboardColor(other) ||
                getArmorColor(player) != -1 && getArmorColor(player) == getArmorColor(other) ||
                getNameColor(player) != -1 && getNameColor(player) == getNameColor(other);
    }

    public static boolean playerOnSameTeam(ServerPlayerEntity player, ServerPlayerEntity other) {
        return playerOnSameTeam((PlayerEntity) player, (PlayerEntity) other);
    }

    public static int getNameColor(PlayerEntity player) {
        try {
            if (player == null || player.getDisplayName() == null) return -1;
            var style = player.getDisplayName().getStyle();
            if (style.getColor() == null) return -1;
            return style.getColor().getRgb();
        } catch (Exception ignored) {
            return -1;
        }
    }

    public static int getScoreboardColor(PlayerEntity player) {
        if (player == null || player.getScoreboardTeam() == null) return -1;
        try {
            Formatting color = player.getScoreboardTeam().getColor();
            if (color == null || color.getColorValue() == null) return -1;
            return color.getColorValue();
        } catch (Exception ignored) {
            return -1;
        }
    }

    public static int getArmorColor(PlayerEntity player) {
        if (player == null || player.getInventory() == null) return -1;

        ItemStack boots = player.getInventory().getStack(36);
        ItemStack leggings = player.getInventory().getStack(37);

        // ItemStack chestplate = player.getInventory().getStack(38);
        // ItemStack helmet = player.getInventory().getStack(39);

        DyedColorComponent b = getDyedColorComponent(boots);
        DyedColorComponent l = getDyedColorComponent(leggings);

        if (b != null && l != null && b.rgb() == l.rgb()) {
            return b.rgb();
        }
        return -1;
    }

    private static DyedColorComponent getDyedColorComponent(ItemStack stack) {
        if (stack == null) return null;
        ComponentType<DyedColorComponent> type = DataComponentTypes.DYED_COLOR;
        return stack.getItem().getComponents().get(type);
    }

    public static ServerPlayerEntity getPlayer(ClientPlayerEntity player) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getServer() == null || player == null) return null; // mc.getServer() null on multiplayer clients
        return mc.getServer().getPlayerManager().getPlayer(player.getUuid());
    }
}