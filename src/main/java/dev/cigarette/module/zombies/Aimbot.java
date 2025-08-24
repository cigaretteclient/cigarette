package dev.cigarette.module.zombies;

import dev.cigarette.GameDetector;
import dev.cigarette.agent.ZombiesAgent;
import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.lib.PlayerEntityL;
import dev.cigarette.lib.WeaponSelector;
import dev.cigarette.mixin.ClientWorldAccessor;
import dev.cigarette.module.TickModule;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityPose;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Aimbot extends TickModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "Aimbot";
    protected static final String MODULE_TOOLTIP = "Automatically aims at zombies.";
    protected static final String MODULE_ID = "zombies.aimbot";

    private final ToggleWidget silentAim = new ToggleWidget(Text.literal("Silent Aim"), Text.literal("Doesn't snap your camera client-side.")).withDefaultState(true);
    private final ToggleWidget autoShoot = new ToggleWidget(Text.literal("Auto Shoot"), Text.literal("Automatically shoots zombies")).withDefaultState(true);
    private final ToggleWidget autoWeaponSwitch = new ToggleWidget(Text.literal("Auto Weapon Switch"), Text.literal("Automatically switch weapons")).withDefaultState(true);
    public final ToggleWidget predictiveAim = new ToggleWidget(Text.literal("Predictive Aim"), Text.literal("Predict zombie movement for better accuracy")).withDefaultState(true);
    public final SliderWidget predictionTicks = new SliderWidget(Text.literal("Prediction Ticks"), Text.literal("How many ticks ahead to predict zombie movement")).withBounds(1, 10, 20).withAccuracy(0);

    private KeyBinding rightClickKey = null;

    public Aimbot() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        this.setChildren(silentAim, autoShoot, autoWeaponSwitch, predictiveAim, predictionTicks);
        silentAim.registerConfigKey("zombies.aimbot.silentAim");
        autoShoot.registerConfigKey("zombies.aimbot.autoShoot");
        autoWeaponSwitch.registerConfigKey("zombies.aimbot.autoWeaponSwitch");
        predictiveAim.registerConfigKey("zombies.aimbot.predictiveAim");
        predictionTicks.registerConfigKey("zombies.aimbot.predictionTicks");
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (rightClickKey == null) {
            rightClickKey = KeyBinding.byId("key.use");
            return;
        }

        if (rightClickKey.isPressed() || autoShoot.getRawState()) {
            if (ZombiesAgent.getZombies().isEmpty()) return;

            HitResult hitResult = client.crosshairTarget;
            if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockResult = (BlockHitResult) hitResult;
                BlockState lookingAt = world.getBlockState(blockResult.getBlockPos());
                if (lookingAt.isIn(BlockTags.BUTTONS) || lookingAt.isOf(Blocks.CHEST)) return;
            }

            ZombiesAgent.ZombieTarget bestTarget = ZombiesAgent.getClosestZombie();
            if (bestTarget == null) return;

            if (autoWeaponSwitch.getRawState()) {
                WeaponSelector.switchToBestWeapon(player, bestTarget);
            }

            if (!ZombiesAgent.isGun(player.getMainHandStack())) return;

            Vec3d predictedPos = bestTarget.getEndVec();
            Vec3d vector = predictedPos.subtract(player.getEyePos()).normalize();

            WeaponSelector.addCooldown(player.getInventory().getSelectedSlot());

            float aimYaw = (float) Math.toDegrees(Math.atan2(-vector.x, vector.z));
            float aimPitch = (float) Math.toDegrees(Math.asin(-vector.y));

            if (!silentAim.getRawState()) {
                PlayerEntityL.setRotationVector(player, vector);
            }

            ClientWorldAccessor clientWorldAccessor = (ClientWorldAccessor) world;
            try (PendingUpdateManager pendingUpdateManager = clientWorldAccessor.getPendingUpdateManager().incrementSequence()) {
                int seq = pendingUpdateManager.getSequence();
                player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, seq, aimYaw, aimPitch));
            }
        }
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.ZOMBIES && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_ZOMBIES;
    }
}
