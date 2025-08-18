package io.github.waqfs.module.zombies;

import io.github.waqfs.GameDetector;
import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

public class PowerupESP extends RenderModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "PowerupESP";
    protected static final String MODULE_TOOLTIP = "Places ESP boxes around dropped powerups.";
    protected static final String MODULE_ID = "zombies.powerupesp";
    private final HashSet<Powerup> powerups = new HashSet<>();

    public PowerupESP() {
        super(ToggleWidget::module, MODULE_NAME, MODULE_TOOLTIP, MODULE_ID);
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        powerups.clear();
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof ArmorStandEntity armorStandEntity)) continue;
        }
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        powerups.clear();
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.ZOMBIES && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_ZOMBIES;
    }

    protected record Powerup(Vec3d position, Powerup.Type type) {
        enum Type {
            INSTANT_KILL(0xFFFF0000), MAX_AMMO(0xFF0000FF), DOUBLE_GOLD(0xFFFFF800);

            private final int color;

            Type(int argb) {
                this.color = argb;
            }

            public int getColor() {
                return this.color;
            }
        }
    }
}
