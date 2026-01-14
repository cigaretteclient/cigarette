package dev.cigarette.module.bedwars;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.cigarette.GameDetector;
import dev.cigarette.gui.widget.BaseWidget;
import dev.cigarette.gui.widget.ColorDropdownWidget;
import dev.cigarette.gui.widget.TextWidget;
import dev.cigarette.gui.widget.ToggleWidget;
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
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.List;

public class FireballESP extends RenderModule<ToggleWidget, Boolean> {
    public static final FireballESP INSTANCE = new FireballESP("bedwars.fireballesp", "FireballESP", "Displays the trajectory and blast radius of all fireballs.");

    /*
    
    private static final RenderLayer RENDER_LAYER = RenderLayer.of(
        "cigarette.blockesp", 
        1536,
        Renderer.BLOCK_ESP_PHASE,
        RenderLayer.MultiPhaseParameters.builder()
            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING) 
            .target(RenderPhase.MAIN_TARGET)
            .lineWidth(RenderPhase.FULL_LINE_WIDTH)
            .texture(RenderPhase.NO_TEXTURE)
            .build(false)
    ); */
    private static final RenderLayer RENDER_LAYER = RenderLayer.of(
        "cigarette.block_esp_nophase",
        1536,
        Renderer.BLOCK_ESP_NOPHASE,
        RenderLayer.MultiPhaseParameters.builder()
            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
            .target(RenderPhase.MAIN_TARGET)
            .lineWidth(RenderPhase.FULL_LINE_WIDTH)
            .texture(RenderPhase.NO_TEXTURE)
            .build(false)
    );
    private static final RenderLayer RENDER_LAYER_SPHERE = RenderLayer.of("cigarette.triespnophase",
        1536,
        Renderer.TRI_ESP_NOPHASE,
        RenderLayer.MultiPhaseParameters.builder()
            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
            .target(RenderPhase.MAIN_TARGET)
            .texture(RenderPhase.NO_TEXTURE)
            .build(false)
    );
    private final HashSet<Fireball> fireballs = new HashSet<>();
    private final Glow.Context glowContext = new Glow.Context();

    private final ColorDropdownWidget<ToggleWidget, Boolean> enableGlow = ColorDropdownWidget.buildToggle("Glowing", "Applies the glowing effect to the fireball entities").withAlpha(false).withDefaultColor(0xFFFF0000);
    private final ColorDropdownWidget<TextWidget, BaseWidget.Stateless> sphereColor = ColorDropdownWidget.buildText("Sphere Color", null).withDefaultColor(0x4FFF0000);
    private final ColorDropdownWidget<TextWidget, BaseWidget.Stateless> lineColor = ColorDropdownWidget.buildText("Projection Color", null).withDefaultColor(0xFFFF0000);

    private FireballESP(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.setChildren(enableGlow, sphereColor, lineColor);
        enableGlow.registerConfigKey(id + ".glow");
        sphereColor.registerConfigKey(id + ".spherecolor");
        lineColor.registerConfigKey(id + ".linecolor");
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
