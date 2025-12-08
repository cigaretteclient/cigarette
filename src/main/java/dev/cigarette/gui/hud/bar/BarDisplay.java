package dev.cigarette.gui.hud.bar;

import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.gui.Scissor;
import dev.cigarette.gui.hud.bar.api.BarWidget;
import dev.cigarette.gui.hud.bar.api.BarWidgetRegistry;
import dev.cigarette.helper.ColorHelper;
import dev.cigarette.helper.ShapeHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

import java.util.*;

public class BarDisplay extends ClickableWidget {
    private static class AnimState {
        float progress = 0f;
        float target = 0f;
    }

    private final Map<String, AnimState> anim = new HashMap<>();

    public static int BG_COLOR = ColorHelper.colorTransparentize(CigaretteScreen.PRIMARY_COLOR, 0.4f);
    public static int TARGET_ROW_HEIGHT = 24;
    public static int GLOBAL_PADDING = 6;
    public static int MAX_ROWS = 3;

    private static final int MIN_BAR_HEIGHT = 0;

    private int animStartWidth;
    private int animStartHeight;
    private int animTargetWidth;
    private int animTargetHeight;
    private boolean animatingSize = false;
    private int barExpansionTicks = 0;
    private static final int MAX_BAR_EXPANSION_TICKS = 10;

    public BarDisplay() {
        super(5, 5, 200, MIN_BAR_HEIGHT, Text.of("Bar Display"));
        this.setMessage(Text.literal("Bar Display"));
        this.setTooltip(Tooltip.of(Text.literal("Displays detected targets with smooth transitions.")));
        this.setFocused(true);
        this.animStartWidth = this.getWidth();
        this.animStartHeight = this.getHeight();
        this.animTargetWidth = this.getWidth();
        this.animTargetHeight = this.getHeight();
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        int width;
        int height;

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        ClientWorld world = mc.world;
        if (mc.player == null || tr == null || world == null || mc.currentScreen instanceof InventoryScreen) {
            anim.clear();
            return;
        }

        BarWidgetRegistry.ensureDefaultsRegistered();

        List<BarWidget> widgets = new ArrayList<>();
        Set<String> seenUuids = new HashSet<>();
        List<Class<?>> providerOrder = List.of(
            dev.cigarette.gui.hud.bar.providers.MurderMysteryProvider.class,
            dev.cigarette.gui.hud.bar.providers.BedwarsProvider.class,
            dev.cigarette.gui.hud.bar.providers.ZombiesProvider.class,
            dev.cigarette.gui.hud.bar.providers.NearbyPlayersProvider.class
        );
        for (Class<?> providerClass : providerOrder) {
            for (var provider : BarWidgetRegistry.providers()) {
                if (provider.getClass() == providerClass) {
                    List<BarWidget> temp = new ArrayList<>();
                    try { provider.collect(mc, world, tr, temp); } catch (Throwable ignored) {}
                    for (BarWidget w : temp) {
                        if (w == null) continue;
                        String id = w.id();
                        if (id == null || id.isEmpty()) continue;
                        if (seenUuids.contains(id)) continue;
                        seenUuids.add(id);
                        widgets.add(w);
                    }
                }
            }
        }

        Map<String, BarWidget> dedup = new LinkedHashMap<>();
        for (BarWidget w : widgets) {
            if (w == null) continue;
            String id = w.id();
            if (id == null || id.isEmpty()) continue;
            dedup.putIfAbsent(id, w);
        }
        List<BarWidget> collected = new ArrayList<>(dedup.values());

        collected.sort(Comparator.comparingDouble(BarWidget::sortKey));

        if (collected.size() > MAX_ROWS) {
            collected = new ArrayList<>(collected.subList(0, Math.max(0, MAX_ROWS)));
        }

        Set<String> visibleNow = new HashSet<>();
        for (BarWidget w : collected) {
            String id = w.id();
            visibleNow.add(id);
            AnimState st = anim.computeIfAbsent(id, k -> new AnimState());
            st.target = 1f;
        }
        for (Map.Entry<String, AnimState> e : anim.entrySet()) {
            if (!visibleNow.contains(e.getKey())) {
                e.getValue().target = 0f;
            }
        }

        final float speed = 0.12f;
        List<BarWidget> toRender = new ArrayList<>();
        Map<String, Float> visibility = new HashMap<>();
        for (BarWidget w : collected) {
            AnimState st = anim.get(w.id());
            if (st == null) continue;
            float diff = st.target - st.progress;
            if (Math.abs(diff) > 0.0001f) {
                float step = Math.signum(diff) * Math.min(Math.abs(diff), speed);
                st.progress = Math.max(0f, Math.min(1f, st.progress + step));
            }
            if (!(st.progress < 0.01f && st.target <= 0f)) {
                toRender.add(w);
                visibility.put(w.id(), st.progress);
            }
        }

        anim.entrySet().removeIf(e -> e.getValue().progress < 0.01f && e.getValue().target <= 0f);

        if (toRender.isEmpty()) return;

        int padX = Math.max(0, GLOBAL_PADDING);
        int vPad = Math.max(0, GLOBAL_PADDING);
        int rowGap = Math.max(0, GLOBAL_PADDING);
        int rowH = Math.max(TARGET_ROW_HEIGHT, tr.fontHeight + 6);
        int n = toRender.size();
        int rawHeight = vPad * 2 + n * rowH + Math.max(0, (n - 1)) * rowGap;
        int contentMax = toRender.stream().mapToInt(w -> w.measureWidth(tr, rowH, padX)).max().orElse(200);
        int desiredWidth = Math.max(200, contentMax + padX * 2);
        int desiredHeight = Math.max(MIN_BAR_HEIGHT, rawHeight);

        int scrH = mc.getWindow().getScaledHeight();
        int maxBarHeight = Math.max(1, scrH / 3);
        int clampedDesiredHeight = Math.min(desiredHeight, maxBarHeight);

        if (desiredWidth != animTargetWidth || clampedDesiredHeight != animTargetHeight) {
            animStartWidth = this.getWidth();
            animStartHeight = this.getHeight();
            animTargetWidth = desiredWidth;
            animTargetHeight = clampedDesiredHeight;
            barExpansionTicks = 0;
            animatingSize = true;
        }

        if (animatingSize) {
            barExpansionTicks = Math.min(barExpansionTicks + 1, MAX_BAR_EXPANSION_TICKS);
            float eased = (float) CigaretteScreen.easeOutExpo((float) barExpansionTicks / MAX_BAR_EXPANSION_TICKS);
            int animWidth = (int) (animStartWidth + (animTargetWidth - animStartWidth) * eased);
            int animHeight = (int) (animStartHeight + (animTargetHeight - animStartHeight) * eased);
            this.setWidth(animWidth);
            this.setHeight(animHeight);
            if (barExpansionTicks >= MAX_BAR_EXPANSION_TICKS) {
                animatingSize = false;
                this.setWidth(animTargetWidth);
                this.setHeight(animTargetHeight);
            }
        } else {
            this.setWidth(animTargetWidth);
            this.setHeight(animTargetHeight);
        }

        int scrW = mc.getWindow().getScaledWidth();
        this.setX((scrW / 2) - (this.getWidth() / 2));
        this.setY(10);

        int x = this.getX();
        int y = this.getY();
        width = this.getWidth();
        height = this.getHeight();

        Scissor.pushExclusive(context, x, y, x + width, y + height);
        int bgCol = BG_COLOR;
        ShapeHelper.roundedRect(context, x, y, x + width, y + height, bgCol, 6);

        int innerLeft = x + padX;
        int innerRight = x + width - padX;
        int fullRowWidth = Math.max(0, innerRight - innerLeft);
        int startY = y + vPad;
        for (int i = 0; i < n; i++) {
            BarWidget w = toRender.get(i);
            float vis = visibility.getOrDefault(w.id(), 1f);
            int top = startY + i * (rowH + rowGap);
            if (top >= y + height) break;
            w.render(context, innerLeft, top, fullRowWidth, rowH, vis, tr);
        }

        Scissor.popExclusive();
    }

    public static float angle(float playerYaw, double dx, double dz) {
        double angleToTarget = Math.toDegrees(Math.atan2(dx, dz));
        float relativeYaw = (float) (angleToTarget - playerYaw);
        while (relativeYaw < -180) relativeYaw += 360;
        while (relativeYaw >= 180) relativeYaw -= 360;
        return relativeYaw;
    }

    public static float angle(MinecraftClient mc, double dx, double dz) {
        if (mc == null || mc.player == null) return 0f;
        return angle(mc.player.getYaw(), dx, dz);
    }

    public static Text nc(Entity entity, Text displayName, ClientWorld world) {
        if (entity == null || displayName == null) return Text.empty();
        if (entity instanceof net.minecraft.entity.player.PlayerEntity player) {
            if (world != null) {
                for (Entity target : world.getOtherEntities(entity, entity.getBoundingBox().expand(2.2, 0, 2.2))) {
                    if (!(target instanceof net.minecraft.entity.decoration.ArmorStandEntity)) continue;
                    Text standDisplayName = target.getDisplayName();
                    if (standDisplayName != null && !standDisplayName.getString().isEmpty()) {
                        return standDisplayName;
                    }
                }
            }
        }
        return displayName;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) { }
}
