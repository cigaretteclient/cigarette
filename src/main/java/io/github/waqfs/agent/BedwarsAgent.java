package io.github.waqfs.agent;

import io.github.waqfs.GameDetector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashSet;

public class BedwarsAgent implements ClientModInitializer {
    private static final HashSet<PersistentBed> persistentBeds = new HashSet<>();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.world == null || GameDetector.rootGame != GameDetector.ParentGame.BEDWARS) {
                this.unset();
                return;
            }
        });
    }

    public static HashSet<PersistentBed> getVisibleBeds() {
        HashSet<PersistentBed> visibleBeds = new HashSet<>();
        HashSet<PersistentBed> nonVisible = new HashSet<>();
        for (PersistentBed bed : persistentBeds) {
            if (!bed.exists()) {
                nonVisible.add(bed);
                continue;
            }
            visibleBeds.add(bed);
        }
        for (PersistentBed bed : nonVisible) {
            persistentBeds.remove(bed);
        }
        return visibleBeds;
    }

    public static void addBed(BlockPos pos, BlockState state) {
        for (PersistentBed bed : persistentBeds) {
            if (bed.contains(pos)) return;
        }

        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return;

        BedPart part = state.get(BedBlock.PART);
        Direction facing = state.get(HorizontalFacingBlock.FACING);

        boolean isHead = part == BedPart.HEAD;
        boolean isFoot = part == BedPart.FOOT;

        BlockPos otherBedPos = isHead ? pos.subtract(facing.getVector()) : pos.add(facing.getVector());
        persistentBeds.add(new PersistentBed(isHead ? pos : otherBedPos, facing, isFoot ? pos : otherBedPos, facing.getOpposite()));
    }

    private void unset() {
        persistentBeds.clear();
    }

    public record PersistentBed(BlockPos head, Direction headDirection, BlockPos foot, Direction footDirection) {
        public PersistentBed(BlockPos head, Direction headDirection, BlockPos foot, Direction footDirection) {
            this.head = new BlockPos(head);
            this.headDirection = headDirection;
            this.foot = new BlockPos(foot);
            this.footDirection = footDirection;
        }

        public boolean exists() {
            ClientWorld world = MinecraftClient.getInstance().world;
            if (world == null) return false;
            BlockState block = world.getBlockState(this.head);
            return block.getBlock() instanceof BedBlock;
        }

        public boolean contains(BlockPos pos) {
            return this.head.equals(pos) || this.foot.equals(pos);
        }
    }
}
