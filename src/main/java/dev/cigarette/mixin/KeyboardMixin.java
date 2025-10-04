package dev.cigarette.mixin;

import dev.cigarette.helper.KeybindHelper;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (window != client.getWindow().getHandle()) {
            ci.cancel();
            return;
        }
        if (KeybindHelper.handleKeyByGUI(client, key, scancode, action, modifiers) || KeybindHelper.handleBlockedKeyInputs(client, key, scancode, action, modifiers) || KeybindHelper.handleCustomKeys(client, key, scancode, action, modifiers)) {
            ci.cancel();
            return;
        }
    }
}
