package dev.cigarette.gui.hud.bar.api;

import dev.cigarette.gui.hud.bar.providers.DefaultProviders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BarWidgetRegistry {
    private static final List<BarWidgetProvider> PROVIDERS = new ArrayList<>();
    private static boolean defaultsRegistered = false;

    private BarWidgetRegistry() {}

    public static void register(BarWidgetProvider provider) {
        if (provider != null) PROVIDERS.add(provider);
    }

    public static List<BarWidgetProvider> providers() {
        return Collections.unmodifiableList(PROVIDERS);
    }

    public static void ensureDefaultsRegistered() {
        if (defaultsRegistered) return;
        defaultsRegistered = true;
        DefaultProviders.registerAll();
    }
}

