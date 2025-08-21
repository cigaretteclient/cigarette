package dev.cigarette.gui.hud.bar;

import dev.cigarette.agent.MurderMysteryAgent;
import dev.cigarette.agent.ZombiesAgent;
import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.gui.RenderUtil;
import dev.cigarette.gui.Scissor;
import dev.cigarette.gui.hud.bar.api.BarWidget;
import dev.cigarette.gui.hud.bar.api.BarWidgetRegistry;
import dev.cigarette.lib.Color;
import dev.cigarette.lib.Shape;
import dev.cigarette.lib.WorldL;
import dev.cigarette.module.murdermystery.PlayerESP;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BarDisplay extends ClickableWidget {
    private static class AnimState {
        float progress = 0f;
        float target = 0f;
    }

    private final Map<String, AnimState> anim = new HashMap<>();

    private static final int TARGET_ROW_HEIGHT = 24;
    private static final int MIN_BAR_HEIGHT = 40;

    private int lastHeight = MIN_BAR_HEIGHT;
    private int lastWidth = 0;
    private boolean doBarExpansion = false;

    private int barExpansionTicks = 0;
    private static final int MAX_BAR_EXPANSION_TICKS = 10;

    public BarDisplay() {
        super(5, 5, 200, MIN_BAR_HEIGHT, Text.of("Bar Display"));
        this.setMessage(Text.literal("Bar Display"));
        this.setTooltip(Tooltip.of(Text.literal("Displays detected targets with smooth transitions.")));
        this.setFocused(true);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        int width = this.getWidth();
        int height = this.getHeight();

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        ClientWorld world = mc.world;

        if (world == null) {
            anim.clear();
            return;
        }

        BarWidgetRegistry.ensureDefaultsRegistered();

        List<BarWidget> widgets = new ArrayList<>();
        for (var provider : BarWidgetRegistry.providers()) {
            try { provider.collect(mc, world, tr, widgets); } catch (Throwable ignored) {}
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

        int padX = 6;
        int vPad = 8;
        int rowGap = 4;
        int rowH = Math.max(TARGET_ROW_HEIGHT, tr.fontHeight + 6);
        int maxW = Math.max(200, mc.getWindow().getScaledWidth() - 20);

        List<Integer> widths = new ArrayList<>(toRender.size());
        for (BarWidget w : toRender) widths.add(Math.max(24, w.measureWidth(tr, rowH, padX)));

        List<List<Integer>> rowsIdx = new ArrayList<>();
        List<Integer> rowWidths = new ArrayList<>();
        int curRow = -1;
        int curWidth = 0;
        for (int i = 0; i < toRender.size(); i++) {
            int w = widths.get(i);
            int needed = (curRow < 0 ? 0 : 6) + w;
            if (curRow < 0 || curWidth + needed > maxW - 12) {
                rowsIdx.add(new ArrayList<>());
                rowWidths.add(0);
                curRow++;
                curWidth = 0;
                needed = w;
            }
            rowsIdx.get(curRow).add(i);
            curWidth += (curWidth == 0 ? w : (6 + w));
            rowWidths.set(curRow, curWidth);
        }

        int rows = rowsIdx.size();
        int rawHeight = vPad * 2 + rows * rowH + (rows - 1) * rowGap;
        int capH = mc.getWindow().getScaledHeight() / 3;
        int maxRows = Math.max(1, Math.min(rows, (capH - vPad * 2 + rowGap) / (rowH + rowGap)));
        if (maxRows < rows) {
            rowsIdx = rowsIdx.subList(0, maxRows);
            rowWidths = rowWidths.subList(0, maxRows);
            rows = maxRows;
            rawHeight = vPad * 2 + rows * rowH + (rows - 1) * rowGap;
        }
        List<Integer> finalRowWidths = rowWidths;
        List<List<Integer>> finalRowsIdx = rowsIdx;
        int desiredWidth = Math.max(200, Math.min(maxW, 12 + rowsIdx.stream().mapToInt(iList -> finalRowWidths.get(finalRowsIdx.indexOf(iList))).max().orElse(200)));
        int desiredHeight = Math.max(MIN_BAR_HEIGHT, rawHeight);

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
        float eased = (float) CigaretteScreen.easeOutExpo((float) barExpansionTicks / MAX_BAR_EXPANSION_TICKS);
        int animWidth = doBarExpansion ? (int) (width + (desiredWidth - width) * eased) : desiredWidth;
        int animHeight = doBarExpansion ? (int) (height + (desiredHeight - height) * eased) : desiredHeight;

        this.setWidth(animWidth);
        this.setHeight(animHeight);

        int scrW = mc.getWindow().getScaledWidth();
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

        int startY = y + vPad;
        for (int r = 0; r < rowsIdx.size(); r++) {
            List<Integer> row = rowsIdx.get(r);
            int rowW = rowWidths.get(r);
            int startX = x + Math.max(6, (width - rowW) / 2);
            int cx = startX;
            for (int idx : row) {
                BarWidget w = toRender.get(idx);
                int wWidth = widths.get(idx);
                float vis = visibility.getOrDefault(w.id(), 1f);
                int top = startY + r * (rowH + rowGap);
                w.render(context, cx, top, wWidth, rowH, vis, tr);
                cx += wWidth + 6;
                if (cx >= x + width - 6) break;
            }
        }

        if (doBarExpansion) RenderUtil.popOpacity();
        Scissor.popExclusive();
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

    private static String prettyZombieName(ZombiesAgent.ZombieType type) {
        if (type == null) return "Zombie";
        return switch (type) {
            case ZOMBIE -> "Zombie";
            case BLAZE -> "Blaze";
            case WOLF -> "Wolf";
            case SKELETON -> "Skeleton";
            case CREEPER -> "Creeper";
            case MAGMACUBE -> "Magma Cube";
            case SLIME -> "Slime";
            case WITCH -> "Witch";
            case ENDERMITE -> "Endermite";
            case SILVERFISH -> "Silverfish";
            case UNKNOWN -> "Unknown";
        };
    }

    private static int defaultZombieColor(ZombiesAgent.ZombieType type) {
        if (type == null) return 0xFFFFFFFF;
        return switch (type) {
            case ZOMBIE -> 0xFF2C936C;
            case BLAZE -> 0xFFFCA50F;
            case WOLF -> 0xFF3FE6FC;
            case SKELETON -> 0xFFE0E0E0;
            case CREEPER, SLIME -> 0xFF155B0D;
            case MAGMACUBE -> 0xFFFC4619;
            case WITCH, ENDERMITE -> 0xFFA625F7;
            case SILVERFISH -> 0xFF3F3F3F;
            case UNKNOWN -> 0xFFFFFFFF;
        };
    }

    private static float calculateRelativeYawToPos(Vec3d targetPos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return 0f;
        Vec3d playerPos = mc.player.getPos();
        double dx = targetPos.x - playerPos.x;
        double dz = targetPos.z - playerPos.z;
        return angle(mc.player.getYaw(), dx, dz);
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

    @Nullable
    private static Entity findEntityByUuid(ClientWorld world, UUID uuid) {
        if (world == null || uuid == null) return null;
        for (Entity e : world.getEntities()) {
            if (uuid.equals(e.getUuid())) return e;
        }
        return null;
    }

    private static int getEntityNameColor(Entity entity) {
        if (entity == null) return 0;
        Text displayName = entity.getDisplayName();
        if (entity instanceof IronGolemEntity) {
            ClientWorld world = MinecraftClient.getInstance().world;
            if (world != null) {
                displayName = nc(entity, displayName, world);
            }
        }
        if (displayName == null) return 0;
        List<Text> siblings = displayName.getSiblings();
        for (Text sibling : siblings) {
            TextColor color = sibling.getStyle().getColor();
            if (color == null) continue;
            if ("dark_gray".equals(color.getName())) continue;
            if ("gray".equals(color.getName())) continue;
            return color.getRgb();
        }
        return 0;
    }

    public static Text nc(Entity entity, Text displayName, ClientWorld world) {
        Box box = Box.of(entity.getPos(), 0, 2.2, 0);
        for (Entity target : world.getOtherEntities(entity, box)) {
            if (!(target instanceof ArmorStandEntity)) continue;
            Text standDisplayName = target.getDisplayName();
            if (standDisplayName == null) continue;
            displayName = standDisplayName;
        }
        return displayName;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) { }
}
