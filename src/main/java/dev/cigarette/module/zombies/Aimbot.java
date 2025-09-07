package dev.cigarette.module.zombies;

import dev.cigarette.GameDetector;
import dev.cigarette.agent.ZombiesAgent;
import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.lib.PlayerEntityL;
import dev.cigarette.lib.AimingL;
import dev.cigarette.lib.WeaponSelector;
import dev.cigarette.module.TickModule;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public class Aimbot extends TickModule<ToggleWidget, Boolean> {
    public static final Aimbot INSTANCE = new Aimbot("zombies.aimbot", "Aimbot", "Automatically aims at zombies.");

    private final ToggleWidget silentAim = new ToggleWidget("Silent Aim", "Doesn't snap your camera client-side.").withDefaultState(true);
    private final ToggleWidget autoShoot = new ToggleWidget("Auto Shoot", "Automatically shoots zombies").withDefaultState(true);
    private final ToggleWidget autoWeaponSwitch = new ToggleWidget("Auto Weapon Switch", "Automatically switch weapons").withDefaultState(true);
    public final ToggleWidget predictiveAim = new ToggleWidget("Predictive Aim", "Predict zombie movement for better accuracy").withDefaultState(true);
    public final SliderWidget predictionTicks = new SliderWidget("Prediction Ticks", "How many ticks ahead to predict zombie movement").withBounds(1, 10, 20).withAccuracy(0);

    private KeyBinding rightClickKey = null;

    private Aimbot(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.setChildren(silentAim, autoShoot, autoWeaponSwitch, predictiveAim, predictionTicks);
        silentAim.registerConfigKey(id + ".silentAim");
        autoShoot.registerConfigKey(id + ".autoShoot");
        autoWeaponSwitch.registerConfigKey(id + ".autoWeaponSwitch");
        predictiveAim.registerConfigKey(id + ".predictiveAim");
        predictionTicks.registerConfigKey(id + ".predictionTicks");
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

            float[] angles = AimingL.anglesFromTo(player.getEyePos(), predictedPos);
            float aimYaw = angles[0];
            float aimPitch = angles[1];

            if (!silentAim.getRawState()) {
                PlayerEntityL.setRotationVector(player, vector);
            }

            AimingL.sendAimPacket(player, aimYaw, aimPitch);
        }
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.ZOMBIES && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_ZOMBIES;
    }
}
