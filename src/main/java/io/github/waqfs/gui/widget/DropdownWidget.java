package io.github.waqfs.gui.widget;

import io.github.waqfs.Cigarette;
import io.github.waqfs.gui.Scissor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

public class DropdownWidget<Widget extends BaseWidget<?>, StateType>
        extends PassthroughWidget<BaseWidget<?>, BaseWidget.Stateless> {
    protected Widget header;
    protected ScrollableWidget<BaseWidget<?>> container;
    private boolean dropdownVisible = false;
    private boolean dropdownIndicator = true;

    private long rotateStartMillis = 0L;
    private double rotateAngleRad = 0.0;

    private double rotateOffsetRad = 0.0;
    private static final int ROTATION_PERIOD_MS = 2000;

    private boolean animating = false;
    private boolean opening = false;
    private long animStartMillis = 0L;
    private static final int TOGGLE_ANIM_MS = 220;

    public DropdownWidget(Text message, @Nullable Text tooltip) {
        super(message, tooltip);
        this.withDefault(new BaseWidget.Stateless());
        this.container = new ScrollableWidget<>(0, 0);
        this.children = new ScrollableWidget[] { this.container };
    }

    public DropdownWidget<Widget, StateType> setHeader(Widget header) {
        this.header = header;
        return this;
    }

    public DropdownWidget<Widget, StateType> setChildren(@Nullable BaseWidget<?>... children) {
        this.container.setChildren(children);
        return this;
    }

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
                boolean target = children != null && !dropdownVisible;

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

        if (this.container.children == null)
            return;
        if (this.container.children.length > 0 && dropdownIndicator) {
            int w = 10;
            int h = 10;
            int iconX = right - 12;
            int iconY = top + (height / 2) - (h / 2);

            int angleDeg = (int) Math.round(Math.toDegrees(rotateAngleRad * toggleProgress));
            cigaretteOnlyAt(context, iconX, iconY, w, h, angleDeg);
        }

        if (toggleProgress <= 0.001 || !this.focused)
            return;

        context.getMatrices().push();

        float scale = (float) (0.9 + 0.1 * toggleProgress);
        // float alpha = (float) toggleProgress;
        context.getMatrices().translate(right, top, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.getMatrices().translate(-right, -top, 0);

        Scissor.pushExclusive(context, right, top, right + this.container.getWidth(), top + this.container.getHeight());

        this.container.withXY(right + childLeftOffset, top)
                .withWH(this.container.getWidth(), this.container.getHeight())
                .renderWidget(context, mouseX, mouseY, deltaTicks);
        Scissor.popExclusive();
        context.getMatrices().pop();
    }

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
