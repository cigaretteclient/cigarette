package io.github.waqfs.gui.notifications;

import java.lang.reflect.Type;
import java.util.Map;

import com.mojang.datafixers.util.Either;

import io.github.waqfs.events.Event;

/*
 * Map.of(
 *     "type", "info",
 *     "title", "Notification Title",
 *     "message", "This is a notification message."
 * )
 */
public class Notification extends Event<Map<String, String>> {
    public Notification(Map<String, String> data) {
        super(data);
    }

    public String getType() {
        return data.get("type");
    }

    public String getTitle() {
        return data.get("title");
    }

    public String getMessage() {
        return data.get("message");
    }
}
