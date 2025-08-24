package dev.cigarette.module.keybind;

import dev.cigarette.gui.widget.KeybindWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.module.TickModule;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

public class AddGlassBlock extends TickModule<ToggleWidget, Boolean> {
    public static final AddGlassBlock INSTANCE = new AddGlassBlock("keybind.place_glass", "Place Glass", "Places a client-side block where you are facing.");

    private final KeybindWidget keybind = new KeybindWidget("Keybind", "Key to trigger the placing of a glass block.");

    private AddGlassBlock(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.setChildren(keybind);
        keybind.registerConfigKey(id + ".key");
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        while (keybind.getKeybind().wasPressed()) {
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
