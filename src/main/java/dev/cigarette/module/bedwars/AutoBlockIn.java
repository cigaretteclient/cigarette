package dev.cigarette.module.bedwars;

import dev.cigarette.GameDetector;
import dev.cigarette.gui.widget.KeybindWidget;
import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.lib.PlayerEntityL;
import dev.cigarette.module.TickModule;
import dev.cigarette.precomputed.BlockIn;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoBlockIn extends TickModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "Auto Block-In";
    protected static final String MODULE_TOOLTIP = "Automatically surrounds you in blocks to help break beds.";
    protected static final String MODULE_ID = "bedwars.autoblockin";

    private final KeybindWidget keybind = new KeybindWidget(Text.literal("Keybind"), Text.literal("A key to trigger the block in module."));
    private final SliderWidget speed = new SliderWidget(Text.literal("Speed"), Text.literal("The higher the speed, the less time spent between adjusting the camera and placing blocks.")).withBounds(0, 12, 15);

    private boolean running = false;
    private BlockPos originalPos = null;
    private float originalYaw = 0;
    private float originalPitch = 0;

    public AutoBlockIn() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        keybind.registerConfigKey("bedwars.autoblockin.key");
        speed.registerConfigKey("bedwars.autoblockin.speed");
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
        player.getPitch(originalPitch);
    }

    private @Nullable ReachableNeighbor getReachableNeighbor(@NotNull ClientWorld world, @NotNull ClientPlayerEntity player, BlockPos pos) {
        ReachableNeighbor closest = null;
        double closestDistance = 0;
        for (Vec3i offset : BlockIn.BLOCK_NEIGHBORS) {
            BlockPos neighborPos = pos.add(offset);
            if (world.getBlockState(neighborPos).isAir()) continue;
            Vec3d faceCenter = neighborPos.toCenterPos().add(new Vec3d(offset).multiply(0.5f));
            double distance = faceCenter.distanceTo(player.getEyePos());
            if (distance < 3 && (closest == null || distance < closestDistance)) {
                closest = new ReachableNeighbor(neighborPos, Direction.fromVector(offset, null), faceCenter);
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
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (!running) {
            if (!keybind.getKeybind().isPressed()) return;
            enable(player);
        }
        int ticksPerBlock = 16 - speed.getRawState().intValue();
        Vec3d nextLookVector = getNextBlockPlaceVector(world, player);
        if (nextLookVector == null) {
            disable(player);
            return;
        }

        PlayerEntityL.setRotationVector(player, nextLookVector);
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }

    private record ReachableNeighbor(BlockPos pos, Direction side, Vec3d faceCenter) {
    }
}
