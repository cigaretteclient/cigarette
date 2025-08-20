package dev.cigarette.gui.hud.bar;

import dev.cigarette.agent.MurderMysteryAgent;
import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.gui.RenderUtil;
import dev.cigarette.gui.Scissor;
import dev.cigarette.lib.Color;
import dev.cigarette.lib.Shape;
import dev.cigarette.module.murdermystery.PlayerESP;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.*;

public class BarDisplay extends ClickableWidget {
    private static class Entry {
        final UUID uuid;
        MurderMysteryAgent.PersistentPlayer player;
        String name;
        int width;
        float progress;
        float target;
        int lastIndex;
        float degrees;

        Entry(UUID uuid, MurderMysteryAgent.PersistentPlayer player, String name, int width, float degrees) {
            this.uuid = uuid;
            this.player = player;
            this.name = name;
            this.width = width;
            this.progress = 0f;
            this.target = 1f;
            this.lastIndex = 0;
            this.degrees = degrees;
        }
    }

    private final Map<UUID, Entry> entries = new HashMap<>();

    private static final int TARGET_HEIGHT = 24; // chip height inside bar
    private static final int MIN_BAR_HEIGHT = 40;

    private int lastHeight = MIN_BAR_HEIGHT;
    private int lastWidth = 0;
    private boolean doBarExpansion = false;

    private int barExpansionTicks = 0;
    private static final int MAX_BAR_EXPANSION_TICKS = 10;

    public BarDisplay() {
        super(5, 5, 200, MIN_BAR_HEIGHT, Text.of("Bar Display"));
        this.setMessage(Text.literal("Bar Display"));
        this.setTooltip(Tooltip.of(Text.literal("Displays detected murderers with smooth transitions.")));
        this.setFocused(true);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        int width = this.getWidth();
        int height = this.getHeight();

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

        // Only consider visible players that are detected as murderers
        Set<MurderMysteryAgent.PersistentPlayer> persistentPlayers = MurderMysteryAgent.getVisiblePlayers();

        Set<UUID> visibleNow = new HashSet<>();
        List<Entry> working = new ArrayList<>();
        for (MurderMysteryAgent.PersistentPlayer pp : persistentPlayers) {
            if (pp == null || pp.playerEntity == null) continue;
            if (pp.role != MurderMysteryAgent.PersistentPlayer.Role.MURDERER) continue;
            UUID uuid = pp.playerEntity.getUuid();
            visibleNow.add(uuid);
            String name = pp.playerEntity.getName().getString();
            int w = tr.getWidth(name);
            float deg = 0f;
            try {
                deg = PlayerESP.calculateRelativeYaw(pp.playerEntity);
            } catch (Throwable ignored) {}

            Entry e = entries.get(uuid);
            if (e == null) {
                e = new Entry(uuid, pp, name, w, deg);
                entries.put(uuid, e);
            } else {
                e.player = pp;
                e.name = name;
                e.width = w;
                e.degrees = deg;
            }
            e.target = 1f;
            working.add(e);
        }

        List<Entry> workingSorted = new ArrayList<>(working);
        workingSorted.sort(Comparator.comparingDouble(a -> a.degrees));
        int ord = 0;
        for (Entry e : workingSorted) {
            e.lastIndex = ord++;
        }

        for (Entry e : entries.values()) {
            if (!visibleNow.contains(e.uuid)) {
                e.target = 0f;
            }
        }

        List<Entry> toRender = new ArrayList<>();
        final float speed = 0.12f;
        for (Entry e : entries.values()) {
            float diff = e.target - e.progress;
            if (Math.abs(diff) > 0.0001f) {
                float step = Math.signum(diff) * Math.min(Math.abs(diff), speed);
                e.progress += step;
            }
            if (e.progress < 0f) e.progress = 0f;
            if (e.progress > 1f) e.progress = 1f;
            if (!(e.progress < 0.01f && e.target <= 0f)) {
                toRender.add(e);
            }
        }

        entries.values().removeIf(e -> e.progress < 0.01f && e.target <= 0f);

        toRender.sort(Comparator.comparingInt(a -> a.lastIndex));

        if (toRender.isEmpty()) {
            return;
        }

        int desiredHeight = MIN_BAR_HEIGHT;
        int desiredWidth = Math.max(200, computeRequiredWidth(toRender));
        if (height != lastHeight || width != lastWidth || desiredWidth != width || desiredHeight != height) {
            barExpansionTicks = 0;
            doBarExpansion = true;
        }

        if (doBarExpansion) {
            barExpansionTicks = Math.min(barExpansionTicks + 1, MAX_BAR_EXPANSION_TICKS);
            if (barExpansionTicks == MAX_BAR_EXPANSION_TICKS) {
                doBarExpansion = false;
                lastHeight = desiredHeight;
                lastWidth = desiredWidth;
            }
        }

        float eased = (float) barExpansionTicks / MAX_BAR_EXPANSION_TICKS;
        eased = (float) CigaretteScreen.easeOutExpo(eased);
        int animWidth = doBarExpansion ? (int) (width + (desiredWidth - width) * eased) : desiredWidth;
        int animHeight = doBarExpansion ? (int) (height + (desiredHeight - height) * eased) : desiredHeight;

        this.setWidth(animWidth);
        this.setHeight(animHeight);

        int scrW = MinecraftClient.getInstance().getWindow().getScaledWidth();
        this.setX((scrW / 2) - (animWidth / 2));
        this.setY(10);

        int x = this.getX();
        int y = this.getY();
        width = this.getWidth();
        height = this.getHeight();

        Scissor.pushExclusive(context, x, y, x + width, y + height);
        int bgCol = Color.colorTransparentize(CigaretteScreen.PRIMARY_COLOR, 0.4f);
        Shape.roundedRect(context, x, y, x + width, y + height, bgCol, 6);

        if (doBarExpansion) RenderUtil.pushOpacity(eased);

        int padX = 6;
        int padY = 2;
        int chipH = Math.min(TARGET_HEIGHT, height - 8);
        chipH = Math.max(chipH, tr.fontHeight + padY * 2);

        int spacing = 6;
        int totalWidth = 0;
        for (Entry e : toRender) {
            totalWidth += (e.width + padX * 2) + spacing;
        }
        if (totalWidth > 0) totalWidth -= spacing;
        int startX = x + Math.max(6, (width - totalWidth) / 2);
        int centerY = y + (height - chipH) / 2;

        int cx = startX;
        final int textColor = (CigaretteScreen.PRIMARY_TEXT_COLOR & 0x00FFFFFF) | 0xFF000000;
        for (Entry e : toRender) {
            float vis = Math.max(0f, Math.min(1f, e.progress));
            int slideY = Math.round((1f - vis) * 10f);
            int chipW = e.width + padX * 2;

            int left = cx;
            int right = Math.min(x + width - 6, left + chipW);
            int top = centerY + slideY;
            int bottom = top + chipH;
            if (right <= left) break;

            int bga = scaleAlpha(0x90000000, vis);
            context.fill(left, top, right, bottom, bga);

            // Match ModuleListDisplay's parameter order (y, x)
            int borderColor = Color.colorVertical(top, left);
            int borderW = 2;
            int bRight = Math.min(right, left + borderW);
            if (left < bRight) context.fill(left, top, bRight, bottom, borderColor);

            int triSize = Math.min(6, chipH / 3);
            if (triSize >= 3) {
                int tx = right - triSize - 2;
                int ty = top + (chipH - triSize) / 2;
                int triCol = scaleAlpha((CigaretteScreen.SECONDARY_COLOR & 0x00FFFFFF) | 0xFF000000, vis);
                context.fill(tx, ty, tx + triSize, ty + triSize, triCol);
            }

            int textX = left + padX;
            int textY = top + (chipH - tr.fontHeight) / 2;
            context.drawText(tr, e.name, textX, textY, textColor, true);

            cx = right + spacing;
            if (cx >= x + width - 6) break;
        }

        if (doBarExpansion) RenderUtil.popOpacity();
        Scissor.popExclusive();
    }

    private static int computeRequiredWidth(List<Entry> toRender) {
        int padX = 6;
        int spacing = 6;
        int totalWidth = 0;
        for (Entry e : toRender) {
            totalWidth += (e.width + padX * 2) + spacing;
        }
        if (totalWidth > 0) totalWidth -= spacing;
        // Ensure a minimum width
        return Math.max(200, totalWidth + 12);
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

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }
}
