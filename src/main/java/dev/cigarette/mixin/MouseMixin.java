package dev.cigarette.mixin;

import dev.cigarette.helper.KeybindHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void updateMouse(double timeDelta, CallbackInfo ci) {
        if (!KeybindHelper.isMouseBlocked()) return;
        ci.cancel();
    }

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (window != client.getWindow().getHandle()) {
            ci.cancel();
            return;
        }
        if (KeybindHelper.handleBlockedMouseInputs(client, button, action, mods) || KeybindHelper.handleCustomMouse(client, button, action, mods)) {
            ci.cancel();
            return;
        }
    }
}
