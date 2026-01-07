package dev.cigarette.module.skyblock;

import dev.cigarette.Cigarette;
import dev.cigarette.GameDetector;
import dev.cigarette.gui.widget.*;
import dev.cigarette.helper.KeybindHelper;
import dev.cigarette.helper.TickHelper;
import dev.cigarette.module.TickModule;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public class GardenWSMacro extends TickModule<ToggleWidget, Boolean> {
    public static final GardenWSMacro INSTANCE = new GardenWSMacro("skyblock.gardenwsmacro", "Garden WS Macro", "Swaps between holding W and S while farming.");
    private static final int MAX_TICK_DELAY = 900;

    private final KeybindWidget toggle = new KeybindWidget("Toggle Keybind", "A keybind to toggle the macro.");
    private final ToggleWidget sendLogs = new ToggleWidget("Send Chat Logs", "Sends chat logs when switching directions.").withDefaultState(true);
    private final SliderWidget switchDelay = new SliderWidget("Switch Delay", "The delay (in ticks) between switching directions once the player stops moving.").withBounds(0, 0, MAX_TICK_DELAY);
    private final SliderWidget switchCooldown = new SliderWidget("Switch Cooldown", "The cooldown (in ticks) after switching directions before another switch can occur.").withBounds(0, 0, MAX_TICK_DELAY);
    private final ToggleWidget warpToSpawn = new ToggleWidget("Warp to Spawn", "Automatically warp to garden spawn when enabling the macro.").withDefaultState(false);
    private final ToggleWidget warpToSpawnOnIce = new ToggleWidget("Ice to Spawn", "Warp to garden spawn when macro stops on packed ice.").withDefaultState(false);

    private final ToggleWidget dir1Forward = new ToggleWidget("Hold Forward", "Move forward.").withDefaultState(true);
    private final SliderWidget dir1ForwardDelay = new SliderWidget("On Delay", "The delay (in ticks) before moving forward.").withBounds(0, 0, MAX_TICK_DELAY);
    private final SliderWidget dir1ForwardOffDelay = new SliderWidget("Off Delay", "The delay (in ticks) before releasing forward after moving forward. Set to 0 to turn off.").withBounds(0, 0, MAX_TICK_DELAY);
    private final ToggleWidget dir1Backward = new ToggleWidget("Hold Backward", "Move backward.").withDefaultState(false);
    private final SliderWidget dir1BackwardDelay = new SliderWidget("On Delay", "The delay (in ticks) before moving backward.").withBounds(0, 0, MAX_TICK_DELAY);
    private final SliderWidget dir1BackwardOffDelay = new SliderWidget("Off Delay", "The delay (in ticks) before releasing backward after moving backward. Set to 0 to turn off.").withBounds(0, 0, MAX_TICK_DELAY);
    private final ToggleWidget dir1Left = new ToggleWidget("Hold Left", "Move left.").withDefaultState(false);
    private final SliderWidget dir1LeftDelay = new SliderWidget("On Delay", "The delay (in ticks) before moving left.").withBounds(0, 0, MAX_TICK_DELAY);
    private final SliderWidget dir1LeftOffDelay = new SliderWidget("Off Delay", "The delay (in ticks) before releasing left after moving left. Set to 0 to turn off.").withBounds(0, 0, MAX_TICK_DELAY);
    private final ToggleWidget dir1Right = new ToggleWidget("Hold Right", "Move right.").withDefaultState(false);
    private final SliderWidget dir1RightDelay = new SliderWidget("On Delay", "The delay (in ticks) before moving right.").withBounds(0, 0, MAX_TICK_DELAY);
    private final SliderWidget dir1RightOffDelay = new SliderWidget("Off Delay", "The delay (in ticks) before releasing right after moving right. Set to 0 to turn off.").withBounds(0, 0, MAX_TICK_DELAY);
    private final ToggleWidget dir1HoldLeft = new ToggleWidget("Hold Left Click", "Hold left click.").withDefaultState(true);
    private final SliderWidget dir1HoldLeftDelay = new SliderWidget("On Delay", "The delay (in ticks) before holding left click.").withBounds(0, 0, MAX_TICK_DELAY);
    private final SliderWidget dir1HoldLeftOffDelay = new SliderWidget("Off Delay", "The delay (in ticks) before releasing left click after holding left click. Set to 0 to turn off.").withBounds(0, 0, MAX_TICK_DELAY);

    private final ToggleWidget dir2Forward = new ToggleWidget("Hold Forward", "Move forward.").withDefaultState(false);
    private final SliderWidget dir2ForwardDelay = new SliderWidget("On Delay", "The delay (in ticks) before moving forward.").withBounds(0, 0, MAX_TICK_DELAY);
    private final SliderWidget dir2ForwardOffDelay = new SliderWidget("Off Delay", "The delay (in ticks) before releasing forward after moving forward. Set to 0 to turn off.").withBounds(0, 0, MAX_TICK_DELAY);
    private final ToggleWidget dir2Backward = new ToggleWidget("Hold Backward", "Move backward.").withDefaultState(true);
    private final SliderWidget dir2BackwardDelay = new SliderWidget("On Delay", "The delay (in ticks) before moving backward.").withBounds(0, 0, MAX_TICK_DELAY);
    private final SliderWidget dir2BackwardOffDelay = new SliderWidget("Off Delay", "The delay (in ticks) before releasing backward after moving backward. Set to 0 to turn off.").withBounds(0, 0, MAX_TICK_DELAY);
    private final ToggleWidget dir2Left = new ToggleWidget("Hold Left", "Move left.").withDefaultState(false);
    private final SliderWidget dir2LeftDelay = new SliderWidget("On Delay", "The delay (in ticks) before moving left.").withBounds(0, 0, MAX_TICK_DELAY);
    private final SliderWidget dir2LeftOffDelay = new SliderWidget("Off Delay", "The delay (in ticks) before releasing left after moving left. Set to 0 to turn off.").withBounds(0, 0, MAX_TICK_DELAY);
    private final ToggleWidget dir2Right = new ToggleWidget("Hold Right", "Move right.").withDefaultState(false);
    private final SliderWidget dir2RightDelay = new SliderWidget("On Delay", "The delay (in ticks) before moving right.").withBounds(0, 0, MAX_TICK_DELAY);
    private final SliderWidget dir2RightOffDelay = new SliderWidget("Off Delay", "The delay (in ticks) before releasing right after moving right. Set to 0 to turn off.").withBounds(0, 0, MAX_TICK_DELAY);
    private final ToggleWidget dir2HoldLeft = new ToggleWidget("Hold Left Click", "Hold left click.").withDefaultState(true);
    private final SliderWidget dir2HoldLeftDelay = new SliderWidget("On Delay", "The delay (in ticks) before holding left click.").withBounds(0, 0, MAX_TICK_DELAY);
    private final SliderWidget dir2HoldLeftOffDelay = new SliderWidget("Off Delay", "The delay (in ticks) before releasing left click after holding left click. Set to 0 to turn off.").withBounds(0, 0, MAX_TICK_DELAY);

    private boolean running = false;
    private boolean isPrimary = false;
    private boolean paused = false;
    private boolean wasPaused = false;
    private boolean didWarp = false;
    private int ticksSinceSwitch = 0;
    private Vec3d lastPos = Vec3d.ZERO;

    private GardenWSMacro(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);

        DropdownWidget<TextWidget, BaseWidget.Stateless> dir1Config = new DropdownWidget<>("dir1", null);
        DropdownWidget<TextWidget, BaseWidget.Stateless> dir2Config = new DropdownWidget<>("dir2", null);

        dir1Config.setHeader(new TextWidget("Direction 1", "Configure the movement of the initial/first direction."));
        dir2Config.setHeader(new TextWidget("Direction 2", "Configure the movement of the alternate/second direction."));

        DropdownWidget<ToggleWidget, Boolean> initialForward = new DropdownWidget<>("a", null);
        initialForward.setHeader(dir1Forward).setChildren(dir1ForwardDelay, dir1ForwardOffDelay);
        dir1Forward.registerConfigKey(id + ".dir1.forward");
        dir1ForwardDelay.registerConfigKey(id + ".dir1.forward.delay");
        dir1ForwardOffDelay.registerConfigKey(id + ".dir1.forward.delayoff");

        DropdownWidget<ToggleWidget, Boolean> initialBackward = new DropdownWidget<>("b", null);
        initialBackward.setHeader(dir1Backward).setChildren(dir1BackwardDelay, dir1BackwardOffDelay);
        dir1Backward.registerConfigKey(id + ".dir1.backward");
        dir1BackwardDelay.registerConfigKey(id + ".dir1.backward.delay");
        dir1BackwardOffDelay.registerConfigKey(id + ".dir1.backward.delayoff");

        DropdownWidget<ToggleWidget, Boolean> initialLeft = new DropdownWidget<>("c", null);
        initialLeft.setHeader(dir1Left).setChildren(dir1LeftDelay, dir1LeftOffDelay);
        dir1Left.registerConfigKey(id + ".dir1.left");
        dir1LeftDelay.registerConfigKey(id + ".dir1.left.delay");
        dir1LeftOffDelay.registerConfigKey(id + ".dir1.left.delayoff");

        DropdownWidget<ToggleWidget, Boolean> initialRight = new DropdownWidget<>("d", null);
        initialRight.setHeader(dir1Right).setChildren(dir1RightDelay, dir1RightOffDelay);
        dir1Right.registerConfigKey(id + ".dir1.right");
        dir1RightDelay.registerConfigKey(id + ".dir1.right.delay");
        dir1RightOffDelay.registerConfigKey(id + ".dir1.right.delayoff");

        DropdownWidget<ToggleWidget, Boolean> initialHoldLeft = new DropdownWidget<>("e", null);
        initialHoldLeft.setHeader(dir1HoldLeft).setChildren(dir1HoldLeftDelay, dir1HoldLeftOffDelay);
        dir1HoldLeft.registerConfigKey(id + ".dir1.holdleft");
        dir1HoldLeftDelay.registerConfigKey(id + ".dir1.holdleft.delay");
        dir1HoldLeftOffDelay.registerConfigKey(id + ".dir1.holdleft.delayoff");

        DropdownWidget<ToggleWidget, Boolean> altForward = new DropdownWidget<>("f", null);
        altForward.setHeader(dir2Forward).setChildren(dir2ForwardDelay, dir2ForwardOffDelay);
        dir2Forward.registerConfigKey(id + ".dir2.forward");
        dir2ForwardDelay.registerConfigKey(id + ".dir2.forward.delay");
        dir2ForwardOffDelay.registerConfigKey(id + ".dir2.forward.delayoff");

        DropdownWidget<ToggleWidget, Boolean> altBackward = new DropdownWidget<>("g", null);
        altBackward.setHeader(dir2Backward).setChildren(dir2BackwardDelay, dir2BackwardOffDelay);
        dir2Backward.registerConfigKey(id + ".dir2.backward");
        dir2BackwardDelay.registerConfigKey(id + ".dir2.backward.delay");
        dir2BackwardOffDelay.registerConfigKey(id + ".dir2.backward.delayoff");

        DropdownWidget<ToggleWidget, Boolean> altLeft = new DropdownWidget<>("h", null);
        altLeft.setHeader(dir2Left).setChildren(dir2LeftDelay, dir2LeftOffDelay);
        dir2Left.registerConfigKey(id + ".dir2.left");
        dir2LeftDelay.registerConfigKey(id + ".dir2.left.delay");
        dir2LeftOffDelay.registerConfigKey(id + ".dir2.left.delayoff");

        DropdownWidget<ToggleWidget, Boolean> altRight = new DropdownWidget<>("i", null);
        altRight.setHeader(dir2Right).setChildren(dir2RightDelay, dir2RightOffDelay);
        dir2Right.registerConfigKey(id + ".dir2.right");
        dir2RightDelay.registerConfigKey(id + ".dir2.right.delay");
        dir2RightOffDelay.registerConfigKey(id + ".dir2.right.delayoff");

        DropdownWidget<ToggleWidget, Boolean> altHoldLeft = new DropdownWidget<>("j", null);
        altHoldLeft.setHeader(dir2HoldLeft).setChildren(dir2HoldLeftDelay, dir2HoldLeftOffDelay);
        dir2HoldLeft.registerConfigKey(id + ".dir2.holdleft");
        dir2HoldLeftDelay.registerConfigKey(id + ".dir2.holdleft.delay");
        dir2HoldLeftOffDelay.registerConfigKey(id + ".dir2.holdleft.delayoff");

        dir1Config.setChildren(initialForward, initialBackward, initialLeft, initialRight, initialHoldLeft);
        dir2Config.setChildren(altForward, altBackward, altLeft, altRight, altHoldLeft);

        this.setChildren(toggle, switchDelay, switchCooldown, warpToSpawn, warpToSpawnOnIce, sendLogs, dir1Config, dir2Config);
        toggle.registerConfigKey(id + ".toggle");
        switchDelay.registerConfigKey(id + ".switchdelay");
        switchCooldown.registerConfigKey(id + ".switchcooldown");
        warpToSpawn.registerConfigKey(id + ".warptospawn");
        warpToSpawnOnIce.registerConfigKey(id + ".warptospawn.onice");
        sendLogs.registerConfigKey(id + ".sendlogs");
    }

    private void reset() {
        TickHelper.unschedule(this);
        TickHelper.unschedule(dir1ForwardDelay);
        TickHelper.unschedule(dir1BackwardDelay);
        TickHelper.unschedule(dir1LeftDelay);
        TickHelper.unschedule(dir1RightDelay);
        TickHelper.unschedule(dir1HoldLeftDelay);
        TickHelper.unschedule(dir2ForwardDelay);
        TickHelper.unschedule(dir2BackwardDelay);
        TickHelper.unschedule(dir2LeftDelay);
        TickHelper.unschedule(dir2RightDelay);
        TickHelper.unschedule(dir2HoldLeftDelay);

        TickHelper.unschedule(dir1ForwardOffDelay);
        TickHelper.unschedule(dir1BackwardOffDelay);
        TickHelper.unschedule(dir1LeftOffDelay);
        TickHelper.unschedule(dir1RightOffDelay);
        TickHelper.unschedule(dir1HoldLeftOffDelay);
        TickHelper.unschedule(dir2ForwardOffDelay);
        TickHelper.unschedule(dir2BackwardOffDelay);
        TickHelper.unschedule(dir2LeftOffDelay);
        TickHelper.unschedule(dir2RightOffDelay);
        TickHelper.unschedule(dir2HoldLeftOffDelay);
        this.releaseAllKeys();
        isPrimary = true;
        paused = false;
        running = false;
        ticksSinceSwitch = 0;
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
                ticksSinceSwitch = switchCooldown.getRawState().intValue();
                isPrimary = false;
                lastPos = player.getEntityPos();
                if (warpToSpawn.getRawState()) {
                    player.networkHandler.sendChatCommand("warp garden");
                    didWarp = true;
                    paused = true;
                    TickHelper.scheduleOnce(this, () -> {
                        paused = false;
                    }, 20);
                }
            }
            return;
        }
        if (!running) {
            return;
        }
        Vec3d diff = player.getEntityPos().subtract(lastPos);
        if (diff.lengthSquared() > 2 && !didWarp) {
            this.reset();
            if (sendLogs.getRawState()) {
                Cigarette.CHAT_LOGGER.error("Player position updated too much, turning off macro.");
            }
            return;
        }
        lastPos = player.getEntityPos();

        if (paused) return;
        didWarp = false;
        if (diff.lengthSquared() == 0 && ++ticksSinceSwitch > switchCooldown.getRawState()) {
            ticksSinceSwitch = 0;

            if (warpToSpawnOnIce.getRawState() && world.getBlockState(player.getBlockPos().down()).getBlock().getDefaultState().isOf(Blocks.PACKED_ICE)) {
                if (sendLogs.getRawState()) Cigarette.CHAT_LOGGER.info("Warping to Garden spawn.");
                player.networkHandler.sendChatCommand("warp garden");
                this.reset();
                running = true;
                didWarp = true;
                paused = true;
                isPrimary = false;
                TickHelper.scheduleOnce(this, () -> {
                    paused = false;
                }, 20);
                return;
            }

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
            isPrimary = !isPrimary;

            this.releaseAllKeys();

            ToggleWidget forward = isPrimary ? dir1Forward : dir2Forward;
            SliderWidget forwardDelay = isPrimary ? dir1ForwardDelay : dir2ForwardDelay;
            SliderWidget forwardOffDelay = isPrimary ? dir1ForwardOffDelay : dir2ForwardOffDelay;
            ToggleWidget backward = isPrimary ? dir1Backward : dir2Backward;
            SliderWidget backwardDelay = isPrimary ? dir1BackwardDelay : dir2BackwardDelay;
            SliderWidget backwardOffDelay = isPrimary ? dir1BackwardOffDelay : dir2BackwardOffDelay;
            ToggleWidget left = isPrimary ? dir1Left : dir2Left;
            SliderWidget leftDelay = isPrimary ? dir1LeftDelay : dir2LeftDelay;
            SliderWidget leftOffDelay = isPrimary ? dir1LeftOffDelay : dir2LeftOffDelay;
            ToggleWidget right = isPrimary ? dir1Right : dir2Right;
            SliderWidget rightDelay = isPrimary ? dir1RightDelay : dir2RightDelay;
            SliderWidget rightOffDelay = isPrimary ? dir1RightOffDelay : dir2RightOffDelay;
            ToggleWidget holdLeft = isPrimary ? dir1HoldLeft : dir2HoldLeft;
            SliderWidget holdLeftDelay = isPrimary ? dir1HoldLeftDelay : dir2HoldLeftDelay;
            SliderWidget holdLeftOffDelay = isPrimary ? dir1HoldLeftOffDelay : dir2HoldLeftOffDelay;

            if (forwardDelay.getRawState() == 0) KeybindHelper.KEY_MOVE_FORWARD.hold(forward.getRawState());
            else TickHelper.scheduleOnce(forwardDelay, () -> KeybindHelper.KEY_MOVE_FORWARD.hold(forward.getRawState()), forwardDelay.getRawState().intValue());
            if (forwardOffDelay.getRawState() > 0) TickHelper.scheduleOnce(forwardOffDelay, KeybindHelper.KEY_MOVE_FORWARD::release, forwardDelay.getRawState().intValue() + forwardOffDelay.getRawState().intValue());

            if (backwardDelay.getRawState() == 0) KeybindHelper.KEY_MOVE_BACK.hold(backward.getRawState());
            else TickHelper.scheduleOnce(backwardDelay, () -> KeybindHelper.KEY_MOVE_BACK.hold(backward.getRawState()), backwardDelay.getRawState().intValue());
            if (backwardOffDelay.getRawState() > 0) TickHelper.scheduleOnce(backwardOffDelay, KeybindHelper.KEY_MOVE_BACK::release, backwardDelay.getRawState().intValue() + backwardOffDelay.getRawState().intValue());

            if (leftDelay.getRawState() == 0) KeybindHelper.KEY_MOVE_LEFT.hold(left.getRawState());
            else TickHelper.scheduleOnce(leftDelay, () -> KeybindHelper.KEY_MOVE_LEFT.hold(left.getRawState()), leftDelay.getRawState().intValue());
            if (leftOffDelay.getRawState() > 0) TickHelper.scheduleOnce(leftOffDelay, KeybindHelper.KEY_MOVE_LEFT::release, leftDelay.getRawState().intValue() + leftOffDelay.getRawState().intValue());

            if (rightDelay.getRawState() == 0) KeybindHelper.KEY_MOVE_RIGHT.hold(right.getRawState());
            else TickHelper.scheduleOnce(rightDelay, () -> KeybindHelper.KEY_MOVE_RIGHT.hold(right.getRawState()), rightDelay.getRawState().intValue());
            if (rightOffDelay.getRawState() > 0) TickHelper.scheduleOnce(rightOffDelay, KeybindHelper.KEY_MOVE_RIGHT::release, rightDelay.getRawState().intValue() + rightOffDelay.getRawState().intValue());

            if (holdLeftDelay.getRawState() == 0) KeybindHelper.KEY_ATTACK.hold(holdLeft.getRawState());
            else TickHelper.scheduleOnce(holdLeftDelay, () -> KeybindHelper.KEY_ATTACK.hold(holdLeft.getRawState()), holdLeftDelay.getRawState().intValue());
            if (holdLeftOffDelay.getRawState() > 0) TickHelper.scheduleOnce(holdLeftOffDelay, KeybindHelper.KEY_ATTACK::release, holdLeftDelay.getRawState().intValue() + holdLeftOffDelay.getRawState().intValue());

            paused = true;
            TickHelper.scheduleOnce(this, () -> {
                paused = false;
            }, 20);
        }
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.SKYBLOCK;
    }
}
