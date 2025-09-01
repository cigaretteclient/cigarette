package dev.cigarette.mixin;

import dev.cigarette.lib.Glow;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void isGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (Glow.hasGlow(((Entity) (Object) this).getUuid())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isGlowingLocal", at = @At("HEAD"), cancellable = true)
    private void isGlowingLocal(CallbackInfoReturnable<Boolean> cir) {
        if (Glow.hasGlow(((Entity) (Object) this).getUuid())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void getTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        UUID uuid = ((Entity) (Object) this).getUuid();
        if (Glow.hasGlow(uuid)) {
            cir.setReturnValue(Glow.getGlowColor(uuid));
        }
    }

    @Inject(method = "setYaw", at = @At("HEAD"))
    public void setYaw(float yaw, CallbackInfo ci) {
        if (yaw > 360) yaw = yaw % 360;
        if (yaw < 0) yaw = 360 + (yaw % 360);
    }
    @Inject(method = "setPitch", at = @At("HEAD"))
    public void setPitch(float pitch, CallbackInfo ci) {
        if (pitch > 90) pitch = 90;
        if (pitch < -90) pitch = -90;
    }
}
