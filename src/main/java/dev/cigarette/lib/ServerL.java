package dev.cigarette.lib;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.ArmorDyeRecipe;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.util.Objects;

public class ServerL {
    public static boolean playerOnSameTeam(ServerPlayerEntity player, ServerPlayerEntity other) {
        if (player.getScoreboardTeam() == null || other.getScoreboardTeam() == null) return false;
        return player.getScoreboardTeam().isEqual(other.getScoreboardTeam()) ||
                getScoreboardColor(player) == getScoreboardColor(other) ||
                getArmorColor(player) == getArmorColor(other) ||
                getNameColor(player) == getNameColor(other);
    }

    public static int getNameColor(ServerPlayerEntity player) {
        return Objects.requireNonNull(
                Objects.requireNonNull(player.getDisplayName()).getStyle().getColor()
        ).getRgb();
    }

    public static int getScoreboardColor(ServerPlayerEntity player) {
        if (player.getScoreboardTeam() == null) return -1;
        try {
            Formatting color = player.getScoreboardTeam().getColor();
            if (color == null) return -1;
            assert color.getColorValue() != null;
            return color.getColorValue();
        } catch (NullPointerException e) {
            return -1;
        }
    }

    public static int getArmorColor(ServerPlayerEntity player) {
        if (player.getInventory() == null) return -1;

        ItemStack boots = player.getInventory().getStack(36);
        ItemStack leggings = player.getInventory().getStack(37);
        ItemStack chestplate = player.getInventory().getStack(38);
        ItemStack helmet = player.getInventory().getStack(39);

        DyedColorComponent b = getDyedColorComponent(boots);
        DyedColorComponent l = getDyedColorComponent(leggings);
//        DyedColorComponent c = getDyedColorComponent(chestplate);
//        DyedColorComponent h = getDyedColorComponent(helmet);

        if (b != null && l != null) {
            if (b.rgb() == l.rgb()) {
                return b.rgb();
            }
        }

        return -1;
    }

    private static DyedColorComponent getDyedColorComponent(ItemStack stack) {
        ComponentType<DyedColorComponent> type = DataComponentTypes.DYED_COLOR;
        return stack.getItem().getComponents().get(type);
    }

    public static ServerPlayerEntity getPlayer(ClientPlayerEntity player) {
        assert MinecraftClient.getInstance().getServer() != null;
        return MinecraftClient.getInstance().getServer().getPlayerManager().getPlayer(player.getUuid());
    }
}