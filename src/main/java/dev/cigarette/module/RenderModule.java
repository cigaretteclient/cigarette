package dev.cigarette.module;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.widget.BaseWidget;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class RenderModule<Widget extends BaseWidget<Boolean>, Boolean> extends TickModule<Widget, Boolean> {
    public RenderModule(WidgetGenerator<Widget, Boolean> func, String key, String displayName, @Nullable String tooltip) {
        super(func, key, displayName, tooltip);
        net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            if (!this.getRawState()) return;
            if (!this._inValidGame()) return;
            MatrixStack matrixStack = ctx.matrices();
            this.onWorldRender(ctx, matrixStack);
        });
    }

    private boolean _inValidGame() {
        if (Cigarette.IN_DEV_ENVIRONMENT) return true;
        return this.inValidGame();
    }

    protected abstract void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack);
}
