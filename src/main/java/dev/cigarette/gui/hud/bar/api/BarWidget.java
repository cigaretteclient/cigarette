package dev.cigarette.gui.hud.bar.api;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * A renderable item inside the BarDisplay. Widgets are measured in the
 * context of a fixed row height; they can render custom content within
 * the allocated bounds and receive a visibility factor for fades.
 */
public interface BarWidget {
    /** A unique and stable identifier for de-duplication and animation state. */
    String id();

    /** Optional numeric key for sorting left-to-right; smaller first. */
    default double sortKey() { return 0d; }

    /** Human-readable label (used for text measurement if needed). */
    default String label(TextRenderer tr) { return ""; }

    /**
     * Measure the pixel width the widget would like to occupy given the row height and padding.
     * Implementations should include any inner padding they need.
     */
    int measureWidth(TextRenderer tr, int rowHeight, int padX);

    /** Render the widget within the given rectangle. */
    void render(DrawContext ctx, int left, int top, int width, int height, float visibility, TextRenderer tr);
}

