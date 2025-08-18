package io.github.waqfs.module.zombies;

import io.github.waqfs.GameDetector;
import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.mixin.ClientWorldAccessor;
import io.github.waqfs.mixin.MinecraftClientInvoker;
import io.github.waqfs.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.NotNull;

public class ReviveAura extends RenderModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "Revive Aura";
    protected static final String MODULE_TOOLTIP = "Automatically revives downed teammates.";
    protected static final String MODULE_ID = "zombies.revive_aura";

    public ReviveAura() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
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

        // DO NOT UNCOMMENT, THIS BANS YOU LOL
        // player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, player.isSneaking()));
        System.out.println("Reviving " + target.getName().getString() + " at " + target.getPos());
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {}

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.ZOMBIES && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_ZOMBIES;
    }
}
