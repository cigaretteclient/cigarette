package dev.cigarette.mixin;

import dev.cigarette.Cigarette;
import dev.cigarette.lib.WorldL;
import dev.cigarette.module.skyblock.RedGifter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Shadow
    public ClientPlayerEntity player;

    @Shadow
    @Nullable
    public HitResult crosshairTarget;

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void doItemUse(CallbackInfo ci) {
        if (RedGifter.INSTANCE.getRawState() && RedGifter.INSTANCE.inValidGame() && RedGifter.INSTANCE.gifter.getRawState() && RedGifter.holdingGift() && this.crosshairTarget != null && this.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity target = ((EntityHitResult) this.crosshairTarget).getEntity();
            if (target instanceof PlayerEntity && WorldL.isRealPlayer((PlayerEntity) target)) {
                ci.cancel();
                RedGifter.INSTANCE.playerToGift = ((PlayerEntity) target).getGameProfile().getId();
                if(RedGifter.INSTANCE.trashDropLocation == null || RedGifter.INSTANCE.worthDropLocation == null) {
                    Cigarette.CHAT_LOGGER.warning("Missing drop locations, dropping may get mixed.");
                }
            }
        }
    }
}
