package io.github.waqfs.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Events {
    private final Map<Class<?>, List<Function<? super Event<?>, Void>>> listeners = new HashMap<>();

    public <T> void registerListener(Class<? extends Event<T>> eventType, Function<? super Event<T>, Void> listener) {
        List<Function<? super Event<?>, Void>> eventListeners = listeners.computeIfAbsent(eventType, k -> new ArrayList<>());
        @SuppressWarnings("unchecked")
        Function<? super Event<?>, Void> castedListener = (Function<? super Event<?>, Void>) listener;
        eventListeners.add(castedListener);
    }

    public <T> void unregisterListener(Class<? extends Event<T>> eventType, Function<? super Event<T>, Void> listener) {
        List<Function<? super Event<?>, Void>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            @SuppressWarnings("unchecked")
            Function<? super Event<?>, Void> castedListener = (Function<? super Event<?>, Void>) listener;
            eventListeners.remove(castedListener);
        }
    }

    public <T> void dispatchEvent(Event<T> event) {
        List<Function<? super Event<?>, Void>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (Function<? super Event<?>, Void> listener : eventListeners) {
                listener.apply(event);
            }
        }
    }
}