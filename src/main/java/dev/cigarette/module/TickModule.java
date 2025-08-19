package dev.cigarette.module;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.widget.BaseWidget;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class TickModule<Widget extends BaseWidget<Boolean>, Boolean> extends BaseModule<Widget, Boolean> {
    public TickModule(WidgetGenerator<Widget, Boolean> func, String key, String displayName, @Nullable String tooltip) {
        super(func, key, displayName, tooltip);
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if ((this.getRawState()) && client.world != null && client.player != null && this._inValidGame()) {
                onEnabledTick(client, client.world, client.player);
            } else {
                onDisabledTick(client);
            }
        });
    }

    private boolean _inValidGame() {
        if (Cigarette.IN_DEV_ENVIRONMENT) return true;
        return this.inValidGame();
    }

    public boolean inValidGame() {
        return true;
    }

    public final boolean isRunning() {
        return (boolean) this.widget.getRawState() && this._inValidGame();
    }

    protected abstract void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player);

    protected void onDisabledTick(MinecraftClient client) {
    }
}
