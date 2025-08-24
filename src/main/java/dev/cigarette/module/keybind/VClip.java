package dev.cigarette.module.keybind;

import dev.cigarette.gui.widget.KeybindWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public class VClip extends TickModule<ToggleWidget, Boolean> {
    public static final VClip INSTANCE = new VClip("keybind.vclip", "V-Clip Down", "Vertically clips you down through floors.");

    private final KeybindWidget keybind = new KeybindWidget("Keybind", "Key to trigger the downward clipping.");

    private VClip(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.setChildren(keybind);
        keybind.registerConfigKey(id + ".key");
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        while (keybind.getKeybind().wasPressed()) {
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
