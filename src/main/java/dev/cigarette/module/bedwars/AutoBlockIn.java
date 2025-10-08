package dev.cigarette.module.bedwars;

import dev.cigarette.GameDetector;
import dev.cigarette.agent.BedwarsAgent;
import dev.cigarette.gui.widget.KeybindWidget;
import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.lib.PlayerEntityL;
import dev.cigarette.mixin.KeyBindingAccessor;
import dev.cigarette.module.TickModule;
import dev.cigarette.precomputed.BlockIn;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoBlockIn extends TickModule<ToggleWidget, Boolean> {
    public static final AutoBlockIn INSTANCE = new AutoBlockIn("bedwars.autoblockin", "Auto Block-In", "Automatically surrounds you in blocks to help break beds.");

    private final KeybindWidget keybind = new KeybindWidget("Keybind", "A key to trigger the block in module.");
    private final SliderWidget speed = new SliderWidget("Speed", "The higher the speed, the less time spent between adjusting the camera and placing blocks.").withBounds(0, 12, 15);
    private final SliderWidget proximityToBeds = new SliderWidget("Max Proximity", "How many blocks close you need to be to any beds for the module to be allowed to activate.").withBounds(1, 5, 9);
    private final ToggleWidget switchToBlocks = new ToggleWidget("Switch to Blocks", "Automatically switches to blocks once activated.").withDefaultState(true);
    private final ToggleWidget switchToTool = new ToggleWidget("Switch to Tools", "Automatically switches to a tool once finished.").withDefaultState(true);
    private final SliderWidget variation = new SliderWidget("Variation", "Applies randomness to the delay between block places.").withBounds(0, 1, 4);

    private KeyBinding rightClickKey = null;
    private boolean running = false;
    private BlockPos originalPos = null;
    private float originalYaw = 0;
    private float originalPitch = 0;
    private Vec3d previousVector = null;
    private int cooldownTicks = 0;

    private AutoBlockIn(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.setChildren(keybind, speed, proximityToBeds, switchToBlocks, switchToTool, variation);
        keybind.registerConfigKey(id + ".key");
        speed.registerConfigKey(id + ".speed");
        proximityToBeds.registerConfigKey(id + ".proximity");
        switchToBlocks.registerConfigKey(id + ".switchblocks");
        switchToTool.registerConfigKey(id + ".switchtool");
        variation.registerConfigKey(id + ".variation");
    }

    private void enable(@NotNull ClientPlayerEntity player) {
        running = true;
        originalPos = player.getBlockPos();
        originalYaw = player.getYaw();
        originalPitch = player.getPitch();
    }

    private void disable(@NotNull ClientPlayerEntity player) {
        running = false;
        player.setYaw(originalYaw);
        player.setPitch(originalPitch);
        previousVector = null;
    }

    private void disableAndSwitch(@NotNull ClientPlayerEntity player) {
        disable(player);
        if (switchToTool.getRawState()) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (BedwarsAgent.isTool(stack)) {
                    player.getInventory().setSelectedSlot(i);
                    break;
                }
            }
        }
    }

    private @Nullable ReachableNeighbor getReachableNeighbor(@NotNull ClientWorld world, @NotNull ClientPlayerEntity player, BlockPos pos) {
        ReachableNeighbor closest = null;
        double closestDistance = 0;
        for (Vec3i offset : BlockIn.BLOCK_NEIGHBORS) {
            BlockPos neighborPos = pos.add(offset);
            if (world.getBlockState(neighborPos).isAir()) continue;
            Vec3d faceCenter = neighborPos.toCenterPos().subtract(new Vec3d(offset).multiply(0.5f));
            Vec3d eye = player.getEyePos();

            double distance = faceCenter.distanceTo(eye);
            if (distance > 3) continue;

            Direction face = Direction.fromVector(offset, Direction.UP).getOpposite();
            if (face == Direction.UP && eye.getY() <= faceCenter.getY()) continue;
            if (face == Direction.DOWN && eye.getY() >= faceCenter.getY()) continue;
            if (face == Direction.NORTH && eye.getZ() >= faceCenter.getZ()) continue;
            if (face == Direction.SOUTH && eye.getZ() <= faceCenter.getZ()) continue;
            if (face == Direction.EAST && eye.getX() <= faceCenter.getX()) continue;
            if (face == Direction.WEST && eye.getX() >= faceCenter.getX()) continue;

            if (closest == null || distance < closestDistance) {
                closest = new ReachableNeighbor(neighborPos, face, faceCenter);
                closestDistance = distance;
            }
        }
        return closest;
    }

    private @Nullable Vec3d getNextBlockPlaceVector(@NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (!player.getBlockPos().equals(originalPos)) return null;
        for (Vec3i offset : BlockIn.PLAYER_NEIGHBORS) {
            BlockPos pos = originalPos.add(offset);
            if (!world.getBlockState(pos).isAir()) continue;

            ReachableNeighbor neighbor = getReachableNeighbor(world, player, pos);
            if (neighbor == null) continue;

            return neighbor.faceCenter().subtract(player.getEyePos()).normalize();
        }
        return null;
    }

    private void rightClick() {
        KeyBindingAccessor useAccessor = (KeyBindingAccessor) rightClickKey;
        useAccessor.setTimesPressed(useAccessor.getTimesPressed() + 1);
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (rightClickKey == null) {
            rightClickKey = KeyBinding.byId("key.use");
            return;
        }
        if (!running) {
            if (!keybind.getKeybind().wasPhysicallyPressed()) return;
            BlockPos pos = player.getBlockPos();
            for (BedwarsAgent.PersistentBed bed : BedwarsAgent.getVisibleBeds()) {
                if (bed.head().isWithinDistance(pos, proximityToBeds.getRawState()) || bed.foot().isWithinDistance(pos, proximityToBeds.getRawState())) {
                    enable(player);
                    return;
                }
            }
            return;
        }
        if (--cooldownTicks > 0) return;

        if (!BedwarsAgent.isBlock(player.getMainHandStack()) && (!switchToBlocks.getRawState() || !BedwarsAgent.switchToNextStackOfBlocks(player))) {
            disable(player);
            return;
        }

        Vec3d nextLookVector = getNextBlockPlaceVector(world, player);
        if (nextLookVector == null || (previousVector != null && previousVector.equals(nextLookVector))) {
            disableAndSwitch(player);
            return;
        }
        previousVector = nextLookVector;

        PlayerEntityL.setRotationVector(player, nextLookVector);
        rightClick();

        int rand = variation.getRawState().intValue() > 0 ? (int) (Math.random() * variation.getRawState().intValue()) : 0;
        cooldownTicks = 16 - speed.getRawState().intValue() + rand;
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }

    private record ReachableNeighbor(BlockPos pos, Direction side, Vec3d faceCenter) {
    }
}
