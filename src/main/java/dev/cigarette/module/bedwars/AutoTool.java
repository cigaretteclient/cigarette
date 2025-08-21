package dev.cigarette.module.bedwars;

import dev.cigarette.GameDetector;
import dev.cigarette.agent.BedwarsAgent;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.module.TickModule;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
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
        if (!BedwarsAgent.isTool(heldItem)) return;

        HitResult hitResult = client.crosshairTarget;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
        BlockState state = world.getBlockState(pos);

        PlayerInventory inventory = player.getInventory();
        if (shouldPickaxe(state) && !BedwarsAgent.isPickaxe(heldItem)) {
            for (int i = 0; i < 9; i++) {
                ItemStack item = inventory.getStack(i);
                if (!BedwarsAgent.isPickaxe(item)) continue;
                switchToSlot(client, player, i);
                return;
            }
        } else if (shouldAxe(state) && !BedwarsAgent.isAxe(heldItem)) {
            for (int i = 0; i < 9; i++) {
                ItemStack item = inventory.getStack(i);
                if (!BedwarsAgent.isAxe(item)) continue;
                switchToSlot(client, player, i);
                return;
            }
        } else if (shouldShears(state) && !BedwarsAgent.isShears(heldItem)) {
            for (int i = 0; i < 9; i++) {
                ItemStack item = inventory.getStack(i);
                if (!BedwarsAgent.isShears(item)) continue;
                switchToSlot(client, player, i);
                return;
            }
        }
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }
}
