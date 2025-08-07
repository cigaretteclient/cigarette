package io.github.waqfs.mixin;

import io.github.waqfs.lib.InputOverride;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void updateMouse(double timeDelta, CallbackInfo ci) {
        if (!InputOverride.isActive) return;
        Entity player = MinecraftClient.getInstance().player;
        if (player != null) {
            player.setAngles(InputOverride.yaw, InputOverride.pitch);
        }
        ci.cancel();
    }
}
