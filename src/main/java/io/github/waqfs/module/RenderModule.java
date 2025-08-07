package io.github.waqfs.module;

import io.github.waqfs.gui.widget.RootModule;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class RenderModule<T extends RootModule<T>> extends TickModule<T> {
    public RenderModule(T widgetClass, String key, String displayName, @Nullable String tooltip) {
        super(widgetClass, key, displayName, tooltip);
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(ctx -> {
            if (!this.state) return;
            if (!this._inValidGame()) return;
            MatrixStack matrixStack = ctx.matrixStack();
            if (matrixStack == null) return;
            this.onWorldRender(ctx, matrixStack);
        });
    }

    private boolean _inValidGame() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) return true;
        return this.inValidGame();
    }

    protected abstract void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack);
}
