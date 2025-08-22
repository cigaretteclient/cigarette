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
    public static final Notifications INSTANCE = Cigarette.CONFIG.constructModule(new Notifications("ui.notifications", "Notifications", "Displays notifications."), "UI");

    private NotificationDisplay display;

    public Notifications(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.widget.withDefaultState(true);
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world,
            @NotNull ClientPlayerEntity player) {
        if (display == null) {
            display = new NotificationDisplay();
            Cigarette.NOTIFICATION_DISPLAY = display;
        }
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        if (Cigarette.NOTIFICATION_DISPLAY == display) {
            Cigarette.NOTIFICATION_DISPLAY = null;
        }
        display = null;
    }
}
