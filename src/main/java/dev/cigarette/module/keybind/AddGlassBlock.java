package dev.cigarette.module.keybind;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.module.TickModule;
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

public class AddGlassBlock extends TickModule<ToggleWidget, Boolean> implements ClientModInitializer {
    public static final AddGlassBlock INSTANCE = Cigarette.CONFIG.constructModule(new AddGlassBlock("keybind.place_glass", "Place Glass", "Places a client-side block where you are facing."), "Keybinds");

    private static KeyBinding keyBinding;

    public AddGlassBlock(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
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
