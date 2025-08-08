package io.github.waqfs.agent;

import io.github.waqfs.Cigarette;
import io.github.waqfs.gui.widget.ToggleOptionsWidget;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseAgent {
    private final @Nullable ToggleOptionsWidget devToggle;

    public BaseAgent(@Nullable ToggleOptionsWidget devToggle) {
        this.devToggle = devToggle;
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.world != null && client.player != null && this._inValidGame()) {
                onValidTick(client, client.world, client.player);
            } else {
                onInvalidTick(client);
            }
        });
    }

    protected boolean _inValidGame() {
        return (Cigarette.IN_DEV_ENVIRONMENT && devToggle != null && devToggle.getState()) || this.inValidGame();
    }

    protected abstract boolean inValidGame();

    protected abstract void onValidTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player);

    protected void onInvalidTick(MinecraftClient client) {
    }
}
