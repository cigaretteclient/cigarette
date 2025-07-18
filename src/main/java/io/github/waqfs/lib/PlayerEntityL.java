package io.github.waqfs.lib;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class PlayerEntityL {
    public static @Nullable ItemStack getHeldItem(PlayerEntity player) {
        ItemStack stack = player.getInventory().getSelectedStack();
        if (stack.isEmpty()) return null;
        return stack;
    }
}
