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
import org.jetbrains.annotations.NotNull;

public class ReviveAura extends RenderModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "Revive Aura";
    protected static final String MODULE_TOOLTIP = "Automatically revives downed teammates.";
    protected static final String MODULE_ID = "zombies.revive_aura";

    public ReviveAura() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
    }

    /**
     * @param ctx  World Render Context
     * @param matrixStack Matrix Stack
     */
    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {

    }

    /**
     * @param client Minecraft Client
     * @param world Client World
     * @param player Client Player
     */
    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof ArmorStandEntity armorStandEntity)) continue;
            System.out.println(armorStandEntity);
        }
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {}

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.ZOMBIES && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_ZOMBIES;
    }
}
