package dev.cigarette.helper.keybind;

public class InputBlocker {
    private final MinecraftKeybind[] blockedBindings;
    private boolean blockCamera = false;
    protected boolean complete = false;

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
