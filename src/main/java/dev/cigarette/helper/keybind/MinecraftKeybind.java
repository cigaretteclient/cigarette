package dev.cigarette.helper.keybind;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class MinecraftKeybind extends VirtualKeybind {
    private @Nullable KeyBinding minecraftBinding;
    private final @NotNull String minecraftKeybindId;

    public MinecraftKeybind(@NotNull String keybindId) {
        super(GLFW.GLFW_KEY_UNKNOWN);
        this.minecraftBinding = KeyBinding.byId(keybindId);
        this.minecraftKeybindId = keybindId;
    }

    /**
     * Attempts to link this object with the native {@code KeyBinding} from Minecraft.
     *
     * @return whether this object was linked successfully or not
     */
    public boolean tryToAttach() {
        this.minecraftBinding = KeyBinding.byId(this.minecraftKeybindId);
        return this.isAttached();
    }

    /**
     * Checks if this object is linked with the native {@code KeyBinding} from Minecraft.
     *
     * @return whether this object is linked or not
     */
    public boolean isAttached() {
        return this.minecraftBinding != null;
    }

    /**
     * {@return whether this key is being virtually held down or not} This can result from the key being physically held down or set to be held by other code.
     */
    public boolean isVirtuallyPressed() {
        if (this.minecraftBinding == null) return false;
        return this.minecraftBinding.isPressed();
    }

    /**
     * {@return whether this key was virtually pressed or not} This can result from the key being physically pressed or set to have been pressed by other code.
     */
    public boolean wasVirtuallyPressed() {
        if (this.minecraftBinding == null) return false;
        return this.minecraftBinding.wasPressed();
    }

    /**
     * Virtually presses the key triggering any Minecraft related events.
     */
    public void press() {
        if (this.minecraftBinding == null) return;
        InputUtil.Key key = InputUtil.fromTranslationKey(this.minecraftBinding.getBoundKeyTranslationKey());
        KeyBinding.onKeyPressed(key);
    }

    @Override
    public boolean isOf(int key, int scancode) {
        if (this.minecraftBinding == null) return false;
        return this.minecraftBinding.matchesKey(key, scancode);
    }
}
