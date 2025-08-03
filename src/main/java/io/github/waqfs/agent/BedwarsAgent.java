package io.github.waqfs.agent;

import io.github.waqfs.GameDetector;
import io.github.waqfs.module.bedwars.AutoClicker;
import io.github.waqfs.module.bedwars.PerfectHit;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

public class BedwarsAgent extends BaseAgent {
    private static final HashSet<PersistentBed> persistentBeds = new HashSet<>();

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

    public static boolean isWool(BlockState state) {
        return state.isOf(Blocks.WHITE_WOOL) || state.isOf(Blocks.BLACK_WOOL) || state.isOf(Blocks.BLUE_WOOL) || state.isOf(Blocks.BROWN_WOOL) || state.isOf(Blocks.CYAN_WOOL) || state.isOf(Blocks.GRAY_WOOL) || state.isOf(Blocks.GREEN_WOOL) || state.isOf(Blocks.LIGHT_BLUE_WOOL) || state.isOf(Blocks.LIGHT_GRAY_WOOL) || state.isOf(Blocks.LIME_WOOL) || state.isOf(Blocks.MAGENTA_WOOL) || state.isOf(Blocks.ORANGE_WOOL) || state.isOf(Blocks.PINK_WOOL) || state.isOf(Blocks.PURPLE_WOOL) || state.isOf(Blocks.RED_WOOL) || state.isOf(Blocks.YELLOW_WOOL);
    }

    public static boolean isEndStone(BlockState state) {
        return state.isOf(Blocks.END_STONE);
    }

    public static boolean isWood(BlockState state) {
        return state.isOf(Blocks.OAK_PLANKS) || state.isOf(Blocks.ACACIA_PLANKS) || state.isOf(Blocks.SPRUCE_PLANKS) || state.isOf(Blocks.DARK_OAK_PLANKS) || state.isOf(Blocks.BIRCH_PLANKS) || state.isOf(Blocks.ACACIA_LOG) || state.isOf(Blocks.BIRCH_LOG) || state.isOf(Blocks.DARK_OAK_LOG) || state.isOf(Blocks.JUNGLE_LOG) || state.isOf(Blocks.OAK_LOG) || state.isOf(Blocks.SPRUCE_LOG);
    }

    public static boolean isClay(BlockState state) {
        return state.isOf(Blocks.TERRACOTTA) || state.isOf(Blocks.BLACK_TERRACOTTA) || state.isOf(Blocks.BLUE_TERRACOTTA) || state.isOf(Blocks.BROWN_TERRACOTTA) || state.isOf(Blocks.CYAN_TERRACOTTA) || state.isOf(Blocks.GRAY_TERRACOTTA) || state.isOf(Blocks.GREEN_TERRACOTTA) || state.isOf(Blocks.LIGHT_BLUE_TERRACOTTA) || state.isOf(Blocks.LIGHT_GRAY_TERRACOTTA) || state.isOf(Blocks.LIME_TERRACOTTA) || state.isOf(Blocks.MAGENTA_TERRACOTTA) || state.isOf(Blocks.ORANGE_TERRACOTTA) || state.isOf(Blocks.PINK_TERRACOTTA) || state.isOf(Blocks.PURPLE_TERRACOTTA) || state.isOf(Blocks.RED_TERRACOTTA) || state.isOf(Blocks.WHITE_TERRACOTTA) || state.isOf(Blocks.YELLOW_TERRACOTTA);
    }

    public static boolean isObsidian(BlockState state) {
        return state.isOf(Blocks.OBSIDIAN);
    }

    public static boolean isGlass(BlockState state) {
        return state.isOf(Blocks.BLACK_STAINED_GLASS) || state.isOf(Blocks.BLUE_STAINED_GLASS) || state.isOf(Blocks.BROWN_STAINED_GLASS) || state.isOf(Blocks.CYAN_STAINED_GLASS) || state.isOf(Blocks.GRAY_STAINED_GLASS) || state.isOf(Blocks.GREEN_STAINED_GLASS) || state.isOf(Blocks.LIGHT_BLUE_STAINED_GLASS) || state.isOf(Blocks.LIGHT_GRAY_STAINED_GLASS) || state.isOf(Blocks.LIME_STAINED_GLASS) || state.isOf(Blocks.MAGENTA_STAINED_GLASS) || state.isOf(Blocks.ORANGE_STAINED_GLASS) || state.isOf(Blocks.PINK_STAINED_GLASS) || state.isOf(Blocks.PURPLE_STAINED_GLASS) || state.isOf(Blocks.RED_STAINED_GLASS) || state.isOf(Blocks.WHITE_STAINED_GLASS) || state.isOf(Blocks.YELLOW_STAINED_GLASS);
    }

    @Override
    protected boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS;
    }

    @Override
    protected void onValidTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
    }

    @Override
    protected void onInvalidTick(MinecraftClient client) {
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

    public static boolean jumpResetEnabled = false;

    public static PerfectHit perfectHitModule;
    public static AutoClicker autoClickerModule;
}
