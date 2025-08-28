package dev.cigarette.gui.widget;

import net.minecraft.client.gui.Element;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Extends the passthrough widget which holds children widgets and automatically forwards events and rendering to them.
 *
 * @param <ChildType> The type of children this widget stores. Use {@code Widget extends BaseWidget<?>} to allow any types as children.
 * @param <StateType> The custom state this widget stores. Use {@code BaseWidget.Stateless} for widgets that should not hold state.
 */
public abstract class PassthroughWidget<ChildType extends BaseWidget<?>, StateType> extends BaseWidget<StateType> {
    /**
     * The children that this widget is the parent of, mapped by String to the actual child reference for sorting capabilities.
     * <p>There is no default methods for adding children as defined in {@code PassthroughWidget}, so check the subclasses for implementation details.</p>
     */
    protected Map<String, ChildType> children = new LinkedHashMap<>();
    /**
     * Left offsetting for dropdown continuation for widgets that may render scrollbars or have inconsistent sizing.
     */
    protected int childLeftOffset = 0;

    /**
     * Creates the super class.
     *
     * @param x       The initial X position of this widget
     * @param y       The initial Y position of this widget
     * @param width   The initial width of this widget
     * @param height  The initial height of this widget
     * @param message The text to display inside this widget
     */
    public PassthroughWidget(int x, int y, int width, int height, String message) {
        super(message, null);
        this.withXY(x, y).withWH(width, height);
    }

    /**
     * Creates the super class.
     *
     * @param message The text to display inside this widget
     * @param tooltip The tooltip to render when this widget is hovered
     */
    public PassthroughWidget(String message, @Nullable String tooltip) {
        super(message, tooltip);
    }

    /**
     * Creates the super class.
     *
     * @param message The text to display inside this widget
     */
    public PassthroughWidget(String message) {
        super(message, null);
    }

    /**
     * Switches the {@code Map} type of {@code children} to a {@code TreeMap} which automatically sorts by the {@code String} keys.
     */
    public void alphabetic() {
        this.children = new TreeMap<>(this.children);
    }

    /**
     * Sets this widget and all of its children to be unfocused.
     */
    @Override
    public void unfocus() {
        for (BaseWidget<?> child : children.values()) {
            if (child == null) continue;
            child.unfocus();
        }
    }

    /**
     * Forwards mouse move events to all the children of this widget.
     *
     * @param mouseX the X coordinate of the mouse
     * @param mouseY the Y coordinate of the mouse
     */
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (children.isEmpty()) return;
        for (Element child : children.values()) {
            if (child == null) continue;
            child.mouseMoved(mouseX, mouseY);
        }
    }

    /**
     * Forwards mouse clicked events to all the children of this widget.
     * <p>The first child that handles the event will be focused and all subsequent children will be unfocused and will not receive the event.</p>
     *
     * @param mouseX the X coordinate of the mouse
     * @param mouseY the Y coordinate of the mouse
     * @param button the mouse button number
     * @return Whether a child handled the click
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (children.isEmpty()) return false;
        boolean wasHandled = false;
        for (BaseWidget<?> child : children.values()) {
            if (child == null) continue;
            if (wasHandled) {
                child.unfocus();
                continue;
            }
            wasHandled = child.mouseClicked(mouseX, mouseY, button);
            if (!wasHandled) child.unfocus();
            else child.setFocused();
        }
        return wasHandled;
    }

    /**
     * Forwards mouse released events to all the children of this widget.
     *
     * @param mouseX the X coordinate of the mouse
     * @param mouseY the Y coordinate of the mouse
     * @param button the mouse button number
     * @return {@code false}
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (children.isEmpty()) return false;
        for (Element child : children.values()) {
            if (child == null) continue;
            child.mouseReleased(mouseX, mouseY, button);
        }
        return false;
    }

    /**
     * Forwards mouse dragged events to all the children of this widget.
     *
     * @param mouseX the current X coordinate of the mouse
     * @param mouseY the current Y coordinate of the mouse
     * @param button the mouse button number
     * @param deltaX the difference of the current X with the previous X coordinate
     * @param deltaY the difference of the current Y with the previous Y coordinate
     * @return {@code false}
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (children.isEmpty()) return false;
        for (Element child : children.values()) {
            if (child == null) continue;
            child.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return false;
    }

    /**
     * Forwards mouse scrolled events to all the children of this widget.
     * <p>This returns after the first child that handles the event.</p>
     *
     * @param mouseX           the X coordinate of the mouse
     * @param mouseY           the Y coordinate of the mouse
     * @param horizontalAmount the horizontal scroll amount
     * @param verticalAmount   the vertical scroll amount
     * @return Whether a child handled the scroll
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (children.isEmpty()) return false;
        for (Element child : children.values()) {
            if (child == null) continue;
            boolean handled = child.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            if (handled) return true;
        }
        return false;
    }

    /**
     * Forwards key pressed events to all the children of this widget.
     * <p>This returns after the first child that handles the event.</p>
     *
     * @param keyCode   the named key code of the event as described in the {@link org.lwjgl.glfw.GLFW GLFW} class
     * @param scanCode  the unique/platform-specific scan code of the keyboard input
     * @param modifiers a GLFW bitfield describing the modifier keys that are held down (see <a href="https://www.glfw.org/docs/3.3/group__mods.html">GLFW Modifier key flags</a>)
     * @return Whether a child handled the key press
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (children.isEmpty()) return false;
        for (Element child : children.values()) {
            if (child == null) continue;
            boolean handled = child.keyPressed(keyCode, scanCode, modifiers);
            if (handled) return true;
        }
        return false;
    }

    /**
     * Forwards the key released events to all the children of this widget.
     * <p>This returns after the first child that handles the event.</p>
     *
     * @param keyCode   the named key code of the event as described in the {@link org.lwjgl.glfw.GLFW GLFW} class
     * @param scanCode  the unique/platform-specific scan code of the keyboard input
     * @param modifiers a GLFW bitfield describing the modifier keys that are held down (see <a href="https://www.glfw.org/docs/3.3/group__mods.html">GLFW Modifier key flags</a>)
     * @return Whether a child handled the key release
     */
    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (children.isEmpty()) return false;
        for (Element child : children.values()) {
            if (child == null) continue;
            boolean handled = child.keyReleased(keyCode, scanCode, modifiers);
            if (handled) return true;
        }
        return false;
    }

    /**
     * Forwards the char typed events to all the children of this widget.
     * <p>This returns after the first child that handles the event.</p>
     *
     * @param chr       the captured character
     * @param modifiers a GLFW bitfield describing the modifier keys that are held down (see <a href="https://www.glfw.org/docs/3.3/group__mods.html">GLFW Modifier key flags</a>)
     * @return Whether a child handled the char type
     */
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (children.isEmpty()) return false;
        for (Element child : children.values()) {
            if (child == null) continue;
            boolean handled = child.charTyped(chr, modifiers);
            if (handled) return true;
        }
        return false;
    }
}
