package dev.cigarette.gui.hud.notification.internal;

import dev.cigarette.gui.hud.notification.Notification;

public class NotificationWithEasingProgress {
    private final Notification notification;
    private float ticks;

    public static final float APPEAR_TICKS = 6f;
    public static final float VISIBLE_TICKS = 60f;
    private static final float DISAPPEAR_TICKS = 6f;
    public static final float TOTAL_TICKS = APPEAR_TICKS + VISIBLE_TICKS + DISAPPEAR_TICKS;

    public static final float TICKS_UNTIL_EXPIRATION = TOTAL_TICKS - APPEAR_TICKS - VISIBLE_TICKS;

    public NotificationWithEasingProgress(Notification notification) {
        this.notification = notification;
        this.ticks = 0f;
    }

    public Notification getNotification() {
        return notification;
    }

    public float getTicksElapsed() {
        return ticks;
    }

    public void updateProgress(float deltaTicks) {
        this.ticks += deltaTicks;
    }

    // (linear)
    public float getSlideProgress() {
        if (ticks <= 0f)
            return 0f;
        if (ticks < APPEAR_TICKS) {
            float t = ticks / APPEAR_TICKS;

            return t;
        }
        if (ticks < APPEAR_TICKS + VISIBLE_TICKS) {
            return 1f;
        }
        if (ticks < TOTAL_TICKS) {
            float t = (ticks - (APPEAR_TICKS + VISIBLE_TICKS)) / DISAPPEAR_TICKS;

            return t;
        }
        return 0f;
    }

    public float getTicksUntilExpiration() {
        return Math.max(0f, TOTAL_TICKS - ticks);
    }

    public boolean isExpired() {
        return ticks >= TOTAL_TICKS;
    }
}