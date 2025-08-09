package io.github.waqfs.module.bedwars;

import io.github.waqfs.GameDetector;
import io.github.waqfs.agent.BedwarsAgent;
import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.module.TickModule;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

public class AutoTool extends TickModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "Auto Tool";
    protected static final String MODULE_TOOLTIP = "Automatically swaps your tool to the correct one when breaking blocks.";
    protected static final String MODULE_ID = "bedwars.autotool";

    public AutoTool() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
    }

    private boolean isTool(ItemStack item) {
        return this.isPickaxe(item) || this.isAxe(item) || this.isShears(item);
    }

    private boolean isPickaxe(ItemStack item) {
        return item.isOf(Items.WOODEN_PICKAXE) || item.isOf(Items.STONE_PICKAXE) || item.isOf(Items.IRON_PICKAXE) || item.isOf(Items.GOLDEN_PICKAXE) || item.isOf(Items.DIAMOND_PICKAXE);
    }

    private boolean isAxe(ItemStack item) {
        return item.isOf(Items.WOODEN_AXE) || item.isOf(Items.STONE_AXE) || item.isOf(Items.IRON_AXE) || item.isOf(Items.GOLDEN_AXE) || item.isOf(Items.DIAMOND_AXE);
    }

    private boolean isShears(ItemStack item) {
        return item.isOf(Items.SHEARS);
    }

    private boolean shouldPickaxe(BlockState state) {
        return BedwarsAgent.isEndStone(state) || BedwarsAgent.isClay(state) || BedwarsAgent.isObsidian(state);
    }

    private boolean shouldAxe(BlockState state) {
        return BedwarsAgent.isWood(state) || state.isOf(Blocks.LADDER);
    }

    private boolean shouldShears(BlockState state) {
        return BedwarsAgent.isWool(state);
    }

    private void switchToSlot(MinecraftClient client, @NotNull ClientPlayerEntity player, int slot) {
        if (client.currentScreen == null) {
            player.getInventory().setSelectedSlot(slot);
        }
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        ItemStack heldItem = player.getMainHandStack();
        if (!isTool(heldItem)) return;

        HitResult hitResult = client.crosshairTarget;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
        BlockState state = world.getBlockState(pos);

        PlayerInventory inventory = player.getInventory();
        if (shouldPickaxe(state) && !isPickaxe(heldItem)) {
            for (int i = 0; i < 9; i++) {
                ItemStack item = inventory.getStack(i);
                if (!isPickaxe(item)) continue;
                switchToSlot(client, player, i);
                return;
            }
        } else if (shouldAxe(state) && !isAxe(heldItem)) {
            for (int i = 0; i < 9; i++) {
                ItemStack item = inventory.getStack(i);
                if (!isAxe(item)) continue;
                switchToSlot(client, player, i);
                return;
            }
        } else if (shouldShears(state) && !isShears(heldItem)) {
            for (int i = 0; i < 9; i++) {
                ItemStack item = inventory.getStack(i);
                if (!isShears(item)) continue;
                switchToSlot(client, player, i);
                return;
            }
        }
    }

    @Override
    protected boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }
}
