package dev.cigarette.helper.keybind;

/**
 * A grouping of keybinds and event handlers for deciding when to block inputs or let events propagate.
 */
public class InputBlocker {
    /**
     * The keybindings blocked when this blocker is activated.
     */
    private final MinecraftKeybind[] blockedBindings;
    /**
     * Whether this blocker also prevents changes in player perspective by the mouse.
     */
    private boolean blockCamera = false;
    /**
     * Whether all keybindings attached have been linked with their native Minecraft {@code KeyBinding}.
     */
    protected boolean complete = false;

    /**
     * Creates a new input blocker.
     *
     * @param blockedBindings The keybinds to block
     */
    public InputBlocker(MinecraftKeybind... blockedBindings) {
        this.blockedBindings = blockedBindings;
    }

    /**
     * Prevents the user from changing their yaw and pitch.
     *
     * @return This object for method chaining
     */
    public InputBlocker preventCameraChanges() {
        this.blockCamera = true;
        return this;
    }

    /**
     * {@return whether this input blocker prevents perspective changes from the mouse}
     */
    public boolean blocksCamera() {
        return this.blockCamera;
    }

    /**
     * Verifies that each keybind object is linked with their native Minecraft {@code KeyBinding}.
     *
     * @return whether all keybinds are linked or not
     */
    private boolean complete() {
        if (this.complete) return true;
        int linked = 0;
        for (MinecraftKeybind binding : this.blockedBindings) {
            if (binding.isAttached() || binding.tryToAttach()) {
                linked++;
            }
        }
        this.complete = linked == this.blockedBindings.length;
        return this.complete;
    }

    /**
     * Processes a non-mouse key event through this blocker.
     *
     * @param key       The key of the event
     * @param scancode  The scancode of the event
     * @param action    The action performed on the key
     * @param modifiers The modifiers of the event
     * @return Whether the event should be considered handled and no longer propagate or not
     */
    public boolean processKey(int key, int scancode, int action, int modifiers) {
        if (!this.complete()) return false;
        for (MinecraftKeybind binding : blockedBindings) {
            if (binding.isMouse) continue;
            if (!binding.isOf(key, scancode)) continue;
            binding.physicalAction(action);
            return true;
        }
        return false;
    }

    /**
     * Processes a mouse key event through this blocker.
     *
     * @param button    The mouse button of the event
     * @param action    The action performed on the mouse button
     * @param modifiers The modifiers of the event
     * @return Whether the event should be considered handled and no longer propagate or not
     */
    public boolean processMouse(int button, int action, int modifiers) {
        if (!this.complete()) return false;
        for (MinecraftKeybind binding : blockedBindings) {
            if (!binding.isMouse) continue;
            if (!binding.isOfMouse(button)) continue;
            System.out.println("Processing Mouse | button=" + button + " action=" + action);
            binding.physicalAction(action);
            return true;
        }
        return false;
    }
}
