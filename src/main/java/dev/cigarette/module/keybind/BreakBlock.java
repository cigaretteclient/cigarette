package dev.cigarette.module.keybind;

import dev.cigarette.gui.widget.KeybindWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.NotNull;

public class BreakBlock extends TickModule<ToggleWidget, Boolean> {
    public static final BreakBlock INSTANCE = new BreakBlock("keybind.break_block", "Break Block", "Breaks the block you're looking at client-side.");

    private final KeybindWidget keybind = new KeybindWidget(Text.literal("Keybind"), Text.literal("Key to trigger the breaking of a block."));

    public BreakBlock(String id, String name, String tooltip) {
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
            world.breakBlock(blockTarget.getBlockPos(), false);

        }
    }
}
