package io.github.waqfs.module.keybind;

import io.github.waqfs.module.BaseModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class VClip extends BaseModule implements ClientModInitializer {
    protected static final String MODULE_NAME = "V-Clip Down";
    protected static final String MODULE_TOOLTIP = "Vertically clips you down through floors.";
    protected static final String MODULE_ID = "keybind.vclip";
    private static KeyBinding keyBinding;

    public VClip() {
        super(MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                if (!this.isEnabled()) break;
                if (client.player == null || client.world == null) break;

                Vec3d pos = client.player.getPos();
                BlockPos blockPos = client.player.getBlockPos();
                for (int offset = 3; offset < 6; offset++) {
                    if (!client.world.getBlockState(blockPos.add(0, -1 * offset, 0)).isAir()) continue;
                    if (!client.world.getBlockState(blockPos.add(0, -1 * offset + 1, 0)).isAir()) continue;
                    client.player.setPosition(pos.x, pos.y - offset, pos.z);
                    break;
                }
            }
        });
    }

    @Override
    public void onInitializeClient() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("vclip_d5.standalone.cigarette.waqfs", InputUtil.Type.KEYSYM, GLFW.GLFW_NOT_INITIALIZED, "standalone.cigarette.waqfs"));
    }
}
