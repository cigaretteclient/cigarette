package io.github.waqfs.module.bedwars;

import io.github.waqfs.GameDetector;
import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.lib.InputOverride;
import io.github.waqfs.mixin.KeyBindingAccessor;
import io.github.waqfs.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

public class Bridger extends TickModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "Bridger";
    protected static final String MODULE_TOOLTIP = "Automatically bridges.";
    protected static final String MODULE_ID = "bedwars.bridger";

    public Bridger() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
    }

    private int liftTicks = 0;
    private boolean autoEnabled = false;
    private boolean right = false;

    private void autoEnable() {
        autoEnabled = true;
        InputOverride.isActive = true;
        InputOverride.pitch = 77.0f;
        InputOverride.sneakKey = true;
        InputOverride.backKey = true;
        InputOverride.leftKey = !right;
        InputOverride.rightKey = right;
        InputOverride.jumpKey = false;
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        KeyBinding sneakBinding = KeyBinding.byId("key.sneak");
        KeyBinding backBinding = KeyBinding.byId("key.back");
        KeyBinding leftBinding = KeyBinding.byId("key.left");
        KeyBinding rightBinding = KeyBinding.byId("key.right");
        KeyBinding useBinding = KeyBinding.byId("key.use");
        if (sneakBinding == null || backBinding == null || leftBinding == null || rightBinding == null || useBinding == null) return;
        if (autoEnabled) {
            KeyBinding dirBinding = right ? rightBinding : leftBinding;
            if (!sneakBinding.isPressed() || !backBinding.isPressed() || !dirBinding.isPressed() || !useBinding.isPressed()) {
                autoEnabled = false;
                InputOverride.isActive = false;
                return;
            }
            KeyBinding jumpBinding = KeyBinding.byId("key.jump");
            if (jumpBinding == null) return;
            InputOverride.jumpKey = jumpBinding.isPressed();
            HitResult hitResult = client.crosshairTarget;
            if (hitResult == null) return;
            InputOverride.sneakKey = liftTicks-- <= 0;
            if (hitResult.getType() != HitResult.Type.BLOCK) return;
            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
            if (blockHitResult.getSide() != Direction.UP && blockHitResult.getSide() != Direction.DOWN) {
                if (blockHitResult.getBlockPos().getY() >= player.getY()) {
                    autoEnabled = false;
                    InputOverride.isActive = false;
                    return;
                }
                KeyBindingAccessor useAccessor = (KeyBindingAccessor) useBinding;
                useAccessor.setTimesPressed(useAccessor.getTimesPressed() + 1);
                liftTicks = 4;
            } else if (blockHitResult.getSide() == Direction.UP && blockHitResult.getBlockPos().getY() < player.getY() - 1) {
                KeyBindingAccessor useAccessor = (KeyBindingAccessor) useBinding;
                useAccessor.setTimesPressed(useAccessor.getTimesPressed() + 1);
            }
        } else {
            if (!sneakBinding.isPressed() || !backBinding.isPressed() || (!leftBinding.isPressed() && !rightBinding.isPressed()) || !useBinding.isPressed()) return;
            right = rightBinding.isPressed();
            HitResult hitResult = client.crosshairTarget;
            if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return;
            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
            if (blockHitResult.getBlockPos().getY() >= player.getY()) return;
            switch (blockHitResult.getSide()) {
                case NORTH -> {
                    autoEnable();
                    InputOverride.yaw = right ? 45.0f : -45.0f;
                }
                case SOUTH -> {
                    autoEnable();
                    InputOverride.yaw = right ? -135.0f : 135.0f;
                }
                case WEST -> {
                    autoEnable();
                    InputOverride.yaw = right ? -45.0f : -135.0f;
                }
                case EAST -> {
                    autoEnable();
                    InputOverride.yaw = right ? 135.0f : 45.0f;
                }
            }
        }
    }

    @Override
    protected boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }
}
