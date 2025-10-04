package dev.cigarette.helper.keybind;

import org.lwjgl.glfw.GLFW;

public class VirtualKeybind {
    protected int currentKey;
    protected final int defaultKey;
    protected boolean pressed = false;
    protected int timesPressed = 0;
    protected boolean isMouse = false;

    public VirtualKeybind(int defaultKey) {
        this.currentKey = defaultKey;
        this.defaultKey = defaultKey;
    }

    /**
     * Indicates that this keybind should receive events from the mouse instead of the keyboard.
     *
     * @return This object for method chaining
     */
    public VirtualKeybind asMouse() {
        this.isMouse = true;
        return this;
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

    public boolean isOf(int key, int scancode) {
        return this.currentKey == key;
    }

    public boolean isOfMouse(int button) {
        return this.currentKey == button;
    }
}
