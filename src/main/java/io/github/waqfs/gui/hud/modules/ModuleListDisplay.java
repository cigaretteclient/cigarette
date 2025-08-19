package io.github.waqfs.gui.hud.modules;

import io.github.waqfs.Cigarette;
import io.github.waqfs.gui.CategoryInstance;
import io.github.waqfs.gui.CigaretteScreen;
import io.github.waqfs.gui.widget.DraggableWidget;
import io.github.waqfs.lib.Color;
import io.github.waqfs.module.BaseModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModuleListDisplay extends ClickableWidget {

    private static class Entry {
        final BaseModule<?, ?> module;
        String name;
        int width;
        float progress;
        float target;
        int lastIndex;

        Entry(BaseModule<?, ?> module, String name, int width) {
            this.module = module;
            this.name = name;
            this.width = width;
            this.progress = 0f;
            this.target = 1f;
            this.lastIndex = 0;
        }
    }

    private final Map<BaseModule<?, ?>, Entry> entries = new HashMap<>();

    public ModuleListDisplay() {
        super(0, 0, 160, 20, Text.of("Module List"));
        Window w = MinecraftClient.getInstance().getWindow();
        this.setX(w.getScaledWidth() - 170);
        this.setY(10);
        this.setDimensions(160, w.getScaledHeight() - 20);
    }

    @Override
    public void setX(int x) {
        Window w = MinecraftClient.getInstance().getWindow();
        if (x + this.getWidth() > w.getScaledWidth()) {
            x = w.getScaledWidth() - this.getWidth();
        }
        super.setX(x);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        TextRenderer tr = Cigarette.REGULAR != null ? Cigarette.REGULAR : MinecraftClient.getInstance().textRenderer;
        if (Cigarette.CONFIG == null)
            return;

        Set<BaseModule<?, ?>> enabledNow = new HashSet<>();
        List<Entry> working = new ArrayList<>();
        for (CategoryInstance cat : Cigarette.CONFIG.allCategories) {
            if (cat == null)
                continue;
            for (BaseModule<?, ?> mod : cat.children) {
                try {
                    if (mod.getRawState()) {
                        enabledNow.add(mod);
                        String name = mod.widget.getMessage().getString();
                        int w = tr.getWidth(name);
                        Entry e = entries.get(mod);
                        if (e == null) {
                            e = new Entry(mod, name, w);
                            entries.put(mod, e);
                        } else {
                            e.name = name;
                            e.width = w;
                        }
                        e.target = 1f;
                        working.add(e);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        List<Entry> workingSorted = new ArrayList<>(working);
        workingSorted.sort((a, b) -> Integer.compare(b.width, a.width));
        int ord = 0;
        for (Entry e : workingSorted) {
            e.lastIndex = ord++;
        }

        for (Entry e : entries.values()) {
            if (!enabledNow.contains(e.module))
                e.target = 0f;
        }

        List<Entry> toRender = new ArrayList<>();

        final float speed = 0.12f;
        entries.values().forEach(e -> {
            float diff = e.target - e.progress;
            if (Math.abs(diff) > 0.0001f) {
                float step = Math.signum(diff) * Math.min(Math.abs(diff), speed);
                e.progress += step;
            }
            if (e.progress < 0f)
                e.progress = 0f;
            if (e.progress > 1f)
                e.progress = 1f;
            if (e.progress < 0.01f && e.target <= 0f) {
            } else {
                toRender.add(e);
            }
        });

        entries.values().removeIf(e -> e.progress < 0.01f && e.target <= 0f);

        toRender.sort((a, b) -> Integer.compare(a.lastIndex, b.lastIndex));

        int x = this.getX();
        boolean doFlip = x < MinecraftClient.getInstance().getWindow().getScaledWidth() / 2;
        int y = this.getY();
        int padX = 3;
        int padY = 1;

        int lineH = tr.fontHeight + padY * 2;

        final int textColor = (CigaretteScreen.PRIMARY_TEXT_COLOR & 0x00FFFFFF) | 0xFF000000;
        final int baseBg = 0x90000000;

        int i = 0;
        for (Entry e : toRender) {
            int lineY = y + i * lineH;
            if (lineY + tr.fontHeight > this.getY() + this.getHeight())
                break;

            float vis = Math.max(0f, Math.min(1f, e.progress));
            int slide = Math.round((1f - vis) * 24f);

            int finalX = doFlip ? (x + 4) : (x + this.getWidth() - 4 - e.width);

            int drawX = doFlip ? (finalX - slide) : (finalX + slide);

            String name = e.name;

            int bgLeft = drawX - padX;
            int bgRight = drawX + e.width + padX;
            int bgTop = lineY - padY;
            int bgBottom = lineY + tr.fontHeight + padY;

            int widgetLeft = this.getX();
            int widgetRight = this.getX() + this.getWidth();
            if (bgLeft < widgetLeft)
                bgLeft = widgetLeft;
            if (bgRight > widgetRight)
                bgRight = widgetRight;
            int bgColor = scaleAlpha(baseBg, vis);
            if (bgLeft < bgRight) {
                context.fill(bgLeft, bgTop, bgRight, bgBottom, bgColor);
            }

            // int borderColor = scaleAlpha(0xFF000000 | (CigaretteScreen.PRIMARY_COLOR & 0x00FFFFFF), vis);
            int borderColor = Color.colorVertical(bgTop, bgLeft);
            int borderW = 2;
            int borderLeft = bgLeft;
            int borderRight = Math.min(bgRight, borderLeft + borderW);
            if (borderLeft < borderRight) {
                context.fill(borderLeft, bgTop, borderRight, bgBottom, borderColor);
            }

            context.drawText(tr, name, drawX, lineY, textColor, true);
            i++;
        }
    }

    private static int scaleAlpha(int argb, float scale01) {
        if (scale01 <= 0f)
            return argb & 0x00FFFFFF;
        if (scale01 >= 1f)
            return argb;
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        int na = Math.max(0, Math.min(255, Math.round(a * scale01)));
        return (na << 24) | (r << 16) | (g << 8) | b;
    }
}