package io.github.waqfs.module;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class TickModule extends BaseModule {
    public TickModule(String key, String displayName, @Nullable String tooltip) {
        super(key, displayName, tooltip);
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (this.state && client.world != null && client.player != null && this._inValidGame()) {
                onEnabledTick(client, client.world, client.player);
            } else {
                onDisabledTick(client);
            }
        });
    }

    private boolean _inValidGame() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) return true;
        return this.inValidGame();
    }

    protected boolean inValidGame() {
        return true;
    }

    protected abstract void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player);

    protected void onDisabledTick(MinecraftClient client) {
    }
}
