package io.github.waqfs.module.bedwars;

import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.waqfs.GameDetector;
import io.github.waqfs.gui.widget.SliderWidget;
import io.github.waqfs.gui.widget.TextWidget;
import io.github.waqfs.gui.widget.ToggleColorWidget;
import io.github.waqfs.gui.widget.ToggleOptionsWidget;
import io.github.waqfs.lib.Glow;
import io.github.waqfs.lib.Raycast;
import io.github.waqfs.lib.Renderer;
import io.github.waqfs.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.HashSet;

public class ProjectileESP extends RenderModule {
    protected static final String MODULE_NAME = "ProjectileESP";
    protected static final String MODULE_TOOLTIP = "Displays the trajectory of all projectiles.";
    protected static final String MODULE_ID = "bedwars.projectileesp";
    private static final RenderLayer RENDER_LAYER = RenderLayer.of("cigarette.blockespnophase", 1536, Renderer.BLOCK_ESP_NOPHASE, RenderLayer.MultiPhaseParameters.builder().build(false));
    private final HashSet<Projectile> projectiles = new HashSet<>();
    private final Glow.Context glowContext = new Glow.Context();
    private final ToggleOptionsWidget enableGlow = new ToggleOptionsWidget(Text.literal("Glowing"), Text.literal("Applies the glowing effect to the entities in the same color as the trajectory.")).withDefaultState(true);
    private final ToggleColorWidget customHitColor = new ToggleColorWidget(Text.literal("Hit Color"), Text.literal("Overrides the glow and trajectory color if the projectile is colliding with an entity."), true).withDefaultColor(0xFFFF0000).withDefaultState(true);
    private final ToggleColorWidget enableArrows = new ToggleColorWidget(Text.literal("Shot Arrows"), Text.literal("Display the trajectory of shot Arrows."), true).withDefaultColor(0xFF0000FF).withDefaultState(true);
    private final ToggleColorWidget enablePearls = new ToggleColorWidget(Text.literal("Thrown Pearls"), Text.literal("Display the trajectory of thrown Pearls."), true).withDefaultColor(0xFF00FF00).withDefaultState(true);
    private final ToggleColorWidget enableSnowballs = new ToggleColorWidget(Text.literal("Thrown Snowballs"), Text.literal("Display the trajectory of thrown Snowballs."), true).withDefaultColor(0xFFFFFFFF).withDefaultState(true);
    private final ToggleColorWidget enableEggs = new ToggleColorWidget(Text.literal("Thrown Eggs"), Text.literal("Display the trajectory of thrown Eggs."), true).withDefaultColor(0xFFFFFF00).withDefaultState(true);
    private final SliderWidget maxTicks = new SliderWidget(Text.literal("Max Ticks"), Text.literal("The maximum ticks the projection calculates into the future.")).withBounds(20, 200, 200);

    public ProjectileESP() {
        super(MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        TextWidget header = new TextWidget(Text.literal("Types")).withUnderline();
        this.widget.setOptions(enableGlow, customHitColor, header, enableArrows, enablePearls, enableSnowballs, enableEggs, maxTicks);
        enableGlow.registerAsOption("bedwars.projectileesp.glow");
        customHitColor.registerAsOption("bedwars.projectileesp.collision");
        enableArrows.registerAsOption("bedwars.projectileesp.arrows");
        enablePearls.registerAsOption("bedwars.projectileesp.pearls");
        enableSnowballs.registerAsOption("bedwars.projectileesp.snowballs");
        enableEggs.registerAsOption("bedwars.projectileesp.eggs");
        maxTicks.registerAsOption("bedwars.projectileesp.maxticks");
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {
        matrixStack.push();

        Matrix4f matrix = Renderer.getCameraTranslatedMatrix(matrixStack, ctx);
        Tessellator tessellator = Tessellator.getInstance();

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (Projectile projectile : projectiles) {
            Raycast.SteppedTrajectory trajectory = projectile.trajectory;

            int collisionStep = trajectory.collisionStep != null ? trajectory.collisionStep : (int) maxTicks.getState();
            Vec3d start = trajectory.steps[0];
            for (int tick = 1; tick < maxTicks.getState(); tick++) {
                if (tick > collisionStep) break;
                Vec3d end = trajectory.steps[tick];
                Renderer.drawFakeLine(buffer, matrix, projectile.color, start.toVector3f(), end.toVector3f(), 0.1f);
                start = end;
            }
        }

        BuiltBuffer build = buffer.endNullable();
        if (build != null) RENDER_LAYER.draw(build);

        matrixStack.pop();
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        this.projectiles.clear();
        this.glowContext.removeAll();
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof ArrowEntity) && !(entity instanceof EggEntity) && !(entity instanceof SnowballEntity) && !(entity instanceof EnderPearlEntity)) continue;
            if (!entity.isPushedByFluids()) continue;
            if (entity instanceof ArrowEntity && !enableArrows.getToggleState()) continue;
            if (entity instanceof EnderPearlEntity && !enablePearls.getToggleState()) continue;
            if (entity instanceof SnowballEntity && !enableSnowballs.getToggleState()) continue;
            if (entity instanceof EggEntity && !enableEggs.getToggleState()) continue;

            Raycast.SteppedTrajectory trajectory = Raycast.trajectory((ProjectileEntity) entity, (int) maxTicks.getState());

            int color = 0xFFFFFFFF;
            if (trajectory.collision instanceof EntityHitResult && customHitColor.getToggleState()) color = customHitColor.getStateARGB();
            else if (entity instanceof ArrowEntity) color = enableArrows.getStateARGB();
            else if (entity instanceof EggEntity) color = enableEggs.getStateARGB();
            else if (entity instanceof EnderPearlEntity) color = enablePearls.getStateARGB();

            Projectile projectile = new Projectile(entity, trajectory, color);
            projectiles.add(projectile);
            if (this.enableGlow.getState()) this.glowContext.addGlow(entity.getUuid(), color & 0xFFFFFF);
        }
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        this.projectiles.clear();
        this.glowContext.removeAll();
    }

    @Override
    protected boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }

    private record Projectile(Entity entity, Raycast.SteppedTrajectory trajectory, int color) {
    }
}
