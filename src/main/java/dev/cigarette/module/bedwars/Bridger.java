package dev.cigarette.module.bedwars;

import dev.cigarette.GameDetector;
import dev.cigarette.agent.BedwarsAgent;
import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.TextWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.helper.keybind.InputBlocker;
import dev.cigarette.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

import static dev.cigarette.helper.KeybindHelper.*;

public class Bridger extends TickModule<ToggleWidget, Boolean> {
    public static final Bridger INSTANCE = new Bridger("bedwars.bridger", "Bridger", "Automatically bridges.");
    public static final InputBlocker INPUT_BLOCKER = new InputBlocker(KEY_JUMP, KEY_MOVE_BACK, KEY_MOVE_LEFT, KEY_MOVE_RIGHT,
            KEY_USE_ITEM, KEY_SNEAK).preventCameraChanges();

    private final SliderWidget speed = new SliderWidget("Speed", "The higher the speed, the less time spent shifting. To look more legit or improve consistency, lower the speed. Setting to 3 will naturally god bridge straight & diagonally inconsistently.").withBounds(0, 2, 3);
    private final ToggleWidget blockSwap = new ToggleWidget("Auto Swap Blocks", "Automatically swap to the next available slot with placeable blocks when current stack runs out.").withDefaultState(true);
    private final ToggleWidget toggleStraight = new ToggleWidget("Straight", "Toggles automatic straight bridging.").withDefaultState(true);
    private final ToggleWidget toggleDiagonal = new ToggleWidget("Diagonal", "Toggles automatic diagonal bridging.").withDefaultState(true);
    private final ToggleWidget toggleDiagonalGod = new ToggleWidget("God Bridging", "Toggles diagonal god bridging when positioning yourself half a block from the corner.").withDefaultState(false);

    private int runningTicks = 0;
    private BridgeType bridgingMode = BridgeType.NONE;

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

    private float straightBridgeYaw(Direction placingFace) {
        boolean right = KEY_MOVE_RIGHT.isPhysicallyPressed();
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

    private void cycleIfNoBlocks(ClientPlayerEntity player) {
        if (!blockSwap.getRawState()) return;
        if (!BedwarsAgent.isBlock(player.getMainHandStack())) {
            boolean hasMoreBlocks = BedwarsAgent.switchToNextStackOfBlocks(player);
            if (!hasMoreBlocks) {
                disable();
            }
        }
    }

    private void enable(@NotNull ClientPlayerEntity player, BridgeType mode, float yaw) {
        this.bridgingMode = mode;
        tryBlockInputs(this, INPUT_BLOCKER);
        player.setPitch(77.0f);
        player.setYaw(yaw);
        KEY_SNEAK.hold();
    }

    private void disable() {
        unblock();
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (this.bridgingMode == BridgeType.NONE) {
            if (!KEY_SNEAK.isPhysicallyPressed() || !KEY_MOVE_BACK.isPhysicallyPressed() || !KEY_USE_ITEM.isPhysicallyPressed()) return;

            HitResult hitResult = client.crosshairTarget;
            if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return;

            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
            Direction placingFace = blockHitResult.getSide();
            if (placingFace == Direction.UP || placingFace == Direction.DOWN) return;

            BlockPos blockPos = blockHitResult.getBlockPos();
            BlockPos playerPos = player.getBlockPos();
            if (playerPos.getY() - blockPos.getY() != 1) return;
            if (Math.abs(playerPos.getX() - blockPos.getX()) > 1 || Math.abs(playerPos.getZ() - blockPos.getZ()) > 1) return;

            BridgeType mode = BridgeType.get(blockPos, playerPos, toggleStraight.getRawState(), toggleDiagonal.getRawState(), toggleDiagonalGod.getRawState());
            float yaw = 0;
            switch (mode) {
                case STRAIGHT -> yaw = straightBridgeYaw(placingFace);
                case DIAGONAL, DIAGONAL_GOD -> yaw = diagonalBridgeYaw(player);
            }
            if (mode != BridgeType.NONE) {
                enable(player, mode, yaw);
            }
        } else {
            if (!KEY_SNEAK.isPhysicallyPressed() || !KEY_MOVE_BACK.isPhysicallyPressed() || !KEY_USE_ITEM.isPhysicallyPressed()) {
                bridgingMode = BridgeType.NONE;
                disable();
                return;
            }

            HitResult hitResult = client.crosshairTarget;
            if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return;
            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
            BlockPos blockPos = blockHitResult.getBlockPos();

            boolean facingTop = blockHitResult.getSide() == Direction.UP;
            boolean facingSide = !facingTop && blockPos.getY() < player.getY();

            switch (bridgingMode) {
                case STRAIGHT -> {
                    if (!KEY_MOVE_RIGHT.isPhysicallyPressed() && !KEY_MOVE_LEFT.isPhysicallyPressed()) {
                        disable();
                        return;
                    }

                    KEY_JUMP.hold(KEY_JUMP.isPhysicallyPressed());
                    KEY_USE_ITEM.press();
                    if (facingSide) {
                        KEY_SNEAK.releaseForTicks(2 + speed.getRawState().intValue());
                    }
                }
                case DIAGONAL -> {
                    KEY_JUMP.hold(KEY_JUMP.isPhysicallyPressed() && KEY_SNEAK.ticksLeft() == 1);
                    KEY_USE_ITEM.press();
                    if (facingSide) {
                        KEY_USE_ITEM.press();
                        KEY_SNEAK.releaseForTicks(3 + speed.getRawState().intValue());
                    }
                }
                case DIAGONAL_GOD -> {
                    KEY_JUMP.hold(KEY_JUMP.isPhysicallyPressed() && KEY_SNEAK.ticksLeft() == 1);
                    KEY_SNEAK.hold(runningTicks++ == 2);
                    KEY_USE_ITEM.press();
                    if (facingSide) {
                        KEY_USE_ITEM.press();
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
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }

    protected enum BridgeType {
        NONE(0), STRAIGHT(1), DIAGONAL(2), DIAGONAL_GOD(3);

        protected static BridgeType get(BlockPos blockPos, BlockPos playerPos, boolean straight, boolean diagonal, boolean diagonalGod) {
            boolean left = KEY_MOVE_LEFT.isPhysicallyPressed();
            boolean right = KEY_MOVE_RIGHT.isPhysicallyPressed();
            if (diagonal && !left && !right) {
                if (diagonalGod && (playerPos.getX() == blockPos.getX() || playerPos.getZ() == blockPos.getZ())) {
                    return BridgeType.DIAGONAL_GOD;
                }
                return BridgeType.DIAGONAL;
            }
            if (straight && left != right) return BridgeType.STRAIGHT;
            return BridgeType.NONE;
        }

        private final int id;

        BridgeType(int id) {
            this.id = id;
        }
    }
}
