package io.github.waqfs.module.keybind;

import io.github.waqfs.gui.widget.ToggleOptionsWidget;
import io.github.waqfs.module.TickModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class AddGlassBlock extends TickModule<ToggleOptionsWidget> implements ClientModInitializer {
    protected static final String MODULE_NAME = "Place Glass";
    protected static final String MODULE_TOOLTIP = "Places a client-side block where you are facing.";
    protected static final String MODULE_ID = "keybind.place_glass";
    private static KeyBinding keyBinding;

    public AddGlassBlock() {
        super(ToggleOptionsWidget.base, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
    }

    @Override
    public void onInitializeClient() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("Place Glass Block", InputUtil.Type.KEYSYM, GLFW.GLFW_NOT_INITIALIZED, "Cigarette | Standalone"));
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        while (keyBinding.wasPressed()) {
            HitResult target = client.crosshairTarget;
            if (target == null) break;
            if (target.getType() != HitResult.Type.BLOCK) break;

            BlockHitResult blockTarget = (BlockHitResult) target;
            BlockPos pos = blockTarget.getBlockPos();
            pos = pos.add(blockTarget.getSide().getVector());
            world.setBlockState(pos, Blocks.GLASS.getDefaultState());
        }
    }
}
