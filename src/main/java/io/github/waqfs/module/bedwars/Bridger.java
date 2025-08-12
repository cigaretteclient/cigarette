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

public class Bridger extends TickModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "Bridger";
    protected static final String MODULE_TOOLTIP = "Automatically bridges.";
    protected static final String MODULE_ID = "bedwars.bridger";

    private KeyBinding sneakKey = null;
    private KeyBinding backwardsKey = null;
    private KeyBinding leftKey = null;
    private KeyBinding rightKey = null;
    private KeyBinding rightClickKey = null;
    private KeyBinding jumpKey = null;
    private int runningTicks = 0;
    private int shiftDiabledTicks = 0;

    protected BridgeType bridgeType = BridgeType.NONE;

    public Bridger() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
    }

    private boolean isStraightBridge() {
        if (!leftKey.isPressed() && !rightKey.isPressed()) return false;
        if (leftKey.isPressed() && rightKey.isPressed()) return false;
        return true;
    }

    private float straightBridgeYaw(Direction placingFace) {
        boolean right = rightKey.isPressed();
        switch (placingFace) {
            case NORTH -> {
                return right ? 45f : -45f;
            }
            case SOUTH -> {
                return right ? -135f : 135f;
            }
            case WEST -> {
                return right ? -45f : -135f;
            }
            case EAST -> {
                return right ? 135f : 45f;
            }
        }
        throw new IllegalStateException("The side of the block must be the north, south, west, or east when starting the bridger.");
    }

    private boolean isDiagonalBridge() {
        return !leftKey.isPressed() && !rightKey.isPressed();
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

    private float diagonalBridgeYaw(ClientPlayerEntity player) {
        float yaw = getBoundedYaw(player);
        if (yaw > 0 && yaw < 90) return 45f;
        if (yaw > 90 && yaw < 180) return 135f;
        if (yaw > -180 && yaw < -90) return -135f;
        if (yaw > -90 && yaw < 0) return -45f;
        return player.getYaw();
    }

    private boolean isDiagonalGodBridge(BlockPos playerPos, BlockPos blockPos) {
        return playerPos.getX() == blockPos.getX() || playerPos.getZ() == blockPos.getZ();
    }

    private void cycleIfNoBlocks(ClientPlayerEntity player) {
        if (!BedwarsAgent.isBlock(player.getMainHandStack())) {
            boolean hasMoreBlocks = BedwarsAgent.switchToTheNextStackOfWoolOrClayOrEndStoneOrWoodOrObsidianOrGlassOrAnyOtherPlaceableBlockThatIsNotALadderOrTNTBecauseThatIsNotARealBlockInTheHotOfTheBarImmediatelyOnTheSubsequentTick(player);
            if (!hasMoreBlocks) {
                disable();
            }
        }
    }

    private void rightClick(int times) {
        KeyBindingAccessor useAccessor = (KeyBindingAccessor) rightClickKey;
        useAccessor.setTimesPressed(useAccessor.getTimesPressed() + times);
    }

    private void enable(BridgeType type, float yaw) {
        this.bridgeType = type;
        InputOverride.isActive = true;
        InputOverride.pitch = 77.0f;
        InputOverride.yaw = yaw;
        InputOverride.sneakKey = true;
        InputOverride.backKey = true;
        InputOverride.leftKey = leftKey.isPressed();
        InputOverride.rightKey = rightKey.isPressed();
        InputOverride.jumpKey = false;
    }

    private void disable() {
        this.bridgeType = BridgeType.NONE;
        InputOverride.isActive = false;
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (sneakKey == null || backwardsKey == null || leftKey == null || rightKey == null || rightClickKey == null || jumpKey == null) {
            this.sneakKey = KeyBinding.byId("key.sneak");
            this.backwardsKey = KeyBinding.byId("key.back");
            this.leftKey = KeyBinding.byId("key.left");
            this.rightKey = KeyBinding.byId("key.right");
            this.rightClickKey = KeyBinding.byId("key.use");
            this.jumpKey = KeyBinding.byId("key.jump");
            return;
        }
        if (bridgeType == BridgeType.NONE) {
            if (!sneakKey.isPressed() || !backwardsKey.isPressed() || !rightClickKey.isPressed()) return;

            HitResult hitResult = client.crosshairTarget;
            if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return;
            BlockHitResult blockHitResult = (BlockHitResult) hitResult;

            Direction placingFace = blockHitResult.getSide();
            if (placingFace == Direction.UP || placingFace == Direction.DOWN) return;

            BlockPos blockPos = blockHitResult.getBlockPos();
            BlockPos playerPos = player.getBlockPos();
            if (playerPos.getY() - blockPos.getY() != 1) return;
            if (Math.abs(playerPos.getX() - blockPos.getX()) > 1 || Math.abs(playerPos.getZ() - blockPos.getZ()) > 1) return;

            if (isStraightBridge()) {
                enable(BridgeType.STRAIGHT, straightBridgeYaw(placingFace));
            } else if (isDiagonalBridge()) {
                boolean godBridge = isDiagonalGodBridge(playerPos, blockPos);
                enable(godBridge ? BridgeType.DIAGONAL_GOD : BridgeType.DIAGONAL, diagonalBridgeYaw(player));
            }
        } else {
            if (!sneakKey.isPressed() || !backwardsKey.isPressed() || !rightClickKey.isPressed()) {
                disable();
                return;
            }

            HitResult hitResult = client.crosshairTarget;
            if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return;
            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
            BlockPos blockPos = blockHitResult.getBlockPos();

            switch (bridgeType) {
                case STRAIGHT -> {
                    InputOverride.jumpKey = jumpKey.isPressed();
                    InputOverride.sneakKey = shiftDiabledTicks-- <= 0;

                    cycleIfNoBlocks(player);

                    if (blockHitResult.getSide() == Direction.UP) {
                        rightClick(1);
                    } else {
                        if (blockPos.getY() >= player.getY()) {
                            disable();
                            return;
                        }
                        rightClick(1);
                        shiftDiabledTicks = 4;
                    }
                }
                case DIAGONAL -> {
                    InputOverride.jumpKey = shiftDiabledTicks == 1 && jumpKey.isPressed();
                    InputOverride.sneakKey = shiftDiabledTicks-- <= 0;

                    if (blockHitResult.getSide() == Direction.UP) {
                        rightClick(1);
                    } else {
                        rightClick(2);
                        shiftDiabledTicks = 5;
                    }
                }
                case DIAGONAL_GOD -> {
                    InputOverride.jumpKey = shiftDiabledTicks == 1 && jumpKey.isPressed();
                    InputOverride.sneakKey = runningTicks++ == 2;

                    if (blockHitResult.getSide() == Direction.UP) {
                        rightClick(1);
                    } else {
                        rightClick(2);
                        if (runningTicks >= 80) {
                            runningTicks = 0;
                        }
                    }
                }
            }

            cycleIfNoBlocks(player);
        }
    }

    @Override
    protected boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }

    private enum BridgeType {
        NONE(0), STRAIGHT(1), DIAGONAL(2), DIAGONAL_GOD(3);

        private final int id;

        BridgeType(int id) {
            this.id = id;
        }

        public int getId() {
            return this.id;
        }
    }
}
