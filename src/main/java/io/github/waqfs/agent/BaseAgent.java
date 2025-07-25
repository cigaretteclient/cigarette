package io.github.waqfs.agent;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;

public abstract class BaseAgent {
    public BaseAgent() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.world != null && client.player != null && this.inValidGame()) {
                onValidTick(client, client.world, client.player);
            } else {
                onInvalidTick(client);
            }
        });
    }

    protected abstract boolean inValidGame();

    protected abstract void onValidTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player);

    protected void onInvalidTick(MinecraftClient client) {
    }
}
