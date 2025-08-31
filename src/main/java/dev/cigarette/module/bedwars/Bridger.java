package dev.cigarette.module.bedwars;

import dev.cigarette.GameDetector;
import dev.cigarette.agent.BedwarsAgent;
import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.TextWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.lib.InputOverride;
import dev.cigarette.mixin.KeyBindingAccessor;
import dev.cigarette.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

public class Bridger extends TickModule<ToggleWidget, Boolean> {
    public static final Bridger INSTANCE = new Bridger("bedwars.bridger", "Bridger", "Automatically bridges.");

    private final SliderWidget speed = new SliderWidget("Speed", "The higher the speed, the less time spent shifting. To look more legit or improve consistency, lower the speed. Setting to 3 will naturally god bridge straight & diagonally inconsistently.").withBounds(0, 2, 3);
    private final ToggleWidget blockSwap = new ToggleWidget("Auto Swap Blocks", "Automatically swap to the next available slot with placeable blocks when current stack runs out.").withDefaultState(true);
    private final ToggleWidget toggleStraight = new ToggleWidget("Straight", "Toggles automatic straight bridging.").withDefaultState(true);
    private final ToggleWidget toggleDiagonal = new ToggleWidget("Diagonal", "Toggles automatic diagonal bridging.").withDefaultState(true);
    private final ToggleWidget toggleDiagonalGod = new ToggleWidget("God Bridging", "Toggles diagonal god bridging when positioning yourself half a block from the corner.").withDefaultState(false);

    private KeyBinding sneakKey = null;
    private KeyBinding backwardsKey = null;
    private KeyBinding leftKey = null;
    private KeyBinding rightKey = null;
    private KeyBinding rightClickKey = null;
    private KeyBinding jumpKey = null;
    private int runningTicks = 0;
    private int shiftDiabledTicks = 0;

    protected BridgeType bridgeType = BridgeType.NONE;

    private Bridger(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        TextWidget header = new TextWidget("Bridging Styles").withUnderline();
        this.setChildren(speed, blockSwap, header, toggleStraight, toggleDiagonal, toggleDiagonalGod);
        speed.registerConfigKey(id + ".speed");
        blockSwap.registerConfigKey(id + ".blockswap");
        toggleStraight.registerConfigKey(id + ".straight");
        toggleDiagonal.registerConfigKey(id + ".diagonal");
        toggleDiagonalGod.registerConfigKey(id + ".diagonal.god");
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
            default -> {
                throw new IllegalStateException("The side of the block must be the north, south, west, or east when starting the bridger.");
            }
        }
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
        if (!blockSwap.getRawState()) return;
        if (!BedwarsAgent.isBlock(player.getMainHandStack())) {
            boolean hasMoreBlocks = BedwarsAgent.switchToNextStackOfBlocks(player);
            if (!hasMoreBlocks) {
                disable();
            }
        }
    }

    private void rightClick(int times) {
        KeyBindingAccessor useAccessor = (KeyBindingAccessor) rightClickKey;
        useAccessor.setTimesPressed(useAccessor.getTimesPressed() + times);
    }

    private float continuousYawFor(ClientPlayerEntity player, float desiredYaw) {
        float current = player.getYaw();
        float delta = MathHelper.wrapDegrees(desiredYaw - current);
        return current + delta;
    }

    private void enable(BridgeType type, ClientPlayerEntity player, float yaw) {
        this.bridgeType = type;
        InputOverride.isActive = true;
        InputOverride.pitch = 77.0f;
        InputOverride.yaw = continuousYawFor(player, yaw);
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
                if (!toggleStraight.getRawState()) return;
                enable(BridgeType.STRAIGHT, player, straightBridgeYaw(placingFace));
            } else if (isDiagonalBridge()) {
                if (!toggleDiagonal.getRawState()) return;
                boolean godBridge = toggleDiagonalGod.getRawState() && isDiagonalGodBridge(playerPos, blockPos);
                enable(godBridge ? BridgeType.DIAGONAL_GOD : BridgeType.DIAGONAL, player, diagonalBridgeYaw(player));
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
                    if (!rightKey.isPressed() && !leftKey.isPressed()) {
                        disable();
                        return;
                    }

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
                        shiftDiabledTicks = 2 + speed.getRawState().intValue();
                    }
                }
                case DIAGONAL -> {
                    InputOverride.jumpKey = shiftDiabledTicks == 1 && jumpKey.isPressed();
                    InputOverride.sneakKey = shiftDiabledTicks-- <= 0;

                    if (blockHitResult.getSide() == Direction.UP) {
                        rightClick(1);
                    } else {
                        rightClick(2);
                        shiftDiabledTicks = 3 + speed.getRawState().intValue();
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
                case NONE -> {
                    InputOverride.jumpKey = jumpKey.isPressed();
                    InputOverride.sneakKey = sneakKey.isPressed();
                }
            }

            cycleIfNoBlocks(player);
        }
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }

    protected enum BridgeType {
        NONE(0), STRAIGHT(1), DIAGONAL(2), DIAGONAL_GOD(3);

        private final int id;

        BridgeType(int id) {
            this.id = id;
        }

        /*
            public int getId() {
                return this.id;
            }
        */
    }
}

