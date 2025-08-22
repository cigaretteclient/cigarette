package dev.cigarette.gui.hud.bar.widgets;

import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.gui.hud.bar.BarDisplay;
import dev.cigarette.lib.Color;
import dev.cigarette.lib.Shape;
import dev.cigarette.gui.hud.bar.api.BarWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public class EntityChipWidget implements BarWidget {
    private final String id;
    private final Entity entity;
    private final String label;
    private final double sortKey;
    private final int iconColor;

    public EntityChipWidget(String id, Entity entity, String label, double sortKey, int iconColor) {
        if (label == null) label = "";
        this.id = (id == null ? "entitychip" : id);
        this.entity = entity;
        this.label = label;
        this.sortKey = sortKey;
        this.iconColor = iconColor;
    }

    @Override
    public String id() { return id; }

    @Override
    public double sortKey() { return sortKey; }

    @Override
    public String label(TextRenderer tr) { return label; }

    @Override
    public int measureWidth(TextRenderer tr, int rowHeight, int padX) {
        int iconSize = Math.max(8, rowHeight - 4);
        int textW = tr.getWidth(label);
        return textW + padX * 3 + iconSize;
    }

    private static double angleDegToEntity(Entity target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || target == null) return 0.0;
        double dx = target.getX() - mc.player.getX();
        double dz = target.getZ() - mc.player.getZ();
        double yawRad = Math.toRadians(mc.player.getHeadYaw());
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        double localX = dx * cos + dz * sin;
        double localZ = -dx * sin + dz * cos;
        return Math.toDegrees(Math.atan2(localX, localZ));
    }

    private static void fillTriangle(DrawContext ctx, int x1, int y1, int x2, int y2, int x3, int y3, int color) {
        if (y2 < y1) { int tx = x1; x1 = x2; x2 = tx; int ty = y1; y1 = y2; y2 = ty; }
        if (y3 < y1) { int tx = x1; x1 = x3; x3 = tx; int ty = y1; y1 = y3; y3 = ty; }
        if (y3 < y2) { int tx = x2; x2 = x3; x3 = tx; int ty = y2; y2 = y3; y3 = ty; }
        if (y1 == y3) return;
        float dx13 = (float) (x3 - x1) / Math.max(1, (y3 - y1));
        float dx12 = (y2 == y1) ? 0 : (float) (x2 - x1) / (y2 - y1);
        float dx23 = (y3 == y2) ? 0 : (float) (x3 - x2) / (y3 - y2);
        float sx1 = x1;
        float sx2 = x1;
        for (int y = y1; y < y2; y++) {
            int xa = Math.round(sx1), xb = Math.round(sx2);
            if (xa > xb) { int t = xa; xa = xb; xb = t; }
            ctx.fill(xa, y, xb + 1, y + 1, color);
            sx1 += dx13; sx2 += dx12;
        }
        sx2 = x2;
        for (int y = y2; y <= y3; y++) {
            int xa = Math.round(sx1), xb = Math.round(sx2);
            if (xa > xb) { int t = xa; xa = xb; xb = t; }
            ctx.fill(xa, y, xb + 1, y + 1, color);
            sx1 += dx13; sx2 += dx23;
        }
    }

    private static void isoscelesTriangle(DrawContext ctx, int x, int y, int size, double angleRad, int color) {
        float cx = x + size / 2f;
        float cy = y + size / 2f;
        float forwardX = (float) Math.cos(angleRad);
        float forwardY = (float) Math.sin(angleRad);
        float perpX = -forwardY;
        float perpY = forwardX;
        float tipLen = Math.max(4f, size * 0.6f);
        float baseBack = Math.max(2f, size * 0.25f);
        float baseHalf = Math.max(2f, size * 0.28f);

        float tx = cx + forwardX * tipLen;
        float ty = cy + forwardY * tipLen;
        float bx = cx - forwardX * baseBack;
        float by = cy - forwardY * baseBack;
        int x1 = Math.round(tx), y1 = Math.round(ty);
        int x2 = Math.round(bx + perpX * baseHalf), y2 = Math.round(by + perpY * baseHalf);
        int x3 = Math.round(bx - perpX * baseHalf), y3 = Math.round(by - perpY * baseHalf);
        fillTriangle(ctx, x1, y1, x2, y2, x3, y3, color);
    }

    @Override
    public void render(DrawContext ctx, int left, int top, int width, int height, float visibility, TextRenderer tr) {
        int padX = Math.max(0, BarDisplay.GLOBAL_PADDING);
        int iconSize = Math.max(8, height - 4);
        int right = left + width;
        int bottom = top + height;

        int borderW = 2;
        int bRight = Math.min(right, left + borderW);
        int borderColor = (iconColor != 0)
                ? Color.scaleAlpha((iconColor & 0x00FFFFFF) | 0xFF000000, visibility)
                : Color.scaleAlpha(Color.colorVertical(left, top), visibility);
        if (left < bRight) ctx.fill(left, top, bRight, bottom, borderColor);

        int triSize = Math.min(10, height / 2);
        if (triSize >= 4) {
            int tx = right - triSize - 2;
            int ty = top + (height - triSize) / 2;
            double relDeg = angleDegToEntity(entity);
            float t = (float) Math.min(1.0, Math.abs(relDeg) / 180.0);
            int nearColor = CigaretteScreen.PRIMARY_COLOR;
            int farColor = CigaretteScreen.DARK_BACKGROUND_COLOR;
            int lerped = Color.lerpColor(nearColor, farColor, t);
            int triCol = Color.scaleAlpha(lerped, visibility);
            double angleRad = Math.toRadians(relDeg) - Math.PI / 2.0;
            isoscelesTriangle(ctx, tx, ty, triSize, angleRad, triCol);
        }

        int iconLeft = left + padX;
        int iconTop = top + (height - iconSize) / 2;
        boolean drewIcon = false;
        if (entity instanceof PlayerEntity player) {
            try {
                Shape.userFaceTexture(ctx, iconLeft, iconTop, iconSize, iconSize, player);
                drewIcon = true;
            } catch (Throwable ignored) {}
        } else if (entity instanceof LivingEntity living) {
            try {
                int cxEntity = iconLeft + iconSize / 2;
                int cyEntity = iconTop + iconSize;
                InventoryScreen.drawEntity(ctx, cxEntity, cyEntity, Math.max(10, iconSize - 2), 0, 0, 0f, 0f, 0f, living);
                drewIcon = true;
            } catch (Throwable ignored) {}
        }
        if (!drewIcon) {
            int iconCol = (iconColor == 0 ? 0xFFFFFFFF : iconColor);
            iconCol = Color.scaleAlpha((iconCol & 0x00FFFFFF) | 0xFF000000, visibility);
            int cx2 = iconLeft + iconSize / 2;
            int cy2 = iconTop + iconSize / 2;
            int r2 = iconSize / 2 - 1;
            for (int i = 0; i < 2; i++) {
                Shape.arc(ctx, cx2, cy2, r2 - i, 0, 360, iconCol);
            }
            ctx.drawText(tr, "?", cx2 - tr.getWidth("?") / 2, cy2 - tr.fontHeight / 2, iconCol, false);
        }

        int textX = iconLeft + iconSize + padX;
        int textY = top + (height - tr.fontHeight) / 2;
        int textColor = Color.scaleAlpha((CigaretteScreen.PRIMARY_TEXT_COLOR & 0x00FFFFFF) | 0xFF000000, visibility);
        ctx.drawText(tr, label, textX, textY, textColor, true);

        float fade = 1f - Math.max(0f, Math.min(1f, visibility));
        if (fade > 0.01f) {
            int a = Math.round(fade * 180);
            int overlay = (a << 24);
            ctx.fill(left, top, right, bottom, overlay);
        }
    }

    public static final class Progress extends EntityChipWidget {
        private final float progress; // 0..1
        private final int barColor;
        private final int barBgColor;

        public Progress(String id, Entity entity, String label, double sortKey, int iconColor, float progress, int barColor, int barBgColor) {
            super(id, entity, label, sortKey, iconColor);
            this.progress = Math.max(0f, Math.min(1f, progress));
            this.barColor = barColor;
            this.barBgColor = barBgColor;
        }

        @Override
        public void render(DrawContext ctx, int left, int top, int width, int height, float visibility, TextRenderer tr) {
            super.render(ctx, left, top, width, height, visibility, tr);
            int padX = Math.max(0, BarDisplay.GLOBAL_PADDING);
            int iconSize = Math.max(8, height - 4);
            int textLeft = left + padX + iconSize + padX;
            int textRight = left + width - padX;
            int barTop = top + height - Math.max(2, height / 6) - 2;
            int barHeight = Math.max(2, height / 6);
            if (textRight > textLeft) {
                int bg = Color.scaleAlpha((barBgColor & 0x00FFFFFF) | 0xFF000000, visibility);
                int fg = Color.scaleAlpha((barColor & 0x00FFFFFF) | 0xFF000000, visibility);
                Shape.roundedRect(ctx, textLeft, barTop, textRight, barTop + barHeight, bg, 2);
                int fillRight = textLeft + Math.round((textRight - textLeft) * progress);
                if (fillRight > textLeft) {
                    Shape.roundedRect(ctx, textLeft, barTop, fillRight, barTop + barHeight, fg, 2);
                }
            }
        }
    }
}
