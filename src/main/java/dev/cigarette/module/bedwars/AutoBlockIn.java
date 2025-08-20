package dev.cigarette.module.bedwars;

import dev.cigarette.GameDetector;
import dev.cigarette.gui.widget.KeybindWidget;
import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public class AutoBlockIn extends TickModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "Auto Block-In";
    protected static final String MODULE_TOOLTIP = "Automatically surrounds you in blocks to help break beds.";
    protected static final String MODULE_ID = "bedwars.autoblockin";

    private final KeybindWidget keybind = new KeybindWidget(Text.literal("Keybind"), Text.literal("A key to trigger the block in module."));
    private final SliderWidget speed = new SliderWidget(Text.literal("Speed"), Text.literal("The higher the speed, the less time spent between adjusting the camera and placing blocks.")).withBounds(0, 12, 15);

    private boolean running = false;
    private Vec3d originalPos = null;
    private float originalYaw = 0;
    private float originalPitch = 0;

    public AutoBlockIn() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        keybind.registerConfigKey("bedwars.autoblockin.key");
        speed.registerConfigKey("bedwars.autoblockin.speed");
    }

    private void enable(@NotNull ClientPlayerEntity player) {
        running = true;
        originalPos = player.getPos();
        originalYaw = player.getYaw();
        originalPitch = player.getPitch();
    }

    private void disable() {
        running = false;
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (!running) {
            if (!keybind.getKeybind().isPressed()) return;
            enable(player);
        }
        int ticksPerBlock = 16 - speed.getRawState().intValue();
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }
}
