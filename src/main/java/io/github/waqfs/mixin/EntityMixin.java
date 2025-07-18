package io.github.waqfs.mixin;

import io.github.waqfs.lib.Glow;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
}
