package dev.cigarette.mixin;

import dev.cigarette.Cigarette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "animateDamage", at = @At("HEAD"))
    private void onDamaged(CallbackInfo ci) {
        if (!Cigarette.CONFIG.COMBAT_JUMP_RESET.getRawState()) return;

        PlayerEntity thisEntity = (PlayerEntity) (Object) this;

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && thisEntity.getGameProfile().getId().equals(player.getGameProfile().getId()) && player.isOnGround()) {
            player.jump();
        }
    }
}
