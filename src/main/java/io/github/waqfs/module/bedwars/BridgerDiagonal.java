package io.github.waqfs.module.bedwars;

import io.github.waqfs.GameDetector;
import io.github.waqfs.agent.BedwarsAgent;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

public class BridgerDiagonal extends TickModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "Diagonal Bridger";
    protected static final String MODULE_TOOLTIP = "Automatically bridges diagonally.";
    protected static final String MODULE_ID = "bedwars.bridger.diagonal";

    private int liftTicks = 0;
    private int runningTicks = 0;
    private boolean autoEnabled = false;
    private boolean godMode = false;
//    private boolean didJump = false;

    private void autoEnable() {
        autoEnabled = true;
        InputOverride.isActive = true;
        InputOverride.pitch = 77.0f;
        InputOverride.sneakKey = true;
        InputOverride.backKey = true;
        InputOverride.leftKey = false;
        InputOverride.rightKey = false;
        InputOverride.jumpKey = false;
    }

    public BridgerDiagonal() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
    }

    private float getBoundedYaw(ClientPlayerEntity player) {
        float yaw = player.getYaw();
        if (yaw < -180) {
            while (yaw < -180) {
                yaw += 360;
            }
            return yaw;
        } else if (yaw > 180) {
            while (yaw > 180) {
                yaw -= 360;
            }
            return yaw;
        }
        return yaw;
    }

    private void whileEnabled(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        KeyBinding sneakBinding = KeyBinding.byId("key.sneak");
        KeyBinding backBinding = KeyBinding.byId("key.back");
        KeyBinding leftBinding = KeyBinding.byId("key.left");
        KeyBinding rightBinding = KeyBinding.byId("key.right");
        KeyBinding useBinding = KeyBinding.byId("key.use");
        KeyBinding jumpBinding = KeyBinding.byId("key.jump");
        if (sneakBinding == null || backBinding == null || leftBinding == null || rightBinding == null || useBinding == null || jumpBinding == null) return;

        if (!sneakBinding.isPressed() || !backBinding.isPressed() || !useBinding.isPressed()) {
            autoEnabled = false;
            InputOverride.isActive = false;
            return;
        }
        HitResult hitResult = client.crosshairTarget;
        if (hitResult == null) return;
        InputOverride.sneakKey = (godMode && runningTicks++ == 2) || liftTicks-- <= 0;

        if (!BedwarsAgent.isBlock(player.getMainHandStack())) {
            boolean hasMoreBlocks = BedwarsAgent.switchToTheNextStackOfWoolOrClayOrEndStoneOrWoodOrObsidianOrGlassOrAnyOtherPlaceableBlockThatIsNotALadderOrTNTBecauseThatIsNotARealBlockInTheHotOfTheBarImmediatelyOnTheSubsequentTick(player);
            if (!hasMoreBlocks) {
                autoEnabled = false;
                InputOverride.isActive = false;
            }
            return;
        }

//        if (jumpBinding.isPressed()) didJump = true;
//        if (liftTicks == 1 && didJump) {
        if (liftTicks == 1 && jumpBinding.isPressed()) {
            InputOverride.jumpKey = true;
//            didJump = false;
        } else {
            InputOverride.jumpKey = false;
        }

        if (hitResult.getType() != HitResult.Type.BLOCK) return;
        BlockHitResult blockHitResult = (BlockHitResult) hitResult;

        if (blockHitResult.getSide() != Direction.UP && blockHitResult.getSide() != Direction.DOWN) {
            if (blockHitResult.getBlockPos().getY() >= player.getY()) {
                autoEnabled = false;
                InputOverride.isActive = false;
                return;
            }
            KeyBindingAccessor useAccessor = (KeyBindingAccessor) useBinding;
            useAccessor.setTimesPressed(useAccessor.getTimesPressed() + 2);
            if (godMode && runningTicks >= 80) {
                InputOverride.sneakKey = true;
                runningTicks = 0;
            } else {
                liftTicks = 5;
            }
        } else if (blockHitResult.getSide() == Direction.UP && blockHitResult.getBlockPos().getY() < player.getY() - 1) {
            KeyBindingAccessor useAccessor = (KeyBindingAccessor) useBinding;
            useAccessor.setTimesPressed(useAccessor.getTimesPressed() + 1);
        }

        if (player.getMainHandStack().getCount() <= 1) {
            BedwarsAgent.switchToTheNextStackOfWoolOrClayOrEndStoneOrWoodOrObsidianOrGlassOrAnyOtherPlaceableBlockThatIsNotALadderOrTNTBecauseThatIsNotARealBlockInTheHotOfTheBarImmediatelyOnTheSubsequentTick(player);
        }
    }

    private void whileDisabled(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        KeyBinding sneakBinding = KeyBinding.byId("key.sneak");
        KeyBinding backBinding = KeyBinding.byId("key.back");
        KeyBinding leftBinding = KeyBinding.byId("key.left");
        KeyBinding rightBinding = KeyBinding.byId("key.right");
        KeyBinding useBinding = KeyBinding.byId("key.use");
        KeyBinding jumpBinding = KeyBinding.byId("key.jump");
        if (sneakBinding == null || backBinding == null || leftBinding == null || rightBinding == null || useBinding == null || jumpBinding == null) return;

        if (!sneakBinding.isPressed() || !backBinding.isPressed() || leftBinding.isPressed() || rightBinding.isPressed() || !useBinding.isPressed()) return;
        if (!BedwarsAgent.isBlock(player.getMainHandStack())) return;
        HitResult hitResult = client.crosshairTarget;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return;
        BlockHitResult blockHitResult = (BlockHitResult) hitResult;
        if (blockHitResult.getBlockPos().getY() >= player.getY()) return;
        if (blockHitResult.getSide() == Direction.UP || blockHitResult.getSide() == Direction.DOWN) return;
        float boundedYaw = getBoundedYaw(player);

        BlockPos blockPos = blockHitResult.getBlockPos();
        BlockPos playerPos = player.getBlockPos();
        if (playerPos.getY() - blockPos.getY() != 1) return;
        if (Math.abs(playerPos.getX() - blockPos.getX()) > 1 || Math.abs(playerPos.getZ() - blockPos.getZ()) > 1) return;
        godMode = playerPos.getX() == blockPos.getX() || playerPos.getZ() == blockPos.getZ();

        if (boundedYaw > 0 && boundedYaw < 90) {
            InputOverride.yaw = 45;
        } else if (boundedYaw > 90 && boundedYaw < 180) {
            InputOverride.yaw = 135;
        } else if (boundedYaw > -180 && boundedYaw < -90) {
            InputOverride.yaw = -135;
        } else if (boundedYaw > -90 && boundedYaw < 0) {
            InputOverride.yaw = -45;
        }
        autoEnable();
        runningTicks = 3;
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        KeyBinding sneakBinding = KeyBinding.byId("key.sneak");
        KeyBinding backBinding = KeyBinding.byId("key.back");
        KeyBinding leftBinding = KeyBinding.byId("key.left");
        KeyBinding rightBinding = KeyBinding.byId("key.right");
        KeyBinding useBinding = KeyBinding.byId("key.use");
        KeyBinding jumpBinding = KeyBinding.byId("key.jump");
        if (sneakBinding == null || backBinding == null || leftBinding == null || rightBinding == null || useBinding == null || jumpBinding == null) return;

        if (autoEnabled) whileEnabled(client, world, player);
        else whileDisabled(client, world, player);
    }

    @Override
    protected boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }
}
