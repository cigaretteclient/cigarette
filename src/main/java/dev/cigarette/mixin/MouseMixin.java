package dev.cigarette.mixin;

import dev.cigarette.helper.KeybindHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.input.MouseInput.MouseAction;

import org.lwjgl.glfw.GLFW;
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
    private void onMouseButton(long window, MouseInput input, @MouseAction int action, CallbackInfo ci) {
        int button = input.button();
        int mods = input.modifiers();
        MinecraftClient client = MinecraftClient.getInstance();
        if (window != client.getWindow().getHandle()) {
            ci.cancel();
            return;
        }
        if (client.currentScreen == null) {
            if (KeybindHelper.handleBlockedMouseInputs(client, input.button(), action, input.modifiers()) || KeybindHelper.handleCustomMouse(client, input.button(), action, input.modifiers())) {
                ci.cancel();
                return;
            }
        } else if (action == GLFW.GLFW_RELEASE) {
            KeybindHelper.handleCustomMouse(client, input.button(), action, input.modifiers());
        }
    }
}
