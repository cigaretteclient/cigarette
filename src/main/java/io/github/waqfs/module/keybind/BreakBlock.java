package io.github.waqfs.module.keybind;

import io.github.waqfs.module.BaseModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public class BreakBlock extends BaseModule implements ClientModInitializer {
    protected static final String MODULE_NAME = "Break Block";
    protected static final String MODULE_TOOLTIP = "Breaks the block you're looking at client-side.";
    protected static final String MODULE_ID = "keybind.break_block";
    private static KeyBinding keyBinding;

    public BreakBlock() {
        super(MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                if (!this.isEnabled()) break;

                HitResult target = client.crosshairTarget;
                if (target == null || client.world == null || client.player == null) break;
                if (target.getType() != HitResult.Type.BLOCK) break;

                BlockHitResult blockTarget = (BlockHitResult) target;
                client.world.breakBlock(blockTarget.getBlockPos(), false);

            }
        });
    }

    @Override
    public void onInitializeClient() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("breakblock.standalone.cigarette.waqfs", InputUtil.Type.KEYSYM, GLFW.GLFW_NOT_INITIALIZED, "standalone.cigarette.waqfs"));
    }
}
