package dev.cigarette.mixin;

import dev.cigarette.lib.InputOverride;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Shadow
    private static float getMovementMultiplier(boolean positive, boolean negative) {
        // dummy body
        return 0;
    }

    @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true)
    private void tick(CallbackInfo ci) {
        if (!InputOverride.isActive) return;
        ci.cancel();
        this.playerInput = new PlayerInput(
                InputOverride.forwardKey,
                InputOverride.backKey,
                InputOverride.leftKey,
                InputOverride.rightKey,
                InputOverride.jumpKey,
                InputOverride.sneakKey,
                InputOverride.sprintKey
        );
        float f = getMovementMultiplier(this.playerInput.forward(), this.playerInput.backward());
        float g = getMovementMultiplier(this.playerInput.left(), this.playerInput.right());
        this.movementVector = new Vec2f(g, f).normalize();
    }
}
