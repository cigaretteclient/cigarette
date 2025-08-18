package io.github.waqfs;

import io.github.waqfs.agent.BedwarsAgent;
import io.github.waqfs.agent.DevWidget;
import io.github.waqfs.agent.MurderMysteryAgent;
import io.github.waqfs.config.Config;
import io.github.waqfs.config.FileSystem;
import io.github.waqfs.events.Events;
import io.github.waqfs.gui.notifications.NotificationDisplay;
import io.github.waqfs.lib.ChatLogger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.font.Font;
import net.minecraft.client.font.FontFilterType.FilterMap;
import net.minecraft.client.font.FontLoader;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TrueTypeFontLoader;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cigarette implements ModInitializer {
    public static final String MOD_ID = "cigarette";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ChatLogger CHAT_LOGGER = new ChatLogger();
    public static final Identifier LOGO_IDENTIFIER = Identifier.of("cigarette", "icon.png");
    public static final boolean IN_DEV_ENVIRONMENT = FabricLoader.getInstance().isDevelopmentEnvironment();
    public static Config CONFIG = new Config();
    public static BedwarsAgent BEDWARS_AGENT = new BedwarsAgent(DevWidget.bedwarsAgent);
    public static MurderMysteryAgent MURDER_MYSTERY_AGENT = new MurderMysteryAgent(DevWidget.murderMysteryAgent);
    public static Events EVENTS = new Events();
    public static NotificationDisplay NOTIFICATION_DISPLAY;
    private static boolean addedNotificationDisplay = false;

    public static TextRenderer REGULAR;

    @Override
    public void onInitialize() {
        FileSystem.loadConfig();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.currentScreen instanceof TitleScreen) {
                if (addedNotificationDisplay)
                    return;
                REGULAR = Cigarette.tryGetTr(false);
                NOTIFICATION_DISPLAY = new NotificationDisplay();
            }
        });

        HudLayerRegistrationCallback.EVENT
                .register(layeredDrawer -> layeredDrawer.attachLayerAfter(IdentifiedLayer.MISC_OVERLAYS,
                        Identifier.of("cigarette", "notifications_after_misc_overlays"), (drawContext, tickDelta) -> {
                            if (NOTIFICATION_DISPLAY != null) {
                                Mouse m = MinecraftClient.getInstance().mouse;
                                Window w = MinecraftClient.getInstance().getWindow();
                                NOTIFICATION_DISPLAY.render(drawContext, (int) m.getScaledX(w), (int) m.getScaledY(w),
                                        tickDelta.getDynamicDeltaTicks());
                            }
                        }));
    }

    public static TextRenderer getTr(boolean bold) throws IOException {
        // MinecraftClient mc = MinecraftClient.getInstance();
        // List<Font.FontFilterPair> fontPairs = new ArrayList<>();
        // TrueTypeFontLoader loader = new TrueTypeFontLoader(
        //         Identifier.of("cigarette", (bold ? "bold" : "regular") + ".ttf"),
        //         11,
        //         11,
        //         TrueTypeFontLoader.Shift.NONE,
        //         "");
        // FontLoader.Loadable loadable = loader.build().orThrow();
        // Font font = loadable.load(mc.getResourceManager());
        // fontPairs.add(new Font.FontFilterPair(font, FilterMap.NO_FILTER));
        // FontStorage storage = new FontStorage(mc.getTextureManager(), Identifier.of("cigarette", "tr"));
        // storage.setFonts(fontPairs, java.util.Collections.emptySet());
        // return new TextRenderer(id -> storage, true);
        return MinecraftClient.getInstance().textRenderer;
    }

    public static TextRenderer tryGetTr(boolean bold) {
        try {
            TextRenderer tr = getTr(bold);
            TextRenderer fallbackTr = MinecraftClient.getInstance().textRenderer;
            return tr != null ? tr : fallbackTr;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}