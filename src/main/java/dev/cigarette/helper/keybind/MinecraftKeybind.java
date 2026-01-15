package dev.cigarette.helper.keybind;

import dev.cigarette.helper.TickHelper;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * A wrapper class for more precise control over a Minecraft {@code KeyBinding}.
 */
public class MinecraftKeybind extends VirtualKeybind {
    /**
     * The reference to the actual {@code KeyBinding}.
     */
    private @Nullable KeyBinding minecraftBinding;
    /**
     * The keybind ID to support creation of keybinds before they exist.
     */
    private final @NotNull String minecraftKeybindId;

    /**
     * Create a new {@code KeyBinding} wrapper.
     *
     * @param keybindId The keybinds translation key
     */
    public MinecraftKeybind(@NotNull String keybindId) {
        super(GLFW.GLFW_KEY_UNKNOWN);
        this.minecraftBinding = KeyBinding.byId(keybindId);
        this.minecraftKeybindId = keybindId;
    }

    /**
     * Create a new {@code KeyBinding} wrapper.
     *
     * @param keybindId The keybinds translation key
     * @param isMouse   Whether this is a mouse keybind or not
     */
    public MinecraftKeybind(@NotNull String keybindId, boolean isMouse) {
        super(GLFW.GLFW_KEY_UNKNOWN);
        this.minecraftBinding = KeyBinding.byId(keybindId);
        this.minecraftKeybindId = keybindId;
        this.isMouse = isMouse;
    }

    @Override
    public void setDefaultKey(int defaultKey) {
    }

    @Override
    public void setKey(int key) {
        if (this.minecraftBinding == null) return;
        if (this.isMouse) {
            this.minecraftBinding.setBoundKey(InputUtil.Type.MOUSE.createFromCode(key));
        } else {
            this.minecraftBinding.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(key));
        }
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
     * Virtually presses the key triggering any Minecraft related events. For player movement, you must use {@code holdForTicks(1)} to simulate a key press because the client only uses {@code isPressed()} to process movement.
     */
    public void press() {
        if (this.minecraftBinding == null) return;
        InputUtil.Key key = InputUtil.fromTranslationKey(this.minecraftBinding.getBoundKeyTranslationKey());
        KeyBinding.onKeyPressed(key);
    }

    /**
     * Virtually holds the key triggering any Minecraft related events.
     */
    public void hold() {
        if (this.minecraftBinding == null) return;
        InputUtil.Key key = InputUtil.fromTranslationKey(this.minecraftBinding.getBoundKeyTranslationKey());
        KeyBinding.setKeyPressed(key, true);
    }

    /**
     * Virtually sets the holding state of the key.
     *
     * @param state The state to set
     */
    public void hold(boolean state) {
        if (this.minecraftBinding == null) return;
        if (state) this.hold();
        else this.release();
    }

    /**
     * Virtually hold this key for a specific number of ticks.
     *
     * @param numOfTicks The number of ticks to hold the key for
     */
    public void holdForTicks(int numOfTicks) {
        if (this.minecraftBinding == null) return;
        this.hold();
        TickHelper.scheduleOnce(this, this::release, numOfTicks);
    }

    /**
     * Virtually releases the key.
     */
    public void release() {
        if (this.minecraftBinding == null) return;
        InputUtil.Key key = InputUtil.fromTranslationKey(this.minecraftBinding.getBoundKeyTranslationKey());
        KeyBinding.setKeyPressed(key, false);
        TickHelper.unschedule(this);
    }

    /**
     * Virtually releases this key for a specific number of ticks.
     *
     * @param numOfTicks The number of ticks to release the key for
     */
    public void releaseForTicks(int numOfTicks) {
        if (this.minecraftBinding == null) return;
        this.release();
        TickHelper.scheduleOnce(this, this::hold, numOfTicks);
    }

    /**
     * The remaining ticks on the {@code holdForTicks()} or {@code releaseForTicks()}.
     *
     * @return The remaining ticks
     */
    public int ticksLeft() {
        return TickHelper.whenOnce(this);
    }

    /**
     * {@return whether the translation key name of both objects are identical}
     *
     * @param o the reference object with which to compare.
     */
    public boolean equals(Object o) {
        if (!(o instanceof MinecraftKeybind other) || this.minecraftBinding == null) return false;
        if (other.minecraftBinding == null) return false;
        return this.minecraftBinding.getBoundKeyTranslationKey().equals(other.minecraftBinding.getBoundKeyTranslationKey());
    }

    @Override
    public boolean isOf(int key, int scancode) {
        if (this.minecraftBinding == null) return false;
        return this.minecraftBinding.matchesKey(new KeyInput(key, scancode,0));
    }

    @Override
    public boolean isOfMouse(int button) {
        if (this.minecraftBinding == null) return false;
        return this.minecraftBinding.matchesMouse(new Click(0, 0, new MouseInput(button, 0)));
    }
}
