package dev.cigarette.helper;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.ItemTags;

public abstract class WeaponHelper {
    public static boolean isRanged(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        return (item instanceof RangedWeaponItem) || (item instanceof ProjectileItem) || stack.isOf(Items.ENDER_PEARL);
    }

    public static boolean isSword(ItemStack stack) {
        return stack.isIn(ItemTags.SWORDS);
    }

    public static int getBestRangedSlot(ClientPlayerEntity player) {
        int best = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (isRanged(s)) {
                best = i;
                // Prefer crossbow over bow if found later; for simplicity, last wins
            }
        }
        return best;
    }

    public static int getBestMeleeSlot(ClientPlayerEntity player) {
        int best = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < 9; i++) {
            ItemStack s = player.getInventory().getStack(i);
            int score = getMeleeScore(s);
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return (bestScore > Integer.MIN_VALUE) ? best : -1;
    }

    public static int getMeleeScore(ItemStack s) {
        if (s == null || s.isEmpty()) return Integer.MIN_VALUE;
        if (s.isOf(Items.NETHERITE_SWORD)) return 90;
        if (s.isOf(Items.DIAMOND_SWORD)) return 80;
        if (s.isOf(Items.IRON_SWORD)) return 70;
        if (s.isOf(Items.STONE_SWORD)) return 60;
        if (s.isOf(Items.GOLDEN_SWORD)) return 55;
        if (s.isOf(Items.WOODEN_SWORD)) return 50;
        if (s.isOf(Items.NETHERITE_AXE)) return 75;
        if (s.isOf(Items.DIAMOND_AXE)) return 68;
        if (s.isOf(Items.IRON_AXE)) return 62;
        if (s.isOf(Items.STONE_AXE)) return 56;
        if (s.isOf(Items.GOLDEN_AXE)) return 50;
        if (s.isOf(Items.WOODEN_AXE)) return 44;
        return Integer.MIN_VALUE;
    }
}
