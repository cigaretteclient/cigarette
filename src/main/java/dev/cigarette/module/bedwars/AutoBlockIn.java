package dev.cigarette.module.bedwars;

import dev.cigarette.GameDetector;
import dev.cigarette.agent.BedwarsAgent;
import dev.cigarette.gui.widget.*;
import dev.cigarette.helper.KeybindHelper;
import dev.cigarette.lib.PlayerEntityL;
import dev.cigarette.lib.Raycast;
import dev.cigarette.module.TickModule;
import dev.cigarette.precomputed.BlockIn;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
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
    private final SliderWidget proximityToBeds = new SliderWidget("Max Proximity", "How many blocks close you need to be to any beds for the module to be allowed to activate. Set to 0 to remove the proximity requirement entirely, allowing you to block-in anywhere.").withBounds(0, 5, 9);
    private final ToggleWidget switchToBlocks = new ToggleWidget("Switch to Blocks", "Automatically switches to blocks once activated.").withDefaultState(true);
    private final ToggleWidget switchToTool = new ToggleWidget("Switch to Tools", "Automatically switches to a tool once finished.").withDefaultState(true);
    private final SliderWidget variation = new SliderWidget("Variation", "Applies randomness to the delay between block places.").withBounds(0, 1, 4);
    private final ToggleWidget jumpEnabled = new ToggleWidget("Jump", "Jumps immediately to ensure the block above you is placed.").withDefaultState(true);

    private final TextWidget allowedBlocksText = new TextWidget("Block Config", "Configure which blocks can be used by the module.").centered(false);
    private final DropdownWidget<TextWidget, BaseWidget.Stateless> allowedBlocks = new DropdownWidget<>("", null);
    private final ToggleWidget prioritizeStrongest = new ToggleWidget("Prioritize Strongest", "Prioritizes stronger blocks over weaker ones when placing.\nOrder: Obsidian > Endstone > Wood > Clay > Wool > Glass").withDefaultState(true);
    private final ToggleWidget enableObsidian = new ToggleWidget("Use Obsidian", "Allows the module to use obsidian blocks.").withDefaultState(false);
    private final ToggleWidget enableEndstone = new ToggleWidget("Use Endstone", "Allows the module to use endstone blocks.").withDefaultState(true);
    private final ToggleWidget enableWood = new ToggleWidget("Use Wood", "Allows the module to use wood blocks.").withDefaultState(true);
    private final ToggleWidget enableClay = new ToggleWidget("Use Clay", "Allows the module to use clay blocks.").withDefaultState(true);
    private final ToggleWidget enableWool = new ToggleWidget("Use Wool", "Allows the module to use wool blocks.").withDefaultState(true);
    private final ToggleWidget enableGlass = new ToggleWidget("Use Glass", "Allows the module to use glass blocks.").withDefaultState(false);

    private boolean running = false;
    private BlockPos originalPos = null;
    private Vec3d originalPosVec = null;
    private float originalYaw = 0;
    private float originalPitch = 0;
    private Vec3d previousVector = null;
    private int cooldownTicks = 0;
    private boolean hasJumped = false;

    private AutoBlockIn(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        allowedBlocks.setHeader(allowedBlocksText);
        allowedBlocks.setChildren(prioritizeStrongest, enableObsidian, enableEndstone, enableWood, enableClay, enableWool, enableGlass);
        this.setChildren(keybind, allowedBlocks, switchToBlocks, switchToTool, jumpEnabled, speed, variation, proximityToBeds);
        keybind.registerConfigKey(id + ".key");
        speed.registerConfigKey(id + ".speed");
        proximityToBeds.registerConfigKey(id + ".proximity");
        switchToBlocks.registerConfigKey(id + ".switchblocks");
        switchToTool.registerConfigKey(id + ".switchtool");
        variation.registerConfigKey(id + ".variation");
        prioritizeStrongest.registerConfigKey(id + ".prioritizestrongest");
        enableObsidian.registerConfigKey(id + ".allow.obsidian");
        enableEndstone.registerConfigKey(id + ".allow.endstone");
        enableWood.registerConfigKey(id + ".allow.wood");
        enableClay.registerConfigKey(id + ".allow.clay");
        enableWool.registerConfigKey(id + ".allow.wool");
        enableGlass.registerConfigKey(id + ".allow.glass");
    }

    private void enable(@NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        running = true;
        hasJumped = false;
        originalPos = player.getBlockPos();
        originalPosVec = player.getPos();
        originalYaw = player.getYaw();
        originalPitch = player.getPitch();
    }

    private void disable(@NotNull ClientPlayerEntity player) {
        running = false;
        hasJumped = false;
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
            if (distance > 4) continue;

            Direction face = Direction.fromVector(offset, Direction.UP).getOpposite();
            if (face == Direction.UP && eye.getY() <= faceCenter.getY()) continue;
            if (face == Direction.DOWN && eye.getY() >= faceCenter.getY()) continue;
            if (face == Direction.NORTH && eye.getZ() >= faceCenter.getZ()) continue;
            if (face == Direction.SOUTH && eye.getZ() <= faceCenter.getZ()) continue;
            if (face == Direction.EAST && eye.getX() <= faceCenter.getX()) continue;
            if (face == Direction.WEST && eye.getX() >= faceCenter.getX()) continue;

            BlockHitResult res = Raycast.raycastBlock(eye, faceCenter, ShapeContext.absent());
            if (res.getType() != BlockHitResult.Type.MISS) {
                if (!res.getBlockPos().equals(neighborPos)) continue;
            }

            if (closest == null || distance < closestDistance) {
                closest = new ReachableNeighbor(neighborPos, face, faceCenter, distance);
                closestDistance = distance;
            }
        }
        return closest;
    }

    private @Nullable NextVector getNextBlockPlaceVector(@NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        BlockPos newBlockPos = player.getBlockPos();
        if (newBlockPos.getX() != originalPos.getX() || newBlockPos.getZ() != originalPos.getZ() || Math.abs(newBlockPos.getY() - originalPos.getY()) > 2) return null;
        ReachableNeighbor closest = null;
        ReachableNeighbor jumpNeighbor = null;
        for (Vec3i offset : BlockIn.PLAYER_NEIGHBORS) {
            BlockPos pos = originalPos.add(offset);
            if (!world.getBlockState(pos).isAir()) continue;

            ReachableNeighbor neighbor = getReachableNeighbor(world, player, pos);
            if (neighbor == null) continue;

            if (offset.getX() == 0 && offset.getZ() == 0) {
                jumpNeighbor = neighbor;
                continue;
            }
            if (closest == null || neighbor.distance < closest.distance) {
                closest = neighbor;
            }
        }
        if (closest != null) {
            return new NextVector(closest.faceCenter().subtract(player.getEyePos()).normalize(), false);
        }
        if (jumpNeighbor != null) {
            return new NextVector(jumpNeighbor.faceCenter().subtract(player.getEyePos()).normalize(), true);
        }
        return null;
    }

    private boolean switchToNextStackOfBlocks(@NotNull ClientPlayerEntity player) {
        int bestSlot = 0;
        BlockPriority bestBlock = null;
        for (int i = 0; i < 9; i++) {
            BlockPriority block = BlockPriority.fromStack(player.getInventory().getStack(i));
            if (!block.isBedwarsBlock()) continue;
            if (!prioritizeStrongest.getRawState()) {
                player.getInventory().setSelectedSlot(i);
                return true;
            }
            if (block == BlockPriority.OBSIDIAN && !enableObsidian.getRawState()) continue;
            if (block == BlockPriority.ENDSTONE && !enableEndstone.getRawState()) continue;
            if (block == BlockPriority.WOOD && !enableWood.getRawState()) continue;
            if (block == BlockPriority.CLAY && !enableClay.getRawState()) continue;
            if (block == BlockPriority.WOOL && !enableWool.getRawState()) continue;
            if (block == BlockPriority.GLASS && !enableGlass.getRawState()) continue;
            if (bestBlock == null || block.strongerThan(bestBlock)) {
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
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (!running) {
            if (!keybind.getKeybind().wasPhysicallyPressed()) return;
            if (proximityToBeds.getRawState() == 0) {
                enable(world, player);
                return;
            }
            BlockPos pos = player.getBlockPos();
            for (BedwarsAgent.PersistentBed bed : BedwarsAgent.getVisibleBeds()) {
                if (bed.head().isWithinDistance(pos, proximityToBeds.getRawState()) || bed.foot().isWithinDistance(pos, proximityToBeds.getRawState())) {
                    enable(world, player);
                    return;
                }
            }
            return;
        }
        if (--cooldownTicks > 0) return;

        if (!switchToBlocks.getRawState() || !switchToNextStackOfBlocks(player)) {
            disable(player);
            return;
        }

        NextVector next = getNextBlockPlaceVector(world, player);
        if (next == null || (previousVector != null && previousVector.equals(next.lookVector))) {
            if (jumpEnabled.getRawState() && player.getPos().getY() > originalPosVec.getY()) {
                return;
            }
            disableAndSwitch(player);
            return;
        }
        previousVector = next.lookVector;

        PlayerEntityL.setRotationVector(player, next.lookVector);
        KeybindHelper.KEY_USE_ITEM.press();

        int rand = variation.getRawState().intValue() > 0 ? (int) (Math.random() * variation.getRawState().intValue()) : 0;
        cooldownTicks = 16 - speed.getRawState().intValue() + rand;

        if (jumpEnabled.getRawState() && !hasJumped && !next.aboveHead) {
            if (world.getBlockState(BlockPos.ofFloored(player.getPos().add(0, 2, 0))).isAir()) {
                KeybindHelper.KEY_JUMP.holdForTicks(1);
                hasJumped = true;
            }
        }
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }

    private record ReachableNeighbor(BlockPos pos, Direction side, Vec3d faceCenter, double distance) {
    }

    private record NextVector(Vec3d lookVector, boolean aboveHead) {
    }

    private enum BlockPriority {
        OBSIDIAN(10), ENDSTONE(8), WOOD(6), CLAY(4), WOOL(2), GLASS(1), NONE(0);

        private int id;

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
}
