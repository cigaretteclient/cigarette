package dev.cigarette.module.bedwars;

import dev.cigarette.GameDetector;
import dev.cigarette.agent.BedwarsAgent;
import dev.cigarette.gui.widget.*;
import dev.cigarette.helper.KeybindHelper;
import dev.cigarette.helper.PlayerEntityHelper;
import dev.cigarette.helper.RaycastHelper;
import dev.cigarette.module.TickModule;
import dev.cigarette.precomputed.BlockIn;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
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
    private final ToggleWidget tpToCenter = new ToggleWidget("TP to Center", "Risky option that teleports you to the center of the block when activating the module to ensure proper placement.").withDefaultState(false);

    private final TextWidget allowedBlocksText = new TextWidget("Block Config", "Configure which blocks can be used by the module.").centered(false);
    private final DropdownWidget<TextWidget, BaseWidget.Stateless> allowedBlocks = new DropdownWidget<>("", null);
    private final ToggleWidget prioritizeStrongest = new ToggleWidget("Prioritize Strongest", "Prioritizes stronger blocks over weaker ones when placing.\nOrder: Obsidian > Endstone > Wood > Clay > Wool > Glass").withDefaultState(true);
    private final ToggleWidget weakNonAdjacent = new ToggleWidget("Weak Non-Adjacent", "Places the weakest blocks on positions not immediately next to or above the player.").withDefaultState(true);
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
        allowedBlocks.setChildren(prioritizeStrongest, weakNonAdjacent, enableObsidian, enableEndstone, enableWood, enableClay, enableWool, enableGlass);
        this.setChildren(keybind, allowedBlocks, switchToBlocks, switchToTool, jumpEnabled, tpToCenter, speed, variation, proximityToBeds);
        keybind.registerConfigKey(id + ".key");
        speed.registerConfigKey(id + ".speed");
        proximityToBeds.registerConfigKey(id + ".proximity");
        switchToBlocks.registerConfigKey(id + ".switchblocks");
        switchToTool.registerConfigKey(id + ".switchtool");
        variation.registerConfigKey(id + ".variation");
        prioritizeStrongest.registerConfigKey(id + ".prioritizestrongest");
        tpToCenter.registerConfigKey(id + ".tptocenter");
        weakNonAdjacent.registerConfigKey(id + ".weaknonadjacent");
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

    private boolean blockBlockedByEntity(@NotNull ClientWorld world, @NotNull ClientPlayerEntity player, @NotNull BlockPos pos) {
        return !world.getOtherEntities(player, new Box(pos)).isEmpty();
    }

    private @Nullable ReachableNeighbor getReachableNeighbor(@NotNull ClientWorld world, @NotNull ClientPlayerEntity player, BlockPos pos) {
        ReachableNeighbor closest = null;
        double closestDistance = 0;
        for (Vec3i offset : BlockIn.BLOCK_NEIGHBORS) {
            BlockPos neighborPos = pos.add(offset);
            BlockState state = world.getBlockState(neighborPos);
            if (state.isAir() || state.isOf(Blocks.WATER) || BedwarsAgent.isBed(state) || state.isOf(Blocks.LADDER)) continue;

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

            HitResult res = RaycastHelper.raycast(eye, faceCenter, ShapeContext.absent());
            if (res.getType() == BlockHitResult.Type.BLOCK) {
                if (!((BlockHitResult) res).getBlockPos().equals(neighborPos)) continue;
            } else if (res.getType() == HitResult.Type.ENTITY) {
                continue;
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
        Vec3i closestOffset = null;
        ReachableNeighbor jumpNeighbor = null;
        for (Vec3i offset : BlockIn.PLAYER_NEIGHBORS) {
            BlockPos pos = originalPos.add(offset);

            BlockState state = world.getBlockState(pos);
            if (!state.isAir() && !state.isOf(Blocks.WATER)) continue;
            if (blockBlockedByEntity(world, player, pos)) continue;

            ReachableNeighbor neighbor = getReachableNeighbor(world, player, pos);
            if (neighbor == null) continue;

            if (offset.getX() == 0 && offset.getZ() == 0) {
                jumpNeighbor = neighbor;
                continue;
            }
            if (closest == null || neighbor.distance < closest.distance) {
                closest = neighbor;
                closestOffset = offset;
            }
        }
        if (closest != null) {
            return new NextVector(closest.faceCenter().subtract(player.getEyePos()).normalize(), false, BlockIn.isPlayerAdjacent(closestOffset));
        }
        if (jumpNeighbor != null) {
            return new NextVector(jumpNeighbor.faceCenter().subtract(player.getEyePos()).normalize(), true, true);
        }
        return null;
    }

    private boolean switchToNextStackOfBlocks(@NotNull ClientPlayerEntity player) {
        return BedwarsAgent.switchToNextStackOfBlocks(player, new BedwarsAgent.BlockConfig(enableObsidian.getRawState(), enableEndstone.getRawState(), enableWood.getRawState(), enableClay.getRawState(), enableWool.getRawState(), enableGlass.getRawState(), prioritizeStrongest.getRawState(), !prioritizeStrongest.getRawState()));
    }

    private boolean switchToWeakestBlocks(@NotNull ClientPlayerEntity player) {
        return BedwarsAgent.switchToNextStackOfBlocks(player, new BedwarsAgent.BlockConfig(enableObsidian.getRawState(), enableEndstone.getRawState(), enableWood.getRawState(), enableClay.getRawState(), enableWool.getRawState(), enableGlass.getRawState(), false, false));
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (!running) {
            if (!keybind.getKeybind().isPhysicallyPressed()) return;
            double xDecimal = player.getX() - Math.floor(player.getX());
            double zDecimal = player.getZ() - Math.floor(player.getZ());
            if (xDecimal < 0.3 || xDecimal > 0.7 || zDecimal < 0.3 || zDecimal > 0.7) {
                if (!tpToCenter.getRawState()) return;
                double xCorrection = (xDecimal < 0.3 ? (0.3 + Math.random() * 0.069) : Math.min(xDecimal, 0.7 - Math.random() * 0.069)) - xDecimal;
                double zCorrection = (zDecimal < 0.3 ? 0.3 + Math.random() * 0.069 : Math.min(zDecimal, 0.7 - Math.random() * 0.069)) - zDecimal;
                player.updatePosition(player.getX() + xCorrection, player.getY(), player.getZ() + zCorrection);
            }

            enableChecks:
            if (proximityToBeds.getRawState() == 0) {
                enable(world, player);
            } else {
                BlockPos pos = player.getBlockPos();
                for (BedwarsAgent.PersistentBed bed : BedwarsAgent.getVisibleBeds()) {
                    if (bed.head().isWithinDistance(pos, proximityToBeds.getRawState()) || bed.foot().isWithinDistance(pos, proximityToBeds.getRawState())) {
                        enable(world, player);
                        break enableChecks;
                    }
                }
                return;
            }
        }
        if (--cooldownTicks > 0) return;

        NextVector next = getNextBlockPlaceVector(world, player);
        if (next == null || (previousVector != null && previousVector.equals(next.lookVector))) {
            if (jumpEnabled.getRawState() && player.getPos().getY() > originalPosVec.getY()) {
                return;
            }
            disableAndSwitch(player);
            return;
        }
        previousVector = next.lookVector;

        if (!switchToBlocks.getRawState() || !(next.isAdjacent || !weakNonAdjacent.getRawState() ? switchToNextStackOfBlocks(player) : switchToWeakestBlocks(player))) {
            disable(player);
            return;
        }

        PlayerEntityHelper.setRotationVector(player, next.lookVector);
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

    private record NextVector(Vec3d lookVector, boolean aboveHead, boolean isAdjacent) {
    }
}
