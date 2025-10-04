package dev.cigarette.helper;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.helper.keybind.InputBlocker;
import dev.cigarette.helper.keybind.MinecraftKeybind;
import dev.cigarette.helper.keybind.VirtualKeybind;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;

public class KeybindHelper {
    public static final MinecraftKeybind KEY_SNEAK = new MinecraftKeybind("key.sneak");
    public static final MinecraftKeybind KEY_MOVE_BACK = new MinecraftKeybind("key.back");
    public static final MinecraftKeybind KEY_MOVE_LEFT = new MinecraftKeybind("key.left");
    public static final MinecraftKeybind KEY_MOVE_RIGHT = new MinecraftKeybind("key.right");
    public static final MinecraftKeybind KEY_RIGHT_CLICK = new MinecraftKeybind("key.use");
    public static final MinecraftKeybind KEY_JUMP = new MinecraftKeybind("key.jump");
    /**
     * Keybind to toggle {@code CigaretteScreen}.
     */
    public static final VirtualKeybind TOGGLE_GUI = new VirtualKeybind(GLFW.GLFW_KEY_RIGHT_SHIFT);

    /**
     * Set of custom keybinds that can be triggered.
     */
    private static final HashSet<VirtualKeybind> customBinds = new HashSet<>();

    /**
     * The module that is blocking inputs.
     */
    private static Object blockingModule = null;

    /**
     * The input blocker to pass events through.
     */
    private static @Nullable InputBlocker blockedInputs = null;

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
            if (key == GLFW.GLFW_KEY_ESCAPE || (action == GLFW.GLFW_PRESS && TOGGLE_GUI.isOf(key, scancode))) {
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
        if (TOGGLE_GUI.isOf(key, scancode)) return false;
        if (blockedInputs == null) return false;
        return blockedInputs.process(key, scancode, action, modifiers);
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
        if (TOGGLE_GUI.isOf(key, scancode)) {
            Cigarette.SCREEN.setParent(client.currentScreen);
            client.setScreen(Cigarette.SCREEN);
            return true;
        }
        for (VirtualKeybind binding : customBinds) {
            if (!binding.isOf(key, scancode)) continue;
            binding.physicalAction(action);
            return true;
        }
        return false;
    }

    /**
     * Attempts to start input blocking. {@return false if input blocking is already running} Use {@code forceBlockInputs()} to bypass this check.
     *
     * @param module
     * @param blocker
     */
    public static boolean tryBlockInputs(Object module, InputBlocker blocker) {
        if (blockedInputs != null) return false;
        forceBlockInputs(module, blocker);
        return true;
    }

    /**
     * Force starts input blocking with the provided configuration. Overrides any previously started configurations.
     *
     * @param module
     * @param blocker
     */
    public static void forceBlockInputs(Object module, InputBlocker blocker) {
        blockedInputs = blocker;
        blockingModule = module;
    }

    /**
     * Disable input blocking.
     */
    public static void unblock() {
        blockedInputs = null;
        blockingModule = null;
    }

    /**
     * {@return whether the provided module is blocking inputs}
     *
     * @param module The module to check
     */
    public static boolean isBlocking(Object module) {
        return blockingModule == module;
    }
}
