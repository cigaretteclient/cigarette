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
    private static final int MAX_TICK_DELAY = 40;

    private final KeybindWidget toggle = new KeybindWidget("Toggle Keybind", "A keybind to toggle the macro.");
    private final ToggleWidget sendLogs = new ToggleWidget("Send Chat Logs", "Sends chat logs when switching directions.").withDefaultState(true);
    private final SliderWidget switchDelay = new SliderWidget("Switch Delay", "The delay (in ticks) between switching directions once the player stops moving.").withBounds(0, 0, MAX_TICK_DELAY);

    private final ToggleWidget dir1Forward = new ToggleWidget("Hold Forward", "Move forward.").withDefaultState(true);
    private final SliderWidget dir1ForwardDelay = new SliderWidget("Delay", "The delay (in ticks) before moving forward.").withBounds(0, 0, MAX_TICK_DELAY);
    private final ToggleWidget dir1Backward = new ToggleWidget("Hold Backward", "Move backward.").withDefaultState(false);
    private final SliderWidget dir1BackwardDelay = new SliderWidget("Delay", "The delay (in ticks) before moving backward.").withBounds(0, 0, MAX_TICK_DELAY);
    private final ToggleWidget dir1Left = new ToggleWidget("Hold Left", "Move left.").withDefaultState(false);
    private final SliderWidget dir1LeftDelay = new SliderWidget("Delay", "The delay (in ticks) before moving left.").withBounds(0, 0, MAX_TICK_DELAY);
    private final ToggleWidget dir1Right = new ToggleWidget("Hold Right", "Move right.").withDefaultState(false);
    private final SliderWidget dir1RightDelay = new SliderWidget("Delay", "The delay (in ticks) before moving right.").withBounds(0, 0, MAX_TICK_DELAY);
    private final ToggleWidget dir1HoldLeft = new ToggleWidget("Hold Left Click", "Hold left click.").withDefaultState(true);
    private final SliderWidget dir1HoldLeftDelay = new SliderWidget("Delay", "The delay (in ticks) before holding left click.").withBounds(0, 0, MAX_TICK_DELAY);

    private final ToggleWidget dir2Forward = new ToggleWidget("Hold Forward", "Move forward.").withDefaultState(false);
    private final SliderWidget dir2ForwardDelay = new SliderWidget("Delay", "The delay (in ticks) before moving forward.").withBounds(0, 0, MAX_TICK_DELAY);
    private final ToggleWidget dir2Backward = new ToggleWidget("Hold Backward", "Move backward.").withDefaultState(true);
    private final SliderWidget dir2BackwardDelay = new SliderWidget("Delay", "The delay (in ticks) before moving backward.").withBounds(0, 0, MAX_TICK_DELAY);
    private final ToggleWidget dir2Left = new ToggleWidget("Hold Left", "Move left.").withDefaultState(false);
    private final SliderWidget dir2LeftDelay = new SliderWidget("Delay", "The delay (in ticks) before moving left.").withBounds(0, 0, MAX_TICK_DELAY);
    private final ToggleWidget dir2Right = new ToggleWidget("Hold Right", "Move right.").withDefaultState(false);
    private final SliderWidget dir2RightDelay = new SliderWidget("Delay", "The delay (in ticks) before moving right.").withBounds(0, 0, MAX_TICK_DELAY);
    private final ToggleWidget dir2HoldLeft = new ToggleWidget("Hold Left Click", "Hold left click.").withDefaultState(true);
    private final SliderWidget dir2HoldLeftDelay = new SliderWidget("Delay", "The delay (in ticks) before holding left click.").withBounds(0, 0, MAX_TICK_DELAY);

    private boolean running = false;
    private boolean isPrimary = false;
    private boolean paused = false;
    private boolean wasPaused = false;

    private GardenWSMacro(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);

        DropdownWidget<TextWidget, BaseWidget.Stateless> dir1Config = new DropdownWidget<>("dir1", null);
        DropdownWidget<TextWidget, BaseWidget.Stateless> dir2Config = new DropdownWidget<>("dir2", null);

        dir1Config.setHeader(new TextWidget("Direction 1", "Configure the movement of the initial/first direction."));
        dir2Config.setHeader(new TextWidget("Direction 2", "Configure the movement of the alternate/second direction."));

        DropdownWidget<ToggleWidget, Boolean> initialForward = new DropdownWidget<>("a", null);
        initialForward.setHeader(dir1Forward).setChildren(dir1ForwardDelay);
        dir1Forward.registerConfigKey(id + ".dir1.forward");
        dir1ForwardDelay.registerConfigKey(id + ".dir1.forward.delay");

        DropdownWidget<ToggleWidget, Boolean> initialBackward = new DropdownWidget<>("b", null);
        initialBackward.setHeader(dir1Backward).setChildren(dir1BackwardDelay);
        dir1Backward.registerConfigKey(id + ".dir1.backward");
        dir1BackwardDelay.registerConfigKey(id + ".dir1.backward.delay");

        DropdownWidget<ToggleWidget, Boolean> initialLeft = new DropdownWidget<>("c", null);
        initialLeft.setHeader(dir1Left).setChildren(dir1LeftDelay);
        dir1Left.registerConfigKey(id + ".dir1.left");
        dir1LeftDelay.registerConfigKey(id + ".dir1.left.delay");

        DropdownWidget<ToggleWidget, Boolean> initialRight = new DropdownWidget<>("d", null);
        initialRight.setHeader(dir1Right).setChildren(dir1RightDelay);
        dir1Right.registerConfigKey(id + ".dir1.right");
        dir1RightDelay.registerConfigKey(id + ".dir1.right.delay");

        DropdownWidget<ToggleWidget, Boolean> initialHoldLeft = new DropdownWidget<>("e", null);
        initialHoldLeft.setHeader(dir1HoldLeft).setChildren(dir1HoldLeftDelay);
        dir1HoldLeft.registerConfigKey(id + ".dir1.holdleft");
        dir1HoldLeftDelay.registerConfigKey(id + ".dir1.holdleft.delay");

        DropdownWidget<ToggleWidget, Boolean> altForward = new DropdownWidget<>("f", null);
        altForward.setHeader(dir2Forward).setChildren(dir2ForwardDelay);
        dir2Forward.registerConfigKey(id + ".dir2.forward");
        dir2ForwardDelay.registerConfigKey(id + ".dir2.forward.delay");

        DropdownWidget<ToggleWidget, Boolean> altBackward = new DropdownWidget<>("g", null);
        altBackward.setHeader(dir2Backward).setChildren(dir2BackwardDelay);
        dir2Backward.registerConfigKey(id + ".dir2.backward");
        dir2BackwardDelay.registerConfigKey(id + ".dir2.backward.delay");

        DropdownWidget<ToggleWidget, Boolean> altLeft = new DropdownWidget<>("h", null);
        altLeft.setHeader(dir2Left).setChildren(dir2LeftDelay);
        dir2Left.registerConfigKey(id + ".dir2.left");
        dir2LeftDelay.registerConfigKey(id + ".dir2.left.delay");

        DropdownWidget<ToggleWidget, Boolean> altRight = new DropdownWidget<>("i", null);
        altRight.setHeader(dir2Right).setChildren(dir2RightDelay);
        dir2Right.registerConfigKey(id + ".dir2.right");
        dir2RightDelay.registerConfigKey(id + ".dir2.right.delay");

        DropdownWidget<ToggleWidget, Boolean> altHoldLeft = new DropdownWidget<>("j", null);
        altHoldLeft.setHeader(dir2HoldLeft).setChildren(dir2HoldLeftDelay);
        dir2HoldLeft.registerConfigKey(id + ".dir2.holdleft");
        dir2HoldLeftDelay.registerConfigKey(id + ".dir2.holdleft.delay");

        dir1Config.setChildren(initialForward, initialBackward, initialLeft, initialRight, initialHoldLeft);
        dir2Config.setChildren(altForward, altBackward, altLeft, altRight, altHoldLeft);

        this.setChildren(toggle, switchDelay, sendLogs, dir1Config, dir2Config);
        toggle.registerConfigKey(id + ".toggle");
        switchDelay.registerConfigKey(id + ".switchdelay");
        sendLogs.registerConfigKey(id + ".sendlogs");
    }

    private void reset() {
        TickHelper.unschedule(this);
        TickHelper.unschedule(dir1Forward);
        TickHelper.unschedule(dir1Backward);
        TickHelper.unschedule(dir1Left);
        TickHelper.unschedule(dir1Right);
        TickHelper.unschedule(dir1HoldLeft);
        TickHelper.unschedule(dir2Forward);
        TickHelper.unschedule(dir2Backward);
        TickHelper.unschedule(dir2Left);
        TickHelper.unschedule(dir2Right);
        TickHelper.unschedule(dir2HoldLeft);
        this.releaseAllKeys();
        isPrimary = true;
        paused = false;
    }

    private void releaseAllKeys() {
        KeybindHelper.KEY_ATTACK.release();
        KeybindHelper.KEY_MOVE_FORWARD.release();
        KeybindHelper.KEY_MOVE_BACK.release();
        KeybindHelper.KEY_MOVE_LEFT.release();
        KeybindHelper.KEY_MOVE_RIGHT.release();
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (toggle.getKeybind().wasPhysicallyPressed()) {
            running = !running;
            wasPaused = false;
            Cigarette.CHAT_LOGGER.info("Garden WS Macro " + (running ? "Enabled" : "Disabled") + ".");
            if (!running) {
                this.reset();
            } else {
                isPrimary = false;
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
            isPrimary = !isPrimary;

            ToggleWidget forward = isPrimary ? dir1Forward : dir2Forward;
            SliderWidget forwardDelay = isPrimary ? dir1ForwardDelay : dir2ForwardDelay;
            ToggleWidget backward = isPrimary ? dir1Backward : dir2Backward;
            SliderWidget backwardDelay = isPrimary ? dir1BackwardDelay : dir2BackwardDelay;
            ToggleWidget left = isPrimary ? dir1Left : dir2Left;
            SliderWidget leftDelay = isPrimary ? dir1LeftDelay : dir2LeftDelay;
            ToggleWidget right = isPrimary ? dir1Right : dir2Right;
            SliderWidget rightDelay = isPrimary ? dir1RightDelay : dir2RightDelay;
            ToggleWidget holdLeft = isPrimary ? dir1HoldLeft : dir2HoldLeft;
            SliderWidget holdLeftDelay = isPrimary ? dir1HoldLeftDelay : dir2HoldLeftDelay;

            this.releaseAllKeys();

            if (forwardDelay.getRawState() == 0) {
                KeybindHelper.KEY_MOVE_FORWARD.hold(forward.getRawState());
            } else {
                TickHelper.scheduleOnce(forward, () -> {
                    KeybindHelper.KEY_MOVE_FORWARD.hold(forward.getRawState());
                }, forwardDelay.getRawState().intValue());
            }

            if (backwardDelay.getRawState() == 0) {
                KeybindHelper.KEY_MOVE_BACK.hold(backward.getRawState());
            } else {
                TickHelper.scheduleOnce(backward, () -> {
                    KeybindHelper.KEY_MOVE_BACK.hold(backward.getRawState());
                }, backwardDelay.getRawState().intValue());
            }

            if (leftDelay.getRawState() == 0) {
                KeybindHelper.KEY_MOVE_LEFT.hold(left.getRawState());
            } else {
                TickHelper.scheduleOnce(left, () -> {
                    KeybindHelper.KEY_MOVE_LEFT.hold(left.getRawState());
                }, leftDelay.getRawState().intValue());
            }

            if (rightDelay.getRawState() == 0) {
                KeybindHelper.KEY_MOVE_RIGHT.hold(right.getRawState());
            } else {
                TickHelper.scheduleOnce(right, () -> {
                    KeybindHelper.KEY_MOVE_RIGHT.hold(right.getRawState());
                }, rightDelay.getRawState().intValue());
            }

            if (holdLeftDelay.getRawState() == 0) {
                KeybindHelper.KEY_ATTACK.hold(holdLeft.getRawState());
            } else {
                TickHelper.scheduleOnce(holdLeft, () -> {
                    KeybindHelper.KEY_ATTACK.hold(holdLeft.getRawState());
                }, holdLeftDelay.getRawState().intValue());
            }

            paused = true;
            TickHelper.scheduleOnce(this, () -> {
                paused = false;
            }, MAX_TICK_DELAY);
        }
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.SKYBLOCK;
    }
}
