package io.github.waqfs.module.keybind;

import io.github.waqfs.gui.widget.ToggleOptionsWidget;
import io.github.waqfs.module.TickModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class VClip extends TickModule<ToggleOptionsWidget, Boolean> implements ClientModInitializer {
    protected static final String MODULE_NAME = "V-Clip Down";
    protected static final String MODULE_TOOLTIP = "Vertically clips you down through floors.";
    protected static final String MODULE_ID = "keybind.vclip";
    private static KeyBinding keyBinding;

    public VClip() {
        super(ToggleOptionsWidget.base, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
    }

    @Override
    public void onInitializeClient() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("V-Clip Downward", InputUtil.Type.KEYSYM, GLFW.GLFW_NOT_INITIALIZED, "Cigarette | Standalone"));
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        while (keyBinding.wasPressed()) {
            Vec3d pos = player.getPos();
            BlockPos blockPos = player.getBlockPos();
            for (int offset = 3; offset < 6; offset++) {
                if (!world.getBlockState(blockPos.add(0, -1 * offset, 0)).isAir()) continue;
                if (!world.getBlockState(blockPos.add(0, -1 * offset + 1, 0)).isAir()) continue;
                player.setPosition(pos.x, pos.y - offset, pos.z);
                break;
            }
        }
    }
}
