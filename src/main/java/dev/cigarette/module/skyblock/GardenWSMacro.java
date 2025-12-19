package dev.cigarette.module.skyblock;

import dev.cigarette.Cigarette;
import dev.cigarette.GameDetector;
import dev.cigarette.gui.widget.*;
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
    private final ToggleWidget holdLeftClick = new ToggleWidget("Hold Left Click", "Holds left click while the macro is running.").withDefaultState(true);

    private final ToggleWidget forwardLeft = new ToggleWidget("Left on Forward", "Holds left while moving forward.").withDefaultState(false);
    private final ToggleWidget forwardRight = new ToggleWidget("Right on Forward", "Holds right while moving forward.").withDefaultState(false);
    private final ToggleWidget backLeft = new ToggleWidget("Left on Back", "Holds left while moving forward.").withDefaultState(false);
    private final ToggleWidget backRight = new ToggleWidget("Right on Back", "Holds right while moving forward.").withDefaultState(false);
    private final SliderWidget secondaryDelay = new SliderWidget("Hold Delay", "The delay (in ticks) between holding left/right after switching directions.").withBounds(0, 0, 40);

    private boolean running = false;
    private boolean isForward = false;
    private boolean paused = false;
    private boolean wasPaused = false;

    private GardenWSMacro(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);

        DropdownWidget<TextWidget, BaseWidget.Stateless> config = new DropdownWidget<>("", "");
        config.setHeader(new TextWidget("Secondary Key", "Configure the left/right movement for the cycle."));
        config.setChildren(forwardLeft, forwardRight, backLeft, backRight, secondaryDelay);

        this.setChildren(toggle, switchDelay, holdLeftClick, sendLogs, config);
        toggle.registerConfigKey(id + ".toggle");
        switchDelay.registerConfigKey(id + ".switchdelay");
        holdLeftClick.registerConfigKey(id + ".holdleftclick");
        sendLogs.registerConfigKey(id + ".sendlogs");
        forwardLeft.registerConfigKey(id + ".secondary.forwardleft");
        forwardRight.registerConfigKey(id + ".secondary.forwardright");
        backLeft.registerConfigKey(id + ".secondary.backleft");
        backRight.registerConfigKey(id + ".secondary.backright");
        secondaryDelay.registerConfigKey(id + ".secondary.delay");
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (toggle.getKeybind().wasPhysicallyPressed()) {
            running = !running;
            wasPaused = false;
            Cigarette.CHAT_LOGGER.info("Garden WS Macro " + (running ? "Enabled" : "Disabled") + ".");
            if (!running) {
                KeybindHelper.KEY_ATTACK.release();
                KeybindHelper.KEY_MOVE_FORWARD.release();
                KeybindHelper.KEY_MOVE_BACK.release();
                KeybindHelper.KEY_MOVE_LEFT.release();
                KeybindHelper.KEY_MOVE_RIGHT.release();
                TickHelper.unschedule(this);
            } else {
                isForward = false;
            }
            return;
        }
        if (!running || paused) return;
        Vec3d vel = player.getVelocity();
        double speed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (speed == 0) {
            if (sendLogs.getRawState()) {
                Cigarette.CHAT_LOGGER.info("Switching macro direction.");
            }
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
            if (holdLeftClick.getRawState()) {
                KeybindHelper.KEY_ATTACK.hold();
            }

            KeybindHelper.KEY_MOVE_FORWARD.hold(isForward);
            KeybindHelper.KEY_MOVE_BACK.hold(!isForward);
            KeybindHelper.KEY_MOVE_LEFT.release();
            KeybindHelper.KEY_MOVE_RIGHT.release();

            paused = true;
            if (secondaryDelay.getRawState().intValue() == 0) {
                KeybindHelper.KEY_MOVE_LEFT.hold((isForward && forwardLeft.getRawState()) || (!isForward && backLeft.getRawState()));
                KeybindHelper.KEY_MOVE_RIGHT.hold((isForward && forwardRight.getRawState()) || (!isForward && backRight.getRawState()));
            } else {
                TickHelper.scheduleOnce(this, () -> {
                    KeybindHelper.KEY_MOVE_LEFT.hold((isForward && forwardLeft.getRawState()) || (!isForward && backLeft.getRawState()));
                    KeybindHelper.KEY_MOVE_RIGHT.hold((isForward && forwardRight.getRawState()) || (!isForward && backRight.getRawState()));
                    paused = false;
                }, secondaryDelay.getRawState().intValue());
                return;
            }

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
