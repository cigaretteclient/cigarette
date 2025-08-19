package dev.cigarette.module.bedwars;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.cigarette.GameDetector;
import dev.cigarette.gui.widget.BaseWidget;
import dev.cigarette.gui.widget.ColorDropdownWidget;
import dev.cigarette.gui.widget.TextWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.gui.widget.*;
import dev.cigarette.lib.Glow;
import dev.cigarette.lib.Raycast;
import dev.cigarette.lib.Renderer;
import dev.cigarette.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.List;

public class FireballESP extends RenderModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "FireballESP";
    protected static final String MODULE_TOOLTIP = "Displays the trajectory and blast radius of all fireballs.";
    protected static final String MODULE_ID = "bedwars.fireballesp";
    private static final RenderLayer RENDER_LAYER = RenderLayer.of("cigarette.blockespnophase", 1536, Renderer.BLOCK_ESP_NOPHASE, RenderLayer.MultiPhaseParameters.builder().build(false));
    private static final RenderLayer RENDER_LAYER_SPHERE = RenderLayer.of("cigarette.triespnophase", 1536, Renderer.TRI_ESP_NOPHASE, RenderLayer.MultiPhaseParameters.builder().build(false));
    private final HashSet<Fireball> fireballs = new HashSet<>();
    private final Glow.Context glowContext = new Glow.Context();
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableGlow = ColorDropdownWidget.buildToggle(Text.literal("Glowing"), Text.literal("Applies the glowing effect to the fireball entities")).withAlpha(false).withDefaultColor(0xFFFF0000);
    private final ColorDropdownWidget<TextWidget, BaseWidget.Stateless> sphereColor = ColorDropdownWidget.buildText(Text.literal("Sphere Color"), null).withDefaultColor(0x4FFF0000);
    private final ColorDropdownWidget<TextWidget, BaseWidget.Stateless> lineColor = ColorDropdownWidget.buildText(Text.literal("Projection Color"), null).withDefaultColor(0xFFFF0000);

    public FireballESP() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        this.setChildren(enableGlow, sphereColor, lineColor);
        enableGlow.registerConfigKey("bedwars.fireballesp.glow");
        sphereColor.registerConfigKey("bedwars.fireballesp.spherecolor");
        lineColor.registerConfigKey("bedwars.fireballesp.linecolor");
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {
        matrixStack.push();

        Matrix4f matrix = Renderer.getCameraTranslatedMatrix(matrixStack, ctx);
        Tessellator tessellator = Tessellator.getInstance();

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (Fireball fireball : fireballs) {
            if (fireball.collisionNearPlayer && fireball.triangles != null) {
                Renderer.drawSphere(buffer, matrix, sphereColor.getStateARGB(), fireball.triangles);
            }
        }

        BuiltBuffer build = buffer.endNullable();
        if (build != null) RENDER_LAYER_SPHERE.draw(build);

        buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (Fireball fireball : fireballs) {
            Renderer.drawFakeLine(buffer, matrix, lineColor.getStateARGB(), fireball.collisionPathStart.toVector3f(), fireball.collisionPathEnd.toVector3f(), 0.1f);
        }

        build = buffer.endNullable();
        if (build != null) RENDER_LAYER.draw(build);

        matrixStack.pop();
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        this.fireballs.clear();
        this.glowContext.removeAll();
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof FireballEntity entityfb)) continue;

            Vec3d velocity = entityfb.getVelocity();
            Vec3d start = entityfb.getPos();
            Vec3d end = start.add(velocity.multiply(1000));

            HitResult result = Raycast.raycast(start, end, entityfb);

            Fireball fireball = null;
            switch (result.getType()) {
                case BLOCK, ENTITY -> {
                    Vec3d collisionEnd = result.getPos();
                    double timeToCollision = start.distanceTo(collisionEnd) / velocity.length();
                    boolean nearPlayer = collisionEnd.distanceTo(player.getPos()) < 100;
                    fireball = new Fireball(entityfb, timeToCollision, start, collisionEnd, nearPlayer, Renderer.calculateSphere(collisionEnd, 8.4f, player.getEyePos()));
                }
                case MISS -> {
                    fireball = new Fireball(entityfb, -1, start, end, false, null);
                }
            }
            fireballs.add(fireball);
            if (enableGlow.getToggleState()) glowContext.addGlow(entityfb.getUuid(), enableGlow.getStateRGB());
        }
        ItemStack heldItem = player.getInventory().getSelectedStack();
        if (heldItem.isOf(Items.FIRE_CHARGE)) {
            Entity camera = client.getCameraEntity();
            if (camera == null) return;

            Vec3d start = player.getEyePos();
            Vec3d end = start.add(camera.getRotationVector().multiply(1000));
            HitResult result = Raycast.raycast(start, end, ShapeContext.absent());

            switch (result.getType()) {
                case BLOCK, ENTITY -> {
                    Vec3d collisionEnd = result.getPos();
                    fireballs.add(new Fireball(null, -1, collisionEnd, collisionEnd, true, Renderer.calculateSphere(collisionEnd, 8.4f, player.getEyePos())));
                }
                default -> {}
            }
        }
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        this.fireballs.clear();
        this.glowContext.removeAll();
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }

    private static class Fireball {
        public final @Nullable FireballEntity entity;

        public final double time;

        public final Vec3d collisionPathStart;
        public final Vec3d collisionPathEnd;
        public final boolean collisionNearPlayer;
        public final @Nullable List<Vec3d[]> triangles;

        public Fireball(@Nullable FireballEntity entity, double time, Vec3d start, Vec3d end, boolean nearPlayer, @Nullable List<Vec3d[]> triangles) {
            this.entity = entity;
            this.time = time;
            this.collisionPathStart = start;
            this.collisionPathEnd = end;
            this.collisionNearPlayer = nearPlayer;
            this.triangles = triangles;
        }
    }
}
