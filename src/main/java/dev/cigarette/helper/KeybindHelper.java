package dev.cigarette.helper;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.CigaretteScreen;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;

public class KeybindHelper {
    /**
     * Keybind to toggle {@code CigaretteScreen}.
     */
    public static final CigaretteKeyBind TOGGLE_GUI = new CigaretteKeyBind(GLFW.GLFW_KEY_RIGHT_SHIFT);

    private static final HashSet<CigaretteKeyBind> customBinds = new HashSet<>();

    /**
     * Attempts to handle a key event inside the {@code CigaretteScreen} GUI.
     * <p>{@code KeyboardMixin} checks this handler first.</p>
     *
     * @param client    The Minecraft client
     * @param key       The events key code
     * @param scancode  The events scan code
     * @param action    The events GLFW action
     * @param modifiers The events modifier
     * @return Whether the key event is handled and should be cancelled
     */
    public static boolean handleByGUI(MinecraftClient client, int key, int scancode, int action, int modifiers) {
        if (!(client.currentScreen instanceof CigaretteScreen)) return false;
        if (CigaretteScreen.bindingKey != null) {
            switch (action) {
                case GLFW.GLFW_PRESS -> CigaretteScreen.bindingKey.keyPressed(key, scancode, modifiers);
                case GLFW.GLFW_RELEASE -> CigaretteScreen.bindingKey.keyReleased(key, scancode, modifiers);
            }
        } else {
            if (key == GLFW.GLFW_KEY_ESCAPE || (action == GLFW.GLFW_PRESS && TOGGLE_GUI.matches(key))) {
                Cigarette.SCREEN.close();
            }
        }
        return true;
    }

    /**
     * Checks if the input should be cancelled because of modules blocking inputs.
     * <p>{@code KeyboardMixin} checks this handler second, after {@code handleByGUI()}.</p>
     *
     * @param client    The Minecraft client
     * @param key       The events key code
     * @param scancode  The events scan code
     * @param action    The events GLFW action
     * @param modifiers The events modifier
     * @return Whether the key event should be cancelled
     */
    public static boolean handleBlockedInputs(MinecraftClient client, int key, int scancode, int action, int modifiers) {
        if (TOGGLE_GUI.matches(key)) return false;
        return false;
    }

    /**
     * Attempts to handle a key event outside the {@code CigaretteScreen} GUI and after input blocking.
     * <p>{@code KeyboardMixin} checks this handler last, after {@code handleBlockedInputs()}.</p>
     *
     * @param client    The Minecraft client
     * @param key       The events key code
     * @param scancode  The events scan code
     * @param action    The events GLFW action
     * @param modifiers The events modifier
     * @return Whether the key event is handled and should be cancelled
     */
    public static boolean handleCustom(MinecraftClient client, int key, int scancode, int action, int modifiers) {
        if (action != GLFW.GLFW_PRESS) return false;
        if (TOGGLE_GUI.matches(key)) {
            Cigarette.SCREEN.setParent(client.currentScreen);
            client.setScreen(Cigarette.SCREEN);
            return true;
        }
        for (CigaretteKeyBind binding : customBinds) {
            if (!binding.matches(key)) continue;
            binding.processAction(action);
            return true;
        }
        return false;
    }

    /**
     * A custom key bind used instead of the native {@code KeyBinding} class.
     */
    public static class CigaretteKeyBind {
        /**
         * The key code that triggers this keybind.
         */
        private int key;
        /**
         * The default key code set for this keybind.
         */
        private final int defaultKey;
        /**
         * Whether this key is currently pressed or not.
         */
        private boolean pressed = false;
        /**
         * The number of times this key has been pressed.
         */
        private int timesPressed = 0;

        /**
         * Creates a custom key bind.
         *
         * @param defaultKey The key and default key to use
         */
        public CigaretteKeyBind(int defaultKey) {
            this.key = defaultKey;
            this.defaultKey = defaultKey;
            customBinds.add(this);
        }

        /**
         * Updates {@code pressed} and {@code timesPressed} depending on the action that occurred on the key.
         *
         * @param glfwAction The key events action as defined by GLFW
         */
        protected void processAction(int glfwAction) {
            switch (glfwAction) {
                case GLFW.GLFW_PRESS -> {
                    this.timesPressed++;
                    this.pressed = true;
                }
                case GLFW.GLFW_RELEASE -> this.pressed = false;
            }
        }

        /**
         * {@return whether this keybind is currently pressed}
         */
        public boolean isPressed() {
            return this.pressed;
        }

        /**
         * {@return whether this keybind was pressed}
         */
        public boolean wasPressed() {
            if (this.timesPressed == 0) return false;
            this.timesPressed--;
            return true;
        }

        /**
         * {@return whether the input key matches the saved key code}
         *
         * @param key The key code to compare
         */
        public boolean matches(int key) {
            return this.key == key;
        }

        /**
         * {@return the key code that should trigger this key bind}
         */
        public int key() {
            return this.key;
        }

        /**
         * Sets the saved key.
         *
         * @param key The key code to save to this key bind
         */
        public void setKey(int key) {
            this.key = key;
        }

        /**
         * Resets this key bind to the default key.
         */
        public void reset() {
            this.key = this.defaultKey;
        }
    }
}
