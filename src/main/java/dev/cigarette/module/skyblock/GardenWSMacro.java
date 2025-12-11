package dev.cigarette.module.skyblock;

import dev.cigarette.Cigarette;
import dev.cigarette.GameDetector;
import dev.cigarette.gui.widget.KeybindWidget;
import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.helper.KeybindHelper;
import dev.cigarette.helper.TickHelper;
import dev.cigarette.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public class GardenWSMacro extends TickModule<ToggleWidget, Boolean> {
    public static final GardenWSMacro INSTANCE = new GardenWSMacro("skyblock.gardenwsmacro", "Garden WS Macro", "Swaps between holding W and S while farming.");

    private final KeybindWidget toggle = new KeybindWidget("Toggle Keybind", "A keybind to toggle the macro.");
    private final ToggleWidget sendLogs = new ToggleWidget("Send Chat Logs", "Sends chat logs when switching directions.").withDefaultState(true);
    private final SliderWidget switchDelay = new SliderWidget("Switch Delay", "The delay (in ticks) between switching directions once the player stops moving.").withBounds(0, 0, 40);

    private boolean running = false;
    private boolean isForward = false;
    private boolean paused = false;
    private boolean wasPaused = false;

    private GardenWSMacro(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.setChildren(toggle, switchDelay, sendLogs);
        toggle.registerConfigKey(id + ".toggle");
        switchDelay.registerConfigKey(id + ".switchdelay");
        sendLogs.registerConfigKey(id + ".sendlogs");
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (toggle.getKeybind().wasPhysicallyPressed()) {
            running = !running;
            wasPaused = false;
            Cigarette.CHAT_LOGGER.info("Garden WS Macro " + (running ? "Enabled" : "Disabled") + ".");
            if (!running) {
                KeybindHelper.KEY_MOVE_FORWARD.release();
                KeybindHelper.KEY_MOVE_BACK.release();
            } else {
                isForward = false;
            }
            return;
        }
        if (!running || paused) return;
        Vec3d vel = player.getVelocity();
        double speed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (speed == 0) {
            if (sendLogs.getRawState()) Cigarette.CHAT_LOGGER.info("Switching macro direction.");
            if (switchDelay.getRawState() > 0 && !wasPaused) {
                paused = true;
                TickHelper.scheduleOnce(this, () -> {
                    paused = false;
                    wasPaused = true;
                }, switchDelay.getRawState().intValue());
                return;
            }
            wasPaused = false;
            isForward = !isForward;
            if (isForward) {
                KeybindHelper.KEY_MOVE_BACK.release();
                KeybindHelper.KEY_MOVE_FORWARD.hold(true);
            } else {
                KeybindHelper.KEY_MOVE_FORWARD.release();
                KeybindHelper.KEY_MOVE_BACK.hold(true);
            }
            paused = true;
            TickHelper.scheduleOnce(this, () -> {
                paused = false;
            }, 10);
        }
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.SKYBLOCK;
    }
}
