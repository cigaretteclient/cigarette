package dev.cigarette.gui.hud.bar.widgets;

import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.lib.Color;
import dev.cigarette.lib.Shape;
import dev.cigarette.gui.hud.bar.api.BarWidget;
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
        this.id = id;
        this.entity = entity;
        this.label = label != null ? label : "";
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

    @Override
    public void render(DrawContext ctx, int left, int top, int width, int height, float visibility, TextRenderer tr) {
        int padX = 6;
        int iconSize = Math.max(8, height - 4);
        int right = left + width;
        int bottom = top + height;

        int bga = Color.scaleAlpha(0x90000000, visibility);
        ctx.fill(left, top, right, bottom, bga);

        int borderColor = Color.colorVertical(left, top);
        int borderW = 2;
        int bRight = Math.min(right, left + borderW);
        if (left < bRight) ctx.fill(left, top, bRight, bottom, borderColor);

        int triSize = Math.min(6, height / 3);
        if (triSize >= 3) {
            int tx = right - triSize - 2;
            int ty = top + (height - triSize) / 2;
            int triCol = Color.scaleAlpha((CigaretteScreen.SECONDARY_COLOR & 0x00FFFFFF) | 0xFF000000, visibility);
            ctx.fill(tx, ty, tx + triSize, ty + triSize, triCol);
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
            ctx.fill(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize, iconCol);
        }

        int textX = iconLeft + iconSize + padX;
        int textY = top + (height - tr.fontHeight) / 2;
        int textColor = (CigaretteScreen.PRIMARY_TEXT_COLOR & 0x00FFFFFF) | 0xFF000000;
        ctx.drawText(tr, label, textX, textY, textColor, true);

        if (visibility < 1f) {
            int overlay = Color.scaleAlpha(0xFF000000, 1f - visibility);
            ctx.fill(left, top, right, bottom, overlay);
        }
    }
}

