package dev.cigarette.lib;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public class Shape {
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
                    int cEdge = Color.scaleAlpha(color, Math.max(0f, Math.min(1f, feather)));
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
                    int cEdge = Color.scaleAlpha(color, Math.max(0f, Math.min(1f, feather)));
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
                        int cEdge = Color.scaleAlpha(color, Math.max(0f, Math.min(1f, leftFeather)));
                        if (lx >= left && lx < right) {
                            context.fill(lx, y, lx + 1, y + 1, cEdge);
                        }
                    }
                    if (rightFeather > 0.001f) {
                        int rx = rightX;
                        int cEdge = Color.scaleAlpha(color, Math.max(0f, Math.min(1f, rightFeather)));
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
                        int cEdge = Color.scaleAlpha(color, Math.max(0f, Math.min(1f, leftFeather)));
                        if (lx >= left && lx < right) {
                            context.fill(lx, y, lx + 1, y + 1, cEdge);
                        }
                    }
                    if (rightFeather > 0.001f) {
                        int rx = rightX;
                        int cEdge = Color.scaleAlpha(color, Math.max(0f, Math.min(1f, rightFeather)));
                        if (rx >= left && rx < right) {
                            context.fill(rx, y, rx + 1, y + 1, cEdge);
                        }
                    }
                }
            }
        }
    }

    public static void rotatedLine(DrawContext context, int x1, int y1, int x2, int y2, int color, float rotation) {
        dxCalc(context, x1, y1, x2, y2, color, rotation, (float) x1, (float) y1);
    }

    public static void rotatedLine(DrawContext context, int x1, int y1, int x2, int y2, int color, float rotation,
            boolean rotateFromCenter) {
        float pivotX = rotateFromCenter ? (x1 + x2) / 2.0f : x1;
        float pivotY = rotateFromCenter ? (y1 + y2) / 2.0f : y1;
        dxCalc(context, x1, y1, x2, y2, color, rotation, pivotX, pivotY);
    }

    private static void dxCalc(DrawContext context, int x1, int y1, int x2, int y2, int color, float rotation, float pivotX, float pivotY) {
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

    public static void userFaceTexture(DrawContext context, int x, int y, int w, int h, PlayerEntity player) {
        SkinTextures t = MinecraftClient.getInstance().getSkinProvider().getSkinTextures(player.getGameProfile());
        Identifier text = t.texture();
        if (text != null) {
            int texW = 64, texH = 64;
            int u = 8, v = 8, faceW = 8, faceH = 8;
            context.drawTexture(
                RenderLayer::getGuiTextured,
                text,
                x, y, (float) u, (float) v, w, h, faceW, faceH,
                texW, texH
            );
        }
    }
}