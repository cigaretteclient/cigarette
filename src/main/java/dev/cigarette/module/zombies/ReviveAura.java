package dev.cigarette.module.zombies;

import dev.cigarette.GameDetector;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public class ReviveAura extends RenderModule<ToggleWidget, Boolean> {
    public static final ReviveAura INSTANCE = new ReviveAura("zombies.revive_aura", "Revive Aura", "Automatically revives downed teammates.");

    private int tickCount = 0;

    public ReviveAura(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
    }

    /**
     * @param ctx  World Render Context
     * @param matrixStack Matrix Stack
     */
    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {

    }

    /**
     * @param client Minecraft Client
     * @param world Client World
     * @param player Client Player
     */
    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        final double reachSq = 3 * 3;
        double closestDistSq = Double.MAX_VALUE;
        OtherClientPlayerEntity target = null;

        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof OtherClientPlayerEntity otherClientPlayerEntity)) continue;

            EntityDimensions entityDimensions = otherClientPlayerEntity.getDimensions(otherClientPlayerEntity.getPose());

            // check if a revive entity
            if (entityDimensions.width() != 0.2f) continue;

            // check if within range to hit
            double distSq = player.squaredDistanceTo(otherClientPlayerEntity);

            if (distSq < reachSq && distSq < closestDistSq) {
                closestDistSq = distSq;
                target = otherClientPlayerEntity;
            }
        }

        if (target == null) return;

        // check that player is alive and not spectating
        if (!player.isAlive() || player.isSpectator() || player.isInvisible()) return;

        tickCount++;

        /**
         * {Integer@34732} 2118 -> {ArmorStandEntity@34733} "ArmorStandEntity['0.9s'/2118, l='ClientLevel', x=36.28, y=70.72, z=18.63]"
         * {Integer@34721} 2116 -> {ArmorStandEntity@34722} "ArmorStandEntity['■■■■■■■■■■■■■■■'/2116, l='ClientLevel', x=36.28, y=70.97, z=18.63]"
         * {Integer@34734} 2114 -> {ArmorStandEntity@34735} "ArmorStandEntity['REVIVING...'/2114, l='ClientLevel', x=36.28, y=71.22, z=18.63]"
         * {Integer@34736} 2112 -> {ArmorStandEntity@34737} "ArmorStandEntity['■■■■■■■■■■■■■■■'/2112, l='ClientLevel', x=36.28, y=71.47, z=18.63]"
         */

        // Check if another player is already reviving the target.
        // This is indicated by an Armor Stand with the name "REVIVING..." near the target.
        for (Entity nearbyEntity : world.getEntities()) {
            if (nearbyEntity instanceof ArmorStandEntity armorStand) {
                // Check for the custom name containing "REVIVING..."
                if (armorStand.getCustomName() != null && armorStand.getCustomName().getSiblings().getFirst().getString().contains("REVIVING...")) {
                    // A downed player is in a "laying down" pose. Their origin (getPos()) is at their feet.
                    // The "REVIVING..." text appears over their torso, which is offset from the origin
                    // based on the direction they are facing (yaw). We must calculate this offset.
                    final double offsetDistance = 0.9; // Approximate distance from origin to torso for a downed player model.
                    float yawRad = (float) Math.toRadians(target.getYaw());

                    // Calculate the torso position using trigonometry based on yaw.
                    double torsoX = target.getX() - Math.sin(yawRad) * offsetDistance;
                    double torsoZ = target.getZ() + Math.cos(yawRad) * offsetDistance;

                    // Check if the armor stand is positioned directly above the target's calculated torso position.
                    double deltaX = Math.abs(armorStand.getX() - torsoX);
                    double deltaZ = Math.abs(armorStand.getZ() - torsoZ);
                    // The Y-check is still relative to the target's base, as they are on the ground.
                    double deltaY = armorStand.getY() - target.getY();

                    // Check if horizontally within a ~1x1 block column of the torso and vertically above.
                    if (deltaX < 1.0 && deltaZ < 1.0 && deltaY > 0 && deltaY < 2.5) {
                        // Someone is already reviving this target, so we skip.
                        return;
                    }
                }
            }
        }
        if (tickCount > 19) {
            Vec3d targetCenterPos = target.getBoundingBox().getCenter();
            Vec3d direction = targetCenterPos.subtract(player.getEyePos()).normalize();

            float aimYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
            float aimPitch = (float) Math.toDegrees(Math.asin(-direction.y));

            player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(aimYaw, aimPitch, player.isOnGround(), player.horizontalCollision));
            player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, player.isSneaking()));
            player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

            tickCount = 0;
        }
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {}

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.ZOMBIES && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_ZOMBIES;
    }
}
