package dev.cigarette.module.combat;

import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.helper.KeybindHelper;
import dev.cigarette.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;

public class SprintAlways extends TickModule<ToggleWidget, Boolean> {

    public static final SprintAlways INSTANCE =
            new SprintAlways("combat.sprintalways", "SprintAlways",
                    "Always holds sprint as long as the module is enabled, except while sneaking.");

    private SprintAlways(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
    }

    @Override
    protected void onEnabledTick(MinecraftClient client,
                                 @NotNull ClientWorld world,
                                 @NotNull ClientPlayerEntity player) {
        // Don’t force sprint during any open GUI
        if (client.currentScreen != null) return;

        // Respect sneaking so the player can still walk normally while crouched
        if (player.isSneaking() || KeybindHelper.KEY_SNEAK.isPhysicallyPressed()) {
            return;
        }

        // Constant sprint input
        KeybindHelper.KEY_SPRINT.hold();
    }
}
