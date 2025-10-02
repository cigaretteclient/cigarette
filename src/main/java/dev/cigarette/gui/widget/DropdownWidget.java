package dev.cigarette.gui.widget;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.gui.Scissor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

public class DropdownWidget<Widget extends BaseWidget<?>, StateType>
        extends PassthroughWidget<BaseWidget<?>, BaseWidget.Stateless> {
    /**
     * The heading widget.
     */
    protected Widget header;
    /**
     * The child widgets inside the dropdown menu.
     */
    protected ScrollableWidget<BaseWidget<?>> container;
    /**
     * Whether the child widgets in this dropdown should be visible.
     */
    private boolean dropdownVisible = false;
    /**
     * Whether a dropdown indicator should be rendered.
     */
    private boolean dropdownIndicator = true;

    /**
     * The time at which the rotation started.
     */
    private long rotateStartMillis = 0L;
    /**
     * The current rotation of the dropdown indicator.
     */
    private double rotateAngleRad = 0.0;

    /**
     * The current rotation of the dropdown indicator whilst the dropdown is open.
     */
    private double rotateOffsetRad = 0.0;
    /**
     *
     */
    private static final int ROTATION_PERIOD_MS = 2000;

    /**
     * Whether the dropdown menu is currently animating.
     */
    private boolean animating = false;
    /**
     * Whether the dropdown menu is in progress of opening.
     */
    private boolean opening = false;
    /**
     * The time at which the animation started.
     */
    private long animStartMillis = 0L;
    /**
     * The maximum animation runtime on opening/closing the dropdown.
     */
    private static final int TOGGLE_ANIM_MS = 220;

    /**
     * Creates a widget that can expand like a dropdown menu to show child widgets.
     *
     * @param message The text to display inside this widget
     * @param tooltip The tooltip to render when this widget is hovered
     */
    public DropdownWidget(String message, @Nullable String tooltip) {
        super(message, tooltip);
        this.withDefault(new BaseWidget.Stateless());
        this.container = new ScrollableWidget<>(0, 0, false);
        super.children.put("0", this.container);
    }

    /**
     * Sets this widgets header widget. The dropdown menu opens when this widget is right-clicked and this widget is always visible.
     *
     * @param header The widget to use as the header
     * @return This widget for method chaining
     */
    public DropdownWidget<Widget, StateType> setHeader(Widget header) {
        this.header = header;
        return this;
    }

    /**
     * Sets this widget's children. These widgets will become visible when the dropdown menu is opened.
     *
     * @param children The children to attach
     * @return This widget for method chaining
     */
    public DropdownWidget<Widget, StateType> setChildren(@Nullable BaseWidget<?>... children) {
        this.container.setChildren(children);
        return this;
    }

    /**
     * Sets whether this widget should render a dropdown indicator over the heading widget.
     *
     * @param indicator Whether the indicator should be rendered
     * @return This widget for method chaining
     */
    public DropdownWidget<Widget, StateType> withIndicator(boolean indicator) {
        this.dropdownIndicator = indicator;
        return this;
    }

    @Override
    public void registerConfigKey(String key) {
        this.header.registerConfigKey(key);
    }

    @Override
    public void unfocus() {
        if (this.header != null)
            this.header.unfocus();
        this.setFocused(false);
        this.dropdownVisible = false;
        this.container.setFocused(false);
        this.container.setExpanded(false);
        super.unfocus();
    }

    @Override
    public void alphabetic() {
        this.container.alphabetic();
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (this.header != null)
            this.header.mouseMoved(mouseX, mouseY);
        if (dropdownVisible)
            super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            this.setFocused();
            if (this.header == null)
                return false;
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.header.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                boolean target = !children.isEmpty() && !dropdownVisible;

                if (target != dropdownVisible) {
                    this.animating = true;
                    this.opening = target;
                    this.animStartMillis = System.currentTimeMillis();
                }
                dropdownVisible = target;
            }
            return true;
        }
        boolean captured = dropdownVisible && super.mouseClicked(mouseX, mouseY, button);
        this.setFocused(captured);
        this.dropdownVisible = captured;
        return captured;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.header != null)
            this.header.mouseReleased(mouseX, mouseY, button);
        super.mouseReleased(mouseX, mouseY, button);
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.header != null)
            this.header.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return dropdownVisible && super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    protected void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left,
                          int top, int right, int bottom) {
        if (this.container == null)
            return;
        if (this.container.focused || this.dropdownVisible) {
            this.container.setExpanded(true);
        } else {
            this.container.setExpanded(false);
        }
        if (this.header == null)
            return;
        this.header.withXY(left, top).withWH(width, height).renderWidget(context, mouseX, mouseY, deltaTicks);
        if (this.container.expanded) {
            long now = System.currentTimeMillis();
            if (rotateStartMillis == 0L) {
                rotateStartMillis = now;
            }
            long elapsed = now - rotateStartMillis;
            double frac = (elapsed % ROTATION_PERIOD_MS) / (double) ROTATION_PERIOD_MS;

            rotateAngleRad = rotateOffsetRad + frac * 2.0 * Math.PI;
            rotateAngleRad = rotateAngleRad % (2.0 * Math.PI);
            if (rotateAngleRad < 0)
                rotateAngleRad += 2.0 * Math.PI;
        } else {
            rotateOffsetRad = rotateAngleRad % (2.0 * Math.PI);
            if (rotateOffsetRad < 0)
                rotateOffsetRad += 2.0 * Math.PI;
            rotateStartMillis = 0L;

            rotateAngleRad = rotateOffsetRad;
        }

        double toggleProgress = dropdownVisible ? 1.0 : 0.0;
        if (animating) {
            long now = System.currentTimeMillis();
            double elapsed = (now - animStartMillis) / (double) TOGGLE_ANIM_MS;
            double t = Math.max(0.0, Math.min(1.0, elapsed));

            double eased = 1 - Math.pow(1 - t, 3);
            toggleProgress = opening ? eased : (1.0 - eased);
            if (t >= 1.0) {
                animating = false;
            }
        }

        if (this.container.children.isEmpty())
            return;
        if (dropdownIndicator) {
            int w = 10;
            int h = 10;
            int iconX = right - 12;
            int iconY = top + (height / 2) - (h / 2);

            int angleDeg = (int) Math.round(Math.toDegrees(rotateAngleRad));
            cigaretteOnlyAt(context, iconX, iconY, w, h, angleDeg);
        }

        if (toggleProgress <= 0.001 || !this.focused)
            return;

        double easedProgress = CigaretteScreen.easeOut(toggleProgress);
        final float MAX_TRANSLATE_PX = 0.0f;
        float translateY = (float) ((1.0 - easedProgress) * MAX_TRANSLATE_PX);
        int animOffset = Math.round(translateY);

        int containerW = this.container.getWidth();
        int containerH = this.container.getHeight();

        int visibleHeight = Math.max(0, Math.min(containerH, (int) Math.round(containerH * easedProgress)));
        if (visibleHeight > 0) {
            int scissorLeft = right;
            int scissorTop = top + animOffset;
            int scissorRight = right + containerW;
            int scissorBottom = scissorTop + visibleHeight;

            context.getMatrices().push();
            Scissor.pushExclusive(context, scissorLeft, scissorTop, scissorRight, scissorBottom);

            this.container.withXY(right + childLeftOffset, top + animOffset)
                    .withWH(containerW, containerH)
                    .renderWidget(context, mouseX, mouseY, deltaTicks);

            Scissor.popExclusive();
            context.getMatrices().pop();
        }
    }

    /**
     * Render the clients logo at a specific location and rotation. Used as the dropdown indicator.
     *
     * @param context The draw context to draw on
     * @param x The X position to draw at
     * @param y The Y position to draw at
     * @param w The width of the texture
     * @param h The height of the texture
     * @param angle The rotation of the texture
     */
    public static void cigaretteOnlyAt(DrawContext context, int x, int y, int w, int h, int angle) {
        context.getMatrices().push();
        float cx = x + w / 2f;
        float cy = y + h / 2f;
        context.getMatrices().translate(cx, cy, 0);
        context.getMatrices().multiply(new Quaternionf().rotateZ((float) Math.toRadians(angle)));
        context.drawTexture(
                RenderLayer::getGuiTextured,
                Cigarette.LOGO_IDENTIFIER,
                Math.round(-w / 2f), Math.round(-h / 2f),
                0f, 0f, w, h, w, h);
        context.getMatrices().pop();
    }
}
