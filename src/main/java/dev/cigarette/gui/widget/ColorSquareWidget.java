package dev.cigarette.gui.widget;

import net.minecraft.client.gui.DrawContext;

public class ColorSquareWidget extends BaseWidget<Integer> {
    public ColorSquareWidget() {
        super("", null);
        this.withDefault(0xFFFFFFFF);
    }

    @Override
    protected void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom) {
        int colorBoxWidth = (bottom - top) - 6;
        context.fill(right - 3 - colorBoxWidth, top + 3, right - 3, bottom - 3, this.getRawState());
    }
}
