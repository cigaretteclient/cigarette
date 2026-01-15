package dev.cigarette.gui.widget;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.gui.ColorScheme;
import dev.cigarette.gui.GradientRenderer;
import dev.cigarette.lib.Color;
import dev.cigarette.lib.Shape;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * A widget that can be dragged around the screen.
 */
public class DraggableWidget extends BaseWidget<BaseWidget.Stateless> {
    public interface DragCallback {
        void updateParentPosition(int newX, int newY, int deltaX, int deltaY);
    }

    public interface ClickCallback {
        void onClick(double mouseX, double mouseY, int button);
    }

    /**
     * Whether this widget is actively being dragged by the user.
     */
    private boolean dragging = false;
    /**
     * The starting X position of the drag on screen.
     */
    private int startingX = 0;
    /**
     * The starting Y position of the drag on screen.
     */
    private int startingY = 0;
    /**
     * The starting mouse X position that initiated the drag.
     */
    private double startingMouseX = 0;
    /**
     * The starting mouse Y position that initiated the drag.
     */
    private double startingMouseY = 0;
    /**
     * Callback triggered when this widget is moved as a result of a drag.
     */
    private @Nullable DragCallback dragCallback = null;
    /**
     * Callback triggered when this widget is right-clicked.
     */
    private @Nullable ClickCallback clickCallback = null;
    /**
     * If this widget is responsible for a {@link ScrollableWidget}'s visibility, this signals whether that widget is collapsed. Used for rounding the bottom corners of this widget when collapsed.
     */
    public boolean expanded = false;

    /**
     * The current number of ticks the collapse animation has progressed through.
     */
    private int ticksOnCollapse = 0;
    /**
     * The max number of ticks the collapse animation lasts.
     */
    private static final int MAX_TICKS_ON_COLLAPSE = 10;

    /**
     * Creates a widget that can be dragged and clicked.
     *
     * @param x       The initial X position of this widget
     * @param y       The initial Y position of this widget
     * @param width   The initial width of this widget
     * @param height  The initial height of this widget
     * @param message The text to display inside this widget
     */
    public DraggableWidget(int x, int y, int width, int height, String message) {
        super(message, null);
        this.captureHover().withXY(x, y).withWH(width, height);
    }

    /**
     * Creates a widget that can be dragged and clicked.
     *
     * @param message The text to display inside this widget
     */
    public DraggableWidget(String message) {
        super(message, null);
        this.captureHover();
    }

    /**
     * Captures a mouse click to initiate dragging and trigger click callbacks.
     *
     * @param click the Click object containing mouse coordinates and button information
     * @param doubled whether the click was a double click
     * @return Whether this widget handled the click
     */
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (isMouseOver(mouseX, mouseY)) {
            this.setFocused();
            switch (button) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> {
                    this.startingX = this.getX();
                    this.startingY = this.getY();
                    this.startingMouseX = mouseX;
                    this.startingMouseY = mouseY;
                    this.dragging = true;
                    return true;
                }
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> {
                    this.dragging = false;
                    if (clickCallback != null) {
                        clickCallback.onClick(mouseX, mouseY, button);
                    }
                    return true;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Captures a mouse drag to update the position of this widget.
     *
     * @param mouseX   the X coordinate of the mouse
     * @param mouseY   the Y coordinate of the mouse
     * @param button   the mouse button number
     * @param ignored  the mouse delta X
     * @param ignored_ the mouse delta Y
     * @return Whether this widget handled the drag
     */
    @Override
    public boolean mouseDragged(Click click, double ignored, double ignored_) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (dragging) {
            int deltaX = (int) (mouseX - startingMouseX);
            int deltaY = (int) (mouseY - startingMouseY);
            int newX = startingX + deltaX;
            int newY = startingY + deltaY;
            MinecraftClient mc = MinecraftClient.getInstance();
            int scrW = mc.getWindow().getScaledWidth();
            int scrH = mc.getWindow().getScaledHeight();
            newX = Math.max(0, Math.min(newX, scrW - this.width));
            newY = Math.max(0, Math.min(newY, scrH - this.height));
            this.setX(newX);
            this.setY(newY);
            if (dragCallback != null) {
                dragCallback.updateParentPosition(newX, newY, deltaX, deltaY);
            }
        }

        return dragging;
    }

    /**
     * Captures a mouse release to stop the dragging of this widget.
     * <p>Does not prevent this event from propagating to other elements.</p>
     *
     * @param mouseX the X coordinate of the mouse
     * @param mouseY the Y coordinate of the mouse
     * @param button the mouse button number
     * @return {@code false}
     */
    @Override
    public boolean mouseReleased(Click click) {
        dragging = false;
        return false;
    }

    /**
     * Attaches a callback that is triggered when this widget is dragged to a new position.
     *
     * @param callback The callback to trigger on drag
     */
    public void onDrag(DragCallback callback) {
        this.dragCallback = callback;
    }

    /**
     * Attaches a callback that is triggered when this widget is clicked and not dragged.
     *
     * @param callback The callback to trigger on click
     */
    public void onClick(ClickCallback callback) {
        this.clickCallback = callback;
    }

    @Override
    public void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left,
                       int top, int right, int bottom) {
        TextRenderer textRenderer = Cigarette.REGULAR;

        if (!this.expanded) {
            ticksOnCollapse = Math.min(ticksOnCollapse + 1, MAX_TICKS_ON_COLLAPSE);
        } else {
            ticksOnCollapse = Math.max(ticksOnCollapse - 1, 0);
        }
        double progress = ticksOnCollapse / (double) MAX_TICKS_ON_COLLAPSE;
        progress = CigaretteScreen.easeOutExpo(progress);

        // Render gradient background with proper rounded corners
        // Use horizontal animated gradient that goes smoothly left-to-right across the screen
        int[] gradient = ColorScheme.getCategoryHeaderGradient();
        
        context.getMatrices().pushMatrix();
        
        // Draw the entire gradient background with wave animation
        GradientRenderer.renderHorizontalWaveGradient(
            context, left, top, right, bottom,
            gradient[0], gradient[1],
            ColorScheme.getWaveWavelength(),
            ColorScheme.getWaveSpeed(),
            ColorScheme.getWaveAmplitude() * 0.3f, // Reduce amplitude for subtler effect
            0.0f);
        
        // Apply rounded corners by masking the corners with background color
        int cornerRadius = 4;
        int height = bottom - top;
        
        // Top corners
        for (int y = 0; y < cornerRadius; y++) {
            int dy = cornerRadius - 1 - y;
            int dx = (int) Math.floor(Math.sqrt((double) cornerRadius * cornerRadius - (double) dy * dy));
            int inset = cornerRadius - dx;
            if (inset > 0) {
                // Use the primary gradient color for corner blending
                context.fill(left, top + y, left + inset, top + y + 1, gradient[0]);
                context.fill(right - inset, top + y, right, top + y + 1, gradient[0]);
            }
        }
        
        // Bottom corners (only if collapsed)
        if (!this.expanded) {
            for (int y = height - cornerRadius; y < height; y++) {
                int dy = y - (height - cornerRadius);
                int dx = (int) Math.floor(Math.sqrt((double) cornerRadius * cornerRadius - (double) dy * dy));
                int inset = cornerRadius - dx;
                if (inset > 0) {
                    // Use the primary gradient color for corner blending
                    context.fill(left, top + y, left + inset, top + y + 1, gradient[0]);
                    context.fill(right - inset, top + y, right, top + y + 1, gradient[0]);
                }
            }
        }
        
        context.getMatrices().popMatrix();
        GradientRenderer.renderSatinOverlay(context, left, top, right, bottom);

        // Draw logo
        context.drawTexture(RenderPipelines.GUI_TEXTURED, Cigarette.LOGO_IDENTIFIER,
                left + 4, top + 4, 0, 0, 12, 12, 12, 12);

        // Draw text
        Text text = getMessage();
        int textWidth = textRenderer.getWidth(text);
        int horizontalMargin = (width - textWidth) / 2;
        int verticalMargin = (height - textRenderer.fontHeight) / 2;
        context.drawText(textRenderer, text, left + horizontalMargin, top + verticalMargin + 1,
                CigaretteScreen.PRIMARY_TEXT_COLOR, false);
    }
}