package io.github.waqfs.module.keybind;

import io.github.waqfs.module.BaseModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Blocks;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class AddGlassBlock extends BaseModule implements ClientModInitializer {
    protected static final String MODULE_NAME = "Place Glass";
    protected static final String MODULE_TOOLTIP = "Places a client-side block where you are facing.";
    protected static final String MODULE_ID = "keybind.place_glass";
    private static KeyBinding keyBinding;

    public AddGlassBlock() {
        super(MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                if (!this.isEnabled()) break;

                HitResult target = client.crosshairTarget;
                if (target == null || client.world == null) break;
                if (target.getType() != HitResult.Type.BLOCK) break;

                BlockHitResult blockTarget = (BlockHitResult) target;
                BlockPos pos = blockTarget.getBlockPos();
                pos = pos.add(blockTarget.getSide().getVector());
                client.world.setBlockState(pos, Blocks.GLASS.getDefaultState());
            }
        });
    }

    @Override
    public void onInitializeClient() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("addglassblock.standalone.cigarette.waqfs", InputUtil.Type.KEYSYM, GLFW.GLFW_NOT_INITIALIZED, "standalone.cigarette.waqfs"));
    }
}
