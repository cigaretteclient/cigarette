package dev.cigarette.module.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.cigarette.gui.widget.ColorDropdownWidget;
import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.TextWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.lib.Glow;
import dev.cigarette.lib.Raycast;
import dev.cigarette.lib.Renderer;
import dev.cigarette.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.HashSet;

public class ProjectileESP extends RenderModule<ToggleWidget, Boolean> {
    public static final ProjectileESP INSTANCE = new ProjectileESP("render.projectileesp", "ProjectileESP", "Displays the trajectory of all projectiles.");

    // private static final RenderLayer RENDER_LAYER = RenderLayer.of("cigarette.blockespnophase", 1536, Renderer.BLOCK_ESP_NOPHASE, RenderLayer.MultiPhaseParameters.builder().build(false));
    private static final RenderLayer RENDER_LAYER = RenderLayer.of("cigarette.blockespnophase",
        RenderSetup.builder(Renderer.BLOCK_ESP_NOPHASE)
            .build()
    );
    private final HashSet<Projectile> projectiles = new HashSet<>();
    private final Glow.Context glowContext = new Glow.Context();

    private final ToggleWidget enableGlow = new ToggleWidget("Glowing", "Applies the glowing effect to the entities in the same color as the trajectory.").withDefaultState(true);
    private final ToggleWidget enablePrefire = new ToggleWidget("Show Prefire", "Shows trajectories while players are holding projectiles before they are shot.").withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> customHitColor = ColorDropdownWidget.buildToggle("Hit Color", "Overrides the glow and trajectory color if the projectile is colliding with an entity.").withDefaultColor(0xFFFF0000).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableArrows = ColorDropdownWidget.buildToggle("Shot Arrows", "Display the trajectory of shot Arrows.").withDefaultColor(0xFF0000FF).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enablePearls = ColorDropdownWidget.buildToggle("Thrown Pearls", "Display the trajectory of thrown Pearls.").withDefaultColor(0xFF00FF00).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableSnowballs = ColorDropdownWidget.buildToggle("Thrown Snowballs", "Display the trajectory of thrown Snowballs.").withDefaultColor(0xFFFFFFFF).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableEggs = ColorDropdownWidget.buildToggle("Thrown Eggs", "Display the trajectory of thrown Eggs.").withDefaultColor(0xFFFFFF00).withDefaultState(true);
    private final SliderWidget maxTicks = new SliderWidget("Max Ticks", "The maximum ticks the projection calculates into the future.").withBounds(20, 200, 200);

    private ProjectileESP(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        TextWidget header = new TextWidget("Types").withUnderline();
        this.setChildren(enableGlow, enablePrefire, customHitColor, header, enableArrows, enablePearls, enableSnowballs, enableEggs, maxTicks);
        enableGlow.registerConfigKey(id + ".glow");
        enablePrefire.registerConfigKey(id + ".prefire");
        customHitColor.registerConfigKey(id + ".collision");
        enableArrows.registerConfigKey(id + ".arrows");
        enablePearls.registerConfigKey(id + ".pearls");
        enableSnowballs.registerConfigKey(id + ".snowballs");
        enableEggs.registerConfigKey(id + ".eggs");
        maxTicks.registerConfigKey(id + ".maxticks");
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {
        matrixStack.push();

        Matrix4f matrix = Renderer.getCameraTranslatedMatrix(matrixStack, ctx);
        Tessellator tessellator = Tessellator.getInstance();

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (Projectile projectile : projectiles) {
            Raycast.SteppedTrajectory trajectory = projectile.trajectory;

            int collisionStep = trajectory.collisionStep != null ? trajectory.collisionStep : maxTicks.getRawState().intValue();
            Vec3d start = trajectory.steps[0];
            for (int tick = 1; tick < trajectory.steps.length; tick++) {
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
            if (!(entity instanceof ArrowEntity) && !(entity instanceof EggEntity) && !(entity instanceof SnowballEntity) && !(entity instanceof EnderPearlEntity)) {
                if (!enablePrefire.getRawState()) continue;
                if (!(entity instanceof PlayerEntity playerEntity)) continue;

                ItemStack holding = playerEntity.getMainHandStack();
                if (holding.isOf(Items.BOW) || holding.isOf(Items.SNOWBALL) || holding.isOf(Items.EGG) || holding.isOf(Items.ENDER_PEARL)) {
                    Raycast.SteppedTrajectory trajectory = Raycast.trajectory(playerEntity, holding, maxTicks.getRawState().intValue());
                    if (trajectory == null) continue;

                    int color = 0xFFFFFFFF;
                    if (trajectory.collision instanceof EntityHitResult && customHitColor.getToggleState()) color = customHitColor.getStateARGB();
                    else if (holding.isOf(Items.BOW)) color = enableArrows.getStateARGB();
                    else if (holding.isOf(Items.EGG)) color = enableEggs.getStateARGB();
                    else if (holding.isOf(Items.ENDER_PEARL)) color = enablePearls.getStateARGB();
                    else if (holding.isOf(Items.SNOWBALL)) color = enableSnowballs.getStateARGB();

                    Projectile projectile = new Projectile(null, trajectory, color);
                    projectiles.add(projectile);
                }

                continue;
            }
            if (!entity.isPushedByFluids()) continue;
            if (entity instanceof ArrowEntity && !enableArrows.getToggleState()) continue;
            if (entity instanceof EnderPearlEntity && !enablePearls.getToggleState()) continue;
            if (entity instanceof SnowballEntity && !enableSnowballs.getToggleState()) continue;
            if (entity instanceof EggEntity && !enableEggs.getToggleState()) continue;

            Raycast.SteppedTrajectory trajectory = Raycast.trajectory((ProjectileEntity) entity, maxTicks.getRawState().intValue());

            int color = 0xFFFFFFFF;
            if (trajectory.collision instanceof EntityHitResult && customHitColor.getToggleState()) color = customHitColor.getStateARGB();
            else if (entity instanceof ArrowEntity) color = enableArrows.getStateARGB();
            else if (entity instanceof EggEntity) color = enableEggs.getStateARGB();
            else if (entity instanceof EnderPearlEntity) color = enablePearls.getStateARGB();

            Projectile projectile = new Projectile(entity, trajectory, color);
            projectiles.add(projectile);
            if (this.enableGlow.getRawState()) this.glowContext.addGlow(entity.getUuid(), color & 0xFFFFFF);
        }
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        this.projectiles.clear();
        this.glowContext.removeAll();
    }

    private record Projectile(Entity entity, Raycast.SteppedTrajectory trajectory, int color) {
    }
}
