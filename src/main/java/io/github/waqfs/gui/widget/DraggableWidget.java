package io.github.waqfs.gui.widget;

import io.github.waqfs.Cigarette;
import io.github.waqfs.gui.CigaretteScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class DraggableWidget extends BaseWidget<BaseWidget.Stateless> {
    public interface DragCallback {
        void updateParentPosition(int newX, int newY, int deltaX, int deltaY);
    }

    public interface ClickCallback {
        void onClick(double mouseX, double mouseY, int button);
    }

    private boolean dragging = false;
    private int startingX = 0;
    private int startingY = 0;
    private double startingMouseX = 0;
    private double startingMouseY = 0;
    private @Nullable DragCallback dragCallback = null;
    private @Nullable ClickCallback clickCallback = null;
    public boolean expanded = true;

    private int ticksOnCollapse = 0;
    private static final int MAX_TICKS_ON_COLLAPSE = 10;

    public DraggableWidget(int x, int y, int width, int height, Text message) {
        super(message, null);
        this.captureHover().withXY(x, y).withWH(width, height);
    }

    public DraggableWidget(Text message) {
        super(message, null);
        this.captureHover();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double ignored, double ignored_) {
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

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return false;
    }

    public void onDrag(DragCallback callback) {
        this.dragCallback = callback;
    }

    public void onClick(ClickCallback callback) {
        this.clickCallback = callback;
    }

    public static class ColorUtil {
        public static int lerpColor(int color1, int color2, float t) {
            if (t < 0f)
                t = 0f;
            else if (t > 1f)
                t = 1f;
            int a = ((int) lerp((color1 >> 24) & 0xFF, (color2 >> 24) & 0xFF, t)) << 24;
            int r = ((int) lerp((color1 >> 16) & 0xFF, (color2 >> 16) & 0xFF, t)) << 16;
            int g = ((int) lerp((color1 >> 8) & 0xFF, (color2 >> 8) & 0xFF, t)) << 8;
            int b = (int) lerp(color1 & 0xFF, color2 & 0xFF, t);
            return a | r | g | b;
        }

        public static int colorDarken(int color, float factor) {
            int a = (color >> 24) & 0xFF;
            int r = (int) (((color >> 16) & 0xFF) * factor);
            int g = (int) (((color >> 8) & 0xFF) * factor);
            int b = (int) ((color & 0xFF) * factor);
            r = Math.max(0, Math.min(255, r));
            g = Math.max(0, Math.min(255, g));
            b = Math.max(0, Math.min(255, b));
            return (a << 24) | (r << 16) | (g << 8) | b;
        }

        public static int colorTransparentize(int color, float factor) {
            int a = (color >> 24) & 0xFF;
            float newA;
            if (factor <= 0f) {
                newA = a;
            } else if (factor <= 1f) {
                newA = a * (1f - factor);
            } else {
                newA = a / factor;
            }
            int ai = Math.max(0, Math.min(255, Math.round(newA)));
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            return (ai << 24) | (r << 16) | (g << 8) | b;
        }

        private static float lerp(float start, float end, float t) {
            return start + (end - start) * t;
        }
    }

    public static int color(int x, int y) {
        double seconds = (System.nanoTime() / 1_000_000_000.0);
        int screenW = 1920, screenH = 1080;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getWindow() != null) {
            screenW = mc.getWindow().getScaledWidth();
            screenH = mc.getWindow().getScaledHeight();
        }
        float xNorm = Math.max(0f, Math.min(1f, (float) x / Math.max(1, screenW)));
        float yNorm = Math.max(0f, Math.min(1f, (float) y / Math.max(1, screenH)));
        float s = xNorm + 0.2f * yNorm;
        double speedHz = 0.3;
        double phase = 2 * Math.PI * (speedHz * seconds - s);
        float pingpong = 0.5f * (1.0f + (float) Math.sin(phase));
        int bg = ColorUtil.lerpColor(CigaretteScreen.PRIMARY_COLOR, 0xFF020618, (float) pingpong);
        return bg;
    }

    public static int colorVertical(int x, int y) {
        double seconds = (System.nanoTime() / 1_000_000_000.0);
        int screenW = 1920, screenH = 1080;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getWindow() != null) {
            screenW = mc.getWindow().getScaledWidth();
            screenH = mc.getWindow().getScaledHeight();
        }
        float xNorm = Math.max(0f, Math.min(1f, (float) x / Math.max(1, screenW)));
        float yNorm = Math.max(0f, Math.min(1f, (float) y / Math.max(1, screenH)));
        float s = xNorm + 0.2f * yNorm;
        double speedHz = 0.3;
        double phase = 2 * Math.PI * (speedHz * seconds - s);
        float pingpong = 0.5f * (1.0f + (float) Math.sin(phase));
        int bg = ColorUtil.lerpColor(CigaretteScreen.PRIMARY_COLOR, 0xFF020618, (float) pingpong);
        return bg;
    }

    public static void pixelAt(DrawContext context, int x, int y, int color) {
        context.fill(x, y, x + 1, y + 1, color);
    }

    public static void arc(DrawContext context, int centerX, int centerY, int radius, int startAngle, int endAngle,
            int color) {
        double angleStep = 1.0 / radius;
        for (double angle = Math.toRadians(startAngle); angle <= Math.toRadians(endAngle); angle += angleStep) {
            int x = centerX + (int) (radius * Math.cos(angle));
            int y = centerY + (int) (radius * Math.sin(angle));
            pixelAt(context, x, y, color);
        }
    }

    private static final java.util.concurrent.ConcurrentHashMap<Integer, int[]> RADIUS_OFFSETS = new java.util.concurrent.ConcurrentHashMap<>();

    private static int[] getRadiusOffsets(int r) {
        int[] res = RADIUS_OFFSETS.computeIfAbsent(Math.max(0, r), radius -> {
            int[] offs = new int[Math.max(1, radius + 1)];
            for (int dy = 0; dy <= radius; dy++) {

                offs[dy] = (int) Math.floor(Math.sqrt((double) radius * radius - (double) dy * dy));
            }
            return offs;
        });

        if (RADIUS_OFFSETS.size() > 128) {
            RADIUS_OFFSETS.clear();
            RADIUS_OFFSETS.put(Math.max(0, r), res);
        }
        return res;
    }

    public static void roundedRect(DrawContext context, int left, int top, int right, int bottom, int color, int r) {
        int width = Math.max(0, right - left);
        int height = Math.max(0, bottom - top);
        if (width <= 0 || height <= 0)
            return;
        int rad = Math.max(0, Math.min(r, Math.min(width / 2, height / 2)));
        if (rad == 0) {
            context.fill(left, top, right, bottom, color);
            return;
        }
        int[] offs = getRadiusOffsets(rad);
        boolean translucent = ((color >>> 24) & 0xFF) < 255;
        for (int y = top; y < bottom; y++) {
            int yIndex = y - top;
            int leftX = left;
            int rightX = right;
            float feather = 0f;
            if (yIndex < rad) {
                int dy = (rad - 1) - yIndex;
                int dx = offs[dy];
                leftX = left + rad - dx;
                rightX = right - (rad - dx);
                if (translucent) {
                    double dxd = Math.sqrt((double) rad * rad - (double) dy * dy);
                    feather = (float) (dxd - dx);
                }
            } else if (yIndex >= height - rad) {
                int dy = yIndex - (height - rad);
                int dx = offs[dy];
                leftX = left + rad - dx;
                rightX = right - (rad - dx);
                if (translucent) {
                    double dxd = Math.sqrt((double) rad * rad - (double) dy * dy);
                    feather = (float) (dxd - dx);
                }
            }
            if (leftX < rightX) {
                context.fill(leftX, y, rightX, y + 1, color);
                if (translucent && feather > 0.001f) {
                    int leftFeatherX = leftX - 1;
                    int rightFeatherX = rightX;
                    int cEdge = scaleAlpha(color, Math.max(0f, Math.min(1f, feather)));
                    if (leftFeatherX >= left && leftFeatherX < right) {
                        context.fill(leftFeatherX, y, leftFeatherX + 1, y + 1, cEdge);
                    }
                    if (rightFeatherX >= left && rightFeatherX < right) {
                        context.fill(rightFeatherX, y, rightFeatherX + 1, y + 1, cEdge);
                    }
                }
            }
        }
    }

    public static void roundedRect(DrawContext context, int left, int top, int right, int bottom, int color, int r,
            boolean topCorners, boolean bottomCorners) {
        int width = Math.max(0, right - left);
        int height = Math.max(0, bottom - top);
        if (width <= 0 || height <= 0)
            return;
        int rad = Math.max(0, Math.min(r, Math.min(width / 2, height / 2)));
        if (rad == 0 || (!topCorners && !bottomCorners)) {
            context.fill(left, top, right, bottom, color);
            return;
        }
        int[] offs = getRadiusOffsets(rad);
        boolean translucent = ((color >>> 24) & 0xFF) < 255;
        for (int y = top; y < bottom; y++) {
            int yIndex = y - top;
            int leftX = left;
            int rightX = right;
            boolean isTopArc = topCorners && yIndex < rad;
            boolean isBottomArc = bottomCorners && yIndex >= height - rad;
            float feather = 0f;
            if (isTopArc) {
                int dy = (rad - 1) - yIndex;
                int dx = offs[dy];
                leftX = left + rad - dx;
                rightX = right - (rad - dx);
                if (translucent) {
                    double dxd = Math.sqrt((double) rad * rad - (double) dy * dy);
                    feather = (float) (dxd - dx);
                }
            } else if (isBottomArc) {
                int dy = yIndex - (height - rad);
                int dx = offs[dy];
                leftX = left + rad - dx;
                rightX = right - (rad - dx);
                if (translucent) {
                    double dxd = Math.sqrt((double) rad * rad - (double) dy * dy);
                    feather = (float) (dxd - dx);
                }
            }
            if (leftX < rightX) {
                context.fill(leftX, y, rightX, y + 1, color);
                if (translucent && feather > 0.001f) {
                    int leftFeatherX = leftX - 1;
                    int rightFeatherX = rightX;
                    int cEdge = scaleAlpha(color, Math.max(0f, Math.min(1f, feather)));
                    if (leftFeatherX >= left && leftFeatherX < right) {
                        context.fill(leftFeatherX, y, leftFeatherX + 1, y + 1, cEdge);
                    }
                    if (rightFeatherX >= left && rightFeatherX < right) {
                        context.fill(rightFeatherX, y, rightFeatherX + 1, y + 1, cEdge);
                    }
                }
            }
        }
    }

    public static void roundedRect(DrawContext context, int left, int top, int right, int bottom, int color, int r,
            boolean topLeft, boolean topRight, boolean bottomLeft, boolean bottomRight) {
        int width = Math.max(0, right - left);
        int height = Math.max(0, bottom - top);
        if (width <= 0 || height <= 0)
            return;
        int rad = Math.max(0, Math.min(r, Math.min(width / 2, height / 2)));
        if (rad == 0 || (!topLeft && !topRight && !bottomLeft && !bottomRight)) {
            context.fill(left, top, right, bottom, color);
            return;
        }
        int[] offs = getRadiusOffsets(rad);
        boolean translucent = ((color >>> 24) & 0xFF) < 255;
        for (int y = top; y < bottom; y++) {
            int yIndex = y - top;
            int leftX = left;
            int rightX = right;
            float leftFeather = 0f;
            float rightFeather = 0f;

            if (yIndex < rad) {
                int dy = (rad - 1) - yIndex;
                int dx = offs[dy];
                if (topLeft) {
                    leftX = Math.max(leftX, left + rad - dx);
                    if (translucent) {
                        double dxd = Math.sqrt((double) rad * rad - (double) dy * dy);
                        leftFeather = (float) (dxd - dx);
                    }
                }
                if (topRight) {
                    rightX = Math.min(rightX, right - (rad - dx));
                    if (translucent) {
                        double dxd = Math.sqrt((double) rad * rad - (double) dy * dy);
                        rightFeather = (float) (dxd - dx);
                    }
                }
            } else if (yIndex >= height - rad) {
                int dy = yIndex - (height - rad);
                int dx = offs[dy];
                if (bottomLeft) {
                    leftX = Math.max(leftX, left + rad - dx);
                    if (translucent) {
                        double dxd = Math.sqrt((double) rad * rad - (double) dy * dy);
                        leftFeather = (float) (dxd - dx);
                    }
                }
                if (bottomRight) {
                    rightX = Math.min(rightX, right - (rad - dx));
                    if (translucent) {
                        double dxd = Math.sqrt((double) rad * rad - (double) dy * dy);
                        rightFeather = (float) (dxd - dx);
                    }
                }
            }

            if (leftX < rightX) {
                context.fill(leftX, y, rightX, y + 1, color);
                if (translucent) {
                    if (leftFeather > 0.001f) {
                        int lx = leftX - 1;
                        int cEdge = scaleAlpha(color, Math.max(0f, Math.min(1f, leftFeather)));
                        if (lx >= left && lx < right) {
                            context.fill(lx, y, lx + 1, y + 1, cEdge);
                        }
                    }
                    if (rightFeather > 0.001f) {
                        int rx = rightX;
                        int cEdge = scaleAlpha(color, Math.max(0f, Math.min(1f, rightFeather)));
                        if (rx >= left && rx < right) {
                            context.fill(rx, y, rx + 1, y + 1, cEdge);
                        }
                    }
                }
            }
        }
    }

    public static void roundedRect(DrawContext context, int left, int top, int right, int bottom, int color,
            int rTL, int rTR, int rBL, int rBR) {
        int width = Math.max(0, right - left);
        int height = Math.max(0, bottom - top);
        if (width <= 0 || height <= 0)
            return;

        rTL = Math.max(0, Math.min(rTL, Math.min(width / 2, height / 2)));
        rTR = Math.max(0, Math.min(rTR, Math.min(width / 2, height / 2)));
        rBL = Math.max(0, Math.min(rBL, Math.min(width / 2, height / 2)));
        rBR = Math.max(0, Math.min(rBR, Math.min(width / 2, height / 2)));

        if (rTL == 0 && rTR == 0 && rBL == 0 && rBR == 0) {
            context.fill(left, top, right, bottom, color);
            return;
        }

        int[] offsTL = rTL > 0 ? getRadiusOffsets(rTL) : null;
        int[] offsTR = rTR > 0 ? getRadiusOffsets(rTR) : null;
        int[] offsBL = rBL > 0 ? getRadiusOffsets(rBL) : null;
        int[] offsBR = rBR > 0 ? getRadiusOffsets(rBR) : null;
        boolean translucent = ((color >>> 24) & 0xFF) < 255;

        for (int y = top; y < bottom; y++) {
            int yIndex = y - top;
            int leftX = left;
            int rightX = right;
            float leftFeather = 0f;
            float rightFeather = 0f;

            if (rTL > 0 && yIndex < rTL) {
                int dy = (rTL - 1) - yIndex;
                int dx = offsTL[dy];
                leftX = Math.max(leftX, left + rTL - dx);
                if (translucent) {
                    double dxd = Math.sqrt((double) rTL * rTL - (double) dy * dy);
                    leftFeather = (float) (dxd - dx);
                }
            } else if (rBL > 0 && yIndex >= height - rBL) {
                int dy = yIndex - (height - rBL);
                int dx = offsBL[dy];
                leftX = Math.max(leftX, left + rBL - dx);
                if (translucent) {
                    double dxd = Math.sqrt((double) rBL * rBL - (double) dy * dy);
                    leftFeather = (float) (dxd - dx);
                }
            }

            if (rTR > 0 && yIndex < rTR) {
                int dy = (rTR - 1) - yIndex;
                int dx = offsTR[dy];
                rightX = Math.min(rightX, right - (rTR - dx));
                if (translucent) {
                    double dxd = Math.sqrt((double) rTR * rTR - (double) dy * dy);
                    rightFeather = (float) (dxd - dx);
                }
            } else if (rBR > 0 && yIndex >= height - rBR) {
                int dy = yIndex - (height - rBR);
                int dx = offsBR[dy];
                rightX = Math.min(rightX, right - (rBR - dx));
                if (translucent) {
                    double dxd = Math.sqrt((double) rBR * rBR - (double) dy * dy);
                    rightFeather = (float) (dxd - dx);
                }
            }

            if (height <= 3) {
                if (rTL > 0 && yIndex == 0) {
                    leftX = Math.max(leftX, left + 1);
                }
                if (rBL > 0 && yIndex == height - 1) {
                    leftX = Math.max(leftX, left + 1);
                }
                if (rTR > 0 && yIndex == 0) {
                    rightX = Math.min(rightX, right - 1);
                }
                if (rBR > 0 && yIndex == height - 1) {
                    rightX = Math.min(rightX, right - 1);
                }
            }

            if (leftX < rightX) {
                context.fill(leftX, y, rightX, y + 1, color);
                if (translucent) {
                    if (leftFeather > 0.001f) {
                        int lx = leftX - 1;
                        int cEdge = scaleAlpha(color, Math.max(0f, Math.min(1f, leftFeather)));
                        if (lx >= left && lx < right) {
                            context.fill(lx, y, lx + 1, y + 1, cEdge);
                        }
                    }
                    if (rightFeather > 0.001f) {
                        int rx = rightX;
                        int cEdge = scaleAlpha(color, Math.max(0f, Math.min(1f, rightFeather)));
                        if (rx >= left && rx < right) {
                            context.fill(rx, y, rx + 1, y + 1, cEdge);
                        }
                    }
                }
            }
        }
    }

    private static int scaleAlpha(int color, float scale) {
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        int na = Math.max(0, Math.min(255, Math.round(a * scale)));
        return (na << 24) | (r << 16) | (g << 8) | b;
    }

    public static void rotatedLine(DrawContext context, int x1, int y1, int x2, int y2, int color, float rotation) {
        float pivotX = x1;
        float pivotY = y1;
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.hypot(dx, dy);
        float step = Math.max(0.002f, 1.0f / Math.max(16f, length));
        for (float t = 0; t <= 1.0f; t += step) {
            float xt = x1 + t * dx;
            float yt = y1 + t * dy;
            float rx = (float) (Math.cos(rotation) * (xt - pivotX) - Math.sin(rotation) * (yt - pivotY) + pivotX);
            float ry = (float) (Math.sin(rotation) * (xt - pivotX) + Math.cos(rotation) * (yt - pivotY) + pivotY);
            pixelAt(context, Math.round(rx), Math.round(ry), color);
        }
    }

    public static void rotatedLine(DrawContext context, int x1, int y1, int x2, int y2, int color, float rotation,
            boolean rotateFromCenter) {
        float pivotX = rotateFromCenter ? (x1 + x2) / 2.0f : x1;
        float pivotY = rotateFromCenter ? (y1 + y2) / 2.0f : y1;
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.hypot(dx, dy);
        float step = Math.max(0.002f, 1.0f / Math.max(16f, length));
        for (float t = 0; t <= 1.0f; t += step) {
            float xt = x1 + t * dx;
            float yt = y1 + t * dy;
            float rx = (float) (Math.cos(rotation) * (xt - pivotX) - Math.sin(rotation) * (yt - pivotY) + pivotX);
            float ry = (float) (Math.sin(rotation) * (xt - pivotX) + Math.cos(rotation) * (yt - pivotY) + pivotY);
            pixelAt(context, Math.round(rx), Math.round(ry), color);
        }
    }

    @Override
    public void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left,
            int top, int right, int bottom) {
        TextRenderer textRenderer = Cigarette.REGULAR;
        int bgColor = color(left, top);
        if (!this.expanded) {
            ticksOnCollapse = Math.min(ticksOnCollapse + 1, MAX_TICKS_ON_COLLAPSE);
        } else {
            ticksOnCollapse = Math.max(ticksOnCollapse - 1, 0);
        }
        double progress = ticksOnCollapse / (double) MAX_TICKS_ON_COLLAPSE;
        progress = CigaretteScreen.easeOutExpo(progress);
        roundedRect(context, left, top, right, bottom, bgColor, 2, true, !this.expanded);
        if (this.expanded) {
            int borderColor = ColorUtil.colorDarken(bgColor, 0.8f);
            context.drawHorizontalLine(left, right - 1, bottom - 1, borderColor);
        }
        Text text = getMessage();
        int textWidth = textRenderer.getWidth(text);
        int horizontalMargin = (width - textWidth) / 2;
        int verticalMargin = (height - textRenderer.fontHeight) / 2;
        context.drawText(textRenderer, text, left + horizontalMargin, top + verticalMargin + 1,
                CigaretteScreen.PRIMARY_TEXT_COLOR, false);
    }
}