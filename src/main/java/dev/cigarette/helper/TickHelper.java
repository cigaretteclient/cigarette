package dev.cigarette.helper;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.util.Pair;

import java.util.HashMap;

/**
 * Helper class for scheduling events to occur in the future.
 */
public class TickHelper {
    /**
     * Actions that only occur once.
     */
    private static final HashMap<Object, Pair<Integer, Runnable>> onceActions = new HashMap<>();

    /**
     * Schedule an {@code action} to occur after {@code ticks} number of ticks.
     *
     * @param key    A unique identifier to store this action. Usually {@code this} unless multiple actions are needed.
     * @param action The action to perform once the number of ticks has passed.
     * @param ticks  The number of ticks to wait until triggered.
     */
    public static void scheduleOnce(Object key, Runnable action, int ticks) {
        onceActions.put(key, new Pair<>(ticks, action));
    }

    /**
     * Unschedule an event that was already scheduled.
     *
     * @param key The unique identifier used in scheduling the event.
     */
    public static void unschedule(Object key) {
        onceActions.remove(key);
    }

    /**
     * Get the remaining ticks until a scheduled event triggers.
     *
     * @param key The unique identifier used in scheduling the event.
     * @return Remaining ticks until event is triggered
     */
    public static int whenOnce(Object key) {
        if (!onceActions.containsKey(key)) return -1;
        return onceActions.get(key).getLeft();
    }

    static {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (onceActions.isEmpty()) return;
            for (Object key : onceActions.keySet()) {
                Pair<Integer, Runnable> action = onceActions.get(key);
                int remainingTicks = action.getLeft() - 1;
                if (remainingTicks <= 0) {
                    onceActions.remove(key);
                    action.getRight().run();
                    continue;
                }
                action.setLeft(remainingTicks);
            }
        });
    }
}
