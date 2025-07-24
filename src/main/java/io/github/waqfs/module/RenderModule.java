package io.github.waqfs.module;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class RenderModule extends TickModule {
    public RenderModule(String key, String displayName, @Nullable String tooltip) {
        super(key, displayName, tooltip);
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(ctx -> {
            if (!this.state) return;
            if (!this.inValidGame()) return;
            MatrixStack matrixStack = ctx.matrixStack();
            if (matrixStack == null) return;
            this.onWorldRender(ctx, matrixStack);
        });
    }

    protected abstract void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack);
}
