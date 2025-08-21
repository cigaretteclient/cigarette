package dev.cigarette.gui.hud.bar.api;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public interface BarWidget {
    String id();

    default double sortKey() { return 0d; }

    default String label(TextRenderer tr) { return ""; }

    int measureWidth(TextRenderer tr, int rowHeight, int padX);

    void render(DrawContext ctx, int left, int top, int width, int height, float visibility, TextRenderer tr);
}

