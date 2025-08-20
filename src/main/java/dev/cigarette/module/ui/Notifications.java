package dev.cigarette.module.ui;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.hud.notification.NotificationDisplay;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;

public class Notifications extends RenderModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "Notifications";
    protected static final String MODULE_TOOLTIP = "Displays notifications.";
    protected static final String MODULE_ID = "render.notifications";

    private NotificationDisplay display;

    public Notifications() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        this.widget.withDefaultState(true);
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {
        if (display == null) {
            display = new NotificationDisplay();
        }
        Cigarette.NOTIFICATION_DISPLAY = display;
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world,
            @NotNull ClientPlayerEntity player) {
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        if (Cigarette.NOTIFICATION_DISPLAY == display) {
            Cigarette.NOTIFICATION_DISPLAY = null;
        }
        display = null;
    }
}
