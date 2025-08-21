package dev.cigarette.gui.hud.bar.providers;

import dev.cigarette.gui.hud.bar.api.BarWidgetRegistry;

public final class DefaultProviders {
    private DefaultProviders() {}

    public static void registerAll() {
        BarWidgetRegistry.register(new MurderMysteryProvider());
        BarWidgetRegistry.register(new ZombiesProvider());
        BarWidgetRegistry.register(new BedwarsProvider());
        BarWidgetRegistry.register(new NearbyPlayersProvider());
    }
}

