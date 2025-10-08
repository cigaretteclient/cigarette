package dev.cigarette.helper.keybind;

import org.lwjgl.glfw.GLFW;

/**
 * A fake {@code KeyBinding} which handles the physical state of a key.
 */
public class VirtualKeybind {
    /**
     * The key currently bounded to.
     */
    protected int currentKey;
    /**
     * The original key that was bounded to.
     */
    protected final int defaultKey;
    /**
     * Whether the bounded key is physically pressed.
     */
    protected boolean pressed = false;
    /**
     * Times the bounded key was pressed.
     */
    protected int timesPressed = 0;
    /**
     * Whether this is a mouse keybind or not.
     */
    protected boolean isMouse = false;

    /**
     * Creates a virtual keybind. Does not create any {@code KeyBinding}'s or register to any Minecraft events.
     *
     * @param defaultKey The keycode of the key to monitor
     */
    public VirtualKeybind(int defaultKey) {
        this.currentKey = defaultKey;
        this.defaultKey = defaultKey;
    }

    /**
     * Creates a virtual keybind. Does not create any {@code KeyBinding}'s or register to any Minecraft events.
     *
     * @param defaultKey The keycode of the key to monitor
     * @param isMouse    Whether this is a mouse keybind or not
     */
    public VirtualKeybind(int defaultKey, boolean isMouse) {
        this.currentKey = defaultKey;
        this.defaultKey = defaultKey;
        this.isMouse = isMouse;
    }

    /**
     * {@return whether this a mouse keybind or not}
     */
    public boolean isMouse() {
        return this.isMouse;
    }

    /**
     * Processes a physical action on the key.
     *
     * @param glfwAction The GLFW action performed on this keybind
     */
    public void physicalAction(int glfwAction) {
        switch (glfwAction) {
            case GLFW.GLFW_PRESS -> {
                this.timesPressed++;
                this.pressed = true;
            }
            case GLFW.GLFW_RELEASE -> this.pressed = false;
        }
    }

    /**
     * {@return whether this key is being physically held down or not}
     */
    public boolean isPhysicallyPressed() {
        return this.pressed;
    }

    /**
     * {@return whether this key was physically pressed or not}
     */
    public boolean wasPhysicallyPressed() {
        if (this.timesPressed == 0) return false;
        this.timesPressed--;
        return true;
    }

    /**
     * {@return if the key is bounded to this keybind}
     *
     * @param key      The key to check if bounded
     * @param scancode The scancode for misc {@code GLFW_KEY_UNKNOWN} keys
     */
    public boolean isOf(int key, int scancode) {
        return !this.isMouse && this.currentKey == key;
    }

    /**
     * {@return if mouse button is bounded to this keybind}
     *
     * @param button The mouse button to check if bounded
     */
    public boolean isOfMouse(int button) {
        return this.isMouse && this.currentKey == button;
    }
}
