package dev.cigarette.agent;

import dev.cigarette.GameDetector;
import dev.cigarette.gui.widget.ToggleWidget;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

public class BedwarsAgent extends BaseAgent {
    private static final HashSet<PersistentBed> persistentBeds = new HashSet<>();

    public BedwarsAgent(@Nullable ToggleWidget devToggle) {
        super(devToggle);
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

    public static boolean isBed(BlockState state) {
        return state.isIn(BlockTags.BEDS);
    }

    public static boolean isWool(BlockState state) {
        return state.isIn(BlockTags.WOOL);
    }

    public static boolean isEndStone(BlockState state) {
        return state.isOf(Blocks.END_STONE);
    }

    public static boolean isWood(BlockState state) {
        return state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.PLANKS);
    }

    public static boolean isClay(BlockState state) {
        return state.isIn(BlockTags.TERRACOTTA);
    }

    public static boolean isObsidian(BlockState state) {
        return state.isOf(Blocks.OBSIDIAN);
    }

    public static boolean isGlass(BlockState state) {
        return state.isIn(BlockTags.IMPERMEABLE);
    }

    public static boolean isBlock(BlockState state) {
        return isWool(state) || isEndStone(state) || isWood(state) || isClay(state) || isObsidian(state) || isGlass(state);
    }

    public static boolean isBlock(ItemStack item) {
        if (!(item.getItem() instanceof BlockItem blockItem)) return false;
        return isBlock(blockItem.getBlock().getDefaultState());
    }

    public static boolean isTool(ItemStack item) {
        return isPickaxe(item) || isAxe(item) || isShears(item);
    }

    public static boolean isPickaxe(ItemStack item) {
        return item.isOf(Items.WOODEN_PICKAXE) || item.isOf(Items.STONE_PICKAXE) || item.isOf(Items.IRON_PICKAXE) || item.isOf(Items.GOLDEN_PICKAXE) || item.isOf(Items.DIAMOND_PICKAXE);
    }

    public static boolean isAxe(ItemStack item) {
        return item.isOf(Items.WOODEN_AXE) || item.isOf(Items.STONE_AXE) || item.isOf(Items.IRON_AXE) || item.isOf(Items.GOLDEN_AXE) || item.isOf(Items.DIAMOND_AXE);
    }

    public static boolean isShears(ItemStack item) {
        return item.isOf(Items.SHEARS);
    }


    public static boolean switchToNextStackOfBlocks(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (isBlock(player.getInventory().getStack(i))) {
                player.getInventory().setSelectedSlot(i);
                return true;
            }
        }
        return false;
    }

    public static boolean switchToNextStackOfBlocks(@NotNull ClientPlayerEntity player, BlockConfig config) {
        if (config.selectFirst && isBlock(player.getMainHandStack())) return true;
        int bestSlot = 0;
        BlockPriority bestBlock = null;
        for (int i = 0; i < 9; i++) {
            BlockPriority block = BlockPriority.fromStack(player.getInventory().getStack(i));
            if (!block.isBedwarsBlock()) continue;
            if (config.selectFirst) {
                player.getInventory().setSelectedSlot(i);
                return true;
            }
            if (block == BlockPriority.OBSIDIAN && !config.enableObsidian) continue;
            if (block == BlockPriority.ENDSTONE && !config.enableEndstone) continue;
            if (block == BlockPriority.WOOD && !config.enableWood) continue;
            if (block == BlockPriority.CLAY && !config.enableClay) continue;
            if (block == BlockPriority.WOOL && !config.enableWool) continue;
            if (block == BlockPriority.GLASS && !config.enableGlass) continue;

            if (bestBlock == null || (block.strongerThan(bestBlock) == config.orderStrongest)) {
                bestSlot = i;
                bestBlock = block;
            }
        }
        if (bestBlock != null) {
            player.getInventory().setSelectedSlot(bestSlot);
            return true;
        }
        return false;
    }

    @Override
    public boolean inValidGame() {
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

    private enum BlockPriority {
        OBSIDIAN(10), ENDSTONE(8), WOOD(6), CLAY(4), WOOL(2), GLASS(1), NONE(0);

        private final int id;

        BlockPriority(int id) {
            this.id = id;
        }

        public boolean isBedwarsBlock() {
            return this != NONE;
        }

        public boolean strongerThan(BlockPriority other) {
            return this.id > other.id;
        }

        public static BlockPriority fromStack(ItemStack item) {
            if (item.getItem() instanceof BlockItem blockItem) {
                BlockState state = blockItem.getBlock().getDefaultState();
                if (BedwarsAgent.isObsidian(state)) return OBSIDIAN;
                if (BedwarsAgent.isEndStone(state)) return ENDSTONE;
                if (BedwarsAgent.isWood(state)) return WOOD;
                if (BedwarsAgent.isClay(state)) return CLAY;
                if (BedwarsAgent.isWool(state)) return WOOL;
                if (BedwarsAgent.isGlass(state)) return GLASS;
            }
            return NONE;
        }
    }

    public record BlockConfig(boolean enableObsidian, boolean enableEndstone, boolean enableWood, boolean enableClay, boolean enableWool, boolean enableGlass, boolean orderStrongest, boolean selectFirst) {
    }
}
