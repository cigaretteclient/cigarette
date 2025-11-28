package dev.cigarette.helper;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.helper.keybind.InputBlocker;
import dev.cigarette.helper.keybind.MinecraftKeybind;
import dev.cigarette.helper.keybind.VirtualKeybind;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;

/**
 * Helper class for handling the physical and virtual state of keybinds. Also supports input blocking via {@code InputBlocker}.
 */
public class KeybindHelper {
    public static final MinecraftKeybind KEY_SPRINT = new MinecraftKeybind("key.sprint");
    public static final MinecraftKeybind KEY_SNEAK = new MinecraftKeybind("key.sneak");
    public static final MinecraftKeybind KEY_MOVE_FORWARD = new MinecraftKeybind("key.forward");
    public static final MinecraftKeybind KEY_MOVE_BACK = new MinecraftKeybind("key.back");
    public static final MinecraftKeybind KEY_MOVE_LEFT = new MinecraftKeybind("key.left");
    public static final MinecraftKeybind KEY_MOVE_RIGHT = new MinecraftKeybind("key.right");
    public static final MinecraftKeybind KEY_USE_ITEM = new MinecraftKeybind("key.use", true);
    public static final MinecraftKeybind KEY_ATTACK = new MinecraftKeybind("key.attack", true);
    public static final MinecraftKeybind KEY_PICK_ITEM = new MinecraftKeybind("key.pickItem", true);
    public static final MinecraftKeybind KEY_JUMP = new MinecraftKeybind("key.jump");
    public static final MinecraftKeybind KEY_DROP_ITEM = new MinecraftKeybind("key.drop");
    public static final MinecraftKeybind KEY_TOGGLE_INVENTORY = new MinecraftKeybind("key.inventory");
    public static final MinecraftKeybind KEY_SLOT_1 = new MinecraftKeybind("key.hotbar.1");
    public static final MinecraftKeybind KEY_SLOT_2 = new MinecraftKeybind("key.hotbar.2");
    public static final MinecraftKeybind KEY_SLOT_3 = new MinecraftKeybind("key.hotbar.3");
    public static final MinecraftKeybind KEY_SLOT_4 = new MinecraftKeybind("key.hotbar.4");
    public static final MinecraftKeybind KEY_SLOT_5 = new MinecraftKeybind("key.hotbar.5");
    public static final MinecraftKeybind KEY_SLOT_6 = new MinecraftKeybind("key.hotbar.6");
    public static final MinecraftKeybind KEY_SLOT_7 = new MinecraftKeybind("key.hotbar.7");
    public static final MinecraftKeybind KEY_SLOT_8 = new MinecraftKeybind("key.hotbar.8");
    public static final MinecraftKeybind KEY_SLOT_9 = new MinecraftKeybind("key.hotbar.9");

    /**
     * Set of bindings that wrap native Minecraft {@code KeyBinding}'s.
     */
    private static final HashSet<MinecraftKeybind> wrappedBindings = new HashSet<>();

    /**
     * Keybind to toggle {@code CigaretteScreen}.
     */
    public static final VirtualKeybind KEY_TOGGLE_GUI = new VirtualKeybind(GLFW.GLFW_KEY_RIGHT_SHIFT);

    /**
     * Set of custom keybinds that can be triggered.
     */
    private static final HashSet<VirtualKeybind> customBinds = new HashSet<>();

    static {
        wrappedBindings.add(KEY_SPRINT);
        wrappedBindings.add(KEY_SNEAK);
        wrappedBindings.add(KEY_MOVE_FORWARD);
        wrappedBindings.add(KEY_MOVE_BACK);
        wrappedBindings.add(KEY_MOVE_LEFT);
        wrappedBindings.add(KEY_MOVE_RIGHT);
        wrappedBindings.add(KEY_USE_ITEM);
        wrappedBindings.add(KEY_ATTACK);
        wrappedBindings.add(KEY_PICK_ITEM);
        wrappedBindings.add(KEY_JUMP);
        wrappedBindings.add(KEY_DROP_ITEM);
        wrappedBindings.add(KEY_TOGGLE_INVENTORY);
        wrappedBindings.add(KEY_SLOT_1);
        wrappedBindings.add(KEY_SLOT_2);
        wrappedBindings.add(KEY_SLOT_3);
        wrappedBindings.add(KEY_SLOT_4);
        wrappedBindings.add(KEY_SLOT_5);
        wrappedBindings.add(KEY_SLOT_6);
        wrappedBindings.add(KEY_SLOT_7);
        wrappedBindings.add(KEY_SLOT_8);
        wrappedBindings.add(KEY_SLOT_9);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (VirtualKeybind bind : customBinds) {
                bind.unset();
            }
        });
    }

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
    public static boolean handleKeyByGUI(MinecraftClient client, int key, int scancode, int action, int modifiers) {
        if (!(client.currentScreen instanceof CigaretteScreen)) return false;
        if (CigaretteScreen.bindingKey != null) {
            switch (action) {
                case GLFW.GLFW_PRESS -> CigaretteScreen.bindingKey.keyPressed(key, scancode, modifiers);
                case GLFW.GLFW_RELEASE -> CigaretteScreen.bindingKey.keyReleased(key, scancode, modifiers);
            }
        } else {
            if (action == GLFW.GLFW_PRESS && (key == GLFW.GLFW_KEY_ESCAPE || KEY_TOGGLE_GUI.isOf(key, scancode))) {
                Cigarette.SCREEN.close();
            }
        }
        return true;
    }

    /**
     * Checks if the input should be cancelled because of modules blocking inputs.
     * <p>{@code KeyboardMixin} checks this handler second, after {@code handleKeyByGUI()}.</p>
     *
     * @param client    The Minecraft client
     * @param key       The events key code
     * @param scancode  The events scan code
     * @param action    The events GLFW action
     * @param modifiers The events modifier
     * @return Whether the key event should be cancelled
     */
    public static boolean handleBlockedKeyInputs(MinecraftClient client, int key, int scancode, int action, int modifiers) {
        if (KEY_TOGGLE_GUI.isOf(key, scancode)) return false;
        if (blockedInputs == null) return false;
        return blockedInputs.processKey(key, scancode, action, modifiers);
    }

    /**
     * Checks if the input should be cancelled because of modules blocking inputs.
     * <p>{@code MouseMixin} checks this handler first.</p>
     *
     * @param client The Minecraft client
     * @param button The buttons key code
     * @param action The events GLFW action
     * @param mods   The events modifier
     * @return Whether the mouse event is handled and should be cancelled
     */
    public static boolean handleBlockedMouseInputs(MinecraftClient client, int button, int action, int mods) {
        if (blockedInputs == null) return false;
        return blockedInputs.processMouse(button, action, mods);
    }

    /**
     * Attempts to handle a key event outside the {@code CigaretteScreen} GUI and after input blocking.
     * <p>{@code KeyboardMixin} checks this handler last, after {@code handleBlockedKeyInputs()}.</p>
     *
     * @param client    The Minecraft client
     * @param key       The events key code
     * @param scancode  The events scan code
     * @param action    The events GLFW action
     * @param modifiers The events modifier
     * @return Whether the key event is handled and should be cancelled
     */
    public static boolean handleCustomKeys(MinecraftClient client, int key, int scancode, int action, int modifiers) {
        for (MinecraftKeybind keybind : wrappedBindings) {
            if (!keybind.isAttached() && !keybind.tryToAttach()) continue;
            if (keybind.isMouse()) continue;
            if (!keybind.isOf(key, scancode)) continue;
            keybind.physicalAction(action);
        }
        if (action == GLFW.GLFW_PRESS && KEY_TOGGLE_GUI.isOf(key, scancode)) {
            Cigarette.SCREEN.setParent(client.currentScreen);
            client.setScreen(Cigarette.SCREEN);
            return true;
        }
        for (VirtualKeybind binding : customBinds) {
            if (binding.isMouse()) continue;
            if (!binding.isOf(key, scancode)) continue;
            binding.physicalAction(action);
            return true;
        }
        return false;
    }

    /**
     * Attempts to handle a mouse event outside the {@code CigaretteScreen} GUI and after input blocking.
     * <p>{@code MouseMixin} checks this handler last, after {@code handleBlocksMouseInputs()}</p>
     *
     * @param client The Minecraft client
     * @param button The buttons key code
     * @param action The events GLFW action
     * @param mods   The events modifier
     * @return Whether the mouse event is handled and should be cancelled
     */
    public static boolean handleCustomMouse(MinecraftClient client, int button, int action, int mods) {
        for (MinecraftKeybind keybind : wrappedBindings) {
            if (!keybind.isAttached() && !keybind.tryToAttach()) continue;
            if (!keybind.isMouse()) continue;
            if (!keybind.isOfMouse(button)) continue;
            keybind.physicalAction(action);
        }
        for (VirtualKeybind binding : customBinds) {
            if (!binding.isMouse()) continue;
            if (!binding.isOfMouse(button)) continue;
            binding.physicalAction(action);
            return true;
        }
        return false;
    }

    /**
     * Register a {@code MinecraftKeybind} wrapper so it can receive physical events. Wrappers from {@code KeybindHelper} are already registered.
     * <p>Required to use {@code isPhysicallyPressed()} and {@code wasPhysicallyPressed()}.</p>
     *
     * @param keybind The keybind wrapper to register
     */
    public static void registerWrapper(MinecraftKeybind keybind) {
        for (MinecraftKeybind existing : wrappedBindings) {
            if (existing.equals(keybind)) return;
        }
        wrappedBindings.add(keybind);
    }

    /**
     * Register a {@code VirtualKeybind} so it can receive physical events.
     *
     * @param keybind The keybind to register
     */
    public static void registerVirtualKey(VirtualKeybind keybind) {
        customBinds.add(keybind);
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
        for (MinecraftKeybind keybind : wrappedBindings) {
            keybind.release();
        }
        updateKeyStates();
    }

    /**
     * {@return whether the provided module is blocking inputs}
     *
     * @param module The module to check
     */
    public static boolean isBlocking(Object module) {
        return blockingModule == module;
    }

    /**
     * {@return whether an input blocker is applied that blocks mouse movement or not}
     */
    public static boolean isMouseBlocked() {
        return blockedInputs != null && blockedInputs.blocksCamera();
    }

    /**
     * Reset the state of all key and mouse bindings to the last physical state reported by GLFW. Wraps {@code KeyBinding.updatePressedStates()} as that method does not reset mouse buttons.
     */
    public static void updateKeyStates() {
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        KeyBinding.updatePressedStates();

        int[] buttons = {GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_MOUSE_BUTTON_MIDDLE, GLFW.GLFW_MOUSE_BUTTON_RIGHT};
        for (int j : buttons) {
            InputUtil.Key button = InputUtil.Type.MOUSE.createFromCode(j);
            KeyBinding.setKeyPressed(button, GLFW.glfwGetMouseButton(window, j) == GLFW.GLFW_PRESS);
        }
    }
}
