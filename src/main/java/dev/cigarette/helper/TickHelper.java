package dev.cigarette.helper;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.util.Pair;

import java.util.HashMap;

public class TickHelper {
    private static final HashMap<Object, Pair<Integer, Runnable>> onceActions = new HashMap<>();

    public static void scheduleOnce(Object key, Runnable action, int ticks) {
        onceActions.put(key, new Pair<>(ticks, action));
    }

    public static void unschedule(Object key) {
        onceActions.remove(key);
    }

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
