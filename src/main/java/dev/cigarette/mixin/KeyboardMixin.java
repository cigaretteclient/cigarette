package dev.cigarette.mixin;

import dev.cigarette.helper.KeybindHelper;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int action, KeyInput input, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (window != client.getWindow().getHandle()) {
            ci.cancel();
            return;
        }
        if (client.currentScreen == null) {
            if (KeybindHelper.handleKeyByGUI(client, input.key(), input.scancode(), action, input.modifiers()) || KeybindHelper.handleBlockedKeyInputs(client, input.key(), input.scancode(), action, input.modifiers()) || KeybindHelper.handleCustomKeys(client, input.key(), input.scancode(), action, input.modifiers())) {
                ci.cancel();
                return;
            }
        } else if (action == GLFW.GLFW_RELEASE) {
            KeybindHelper.handleCustomKeys(client, input.key(), input.scancode(), action, input.modifiers());
        }
    }
}
