package io.github.waqfs;

import io.github.waqfs.agent.BedwarsAgent;
import io.github.waqfs.agent.DevWidget;
import io.github.waqfs.agent.MurderMysteryAgent;
import io.github.waqfs.agent.ZombiesAgent;
import io.github.waqfs.config.Config;
import io.github.waqfs.config.FileSystem;
import io.github.waqfs.events.Events;
import io.github.waqfs.gui.hud.notification.NotificationDisplay;
import io.github.waqfs.lib.ChatLogger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.util.Pair;

public class Cigarette implements ModInitializer {
    public static final String MOD_ID = "cigarette";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ChatLogger CHAT_LOGGER = new ChatLogger();
    public static final Identifier LOGO_IDENTIFIER = Identifier.of("cigarette", "icon.png");
    public static final boolean IN_DEV_ENVIRONMENT = FabricLoader.getInstance().isDevelopmentEnvironment();
    public static Config CONFIG = new Config();
    public static BedwarsAgent BEDWARS_AGENT = new BedwarsAgent(DevWidget.bedwarsAgent);
    public static MurderMysteryAgent MURDER_MYSTERY_AGENT = new MurderMysteryAgent(DevWidget.murderMysteryAgent);
    public static ZombiesAgent ZOMBIES_AGENT = new ZombiesAgent(DevWidget.zombiesAgent);
    public static Events EVENTS = new Events();
    public static NotificationDisplay NOTIFICATION_DISPLAY;
    private static boolean addedNotificationDisplay = false;
    public static List<Pair<Vector4f, ClickableWidget>> HUD_ELEMENTS = new ArrayList<>();

    public static TextRenderer REGULAR;

    public static void registerHudElement(ClickableWidget widget) {
        HUD_ELEMENTS.add(new Pair<Vector4f, ClickableWidget>(new Vector4f(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight()), widget));
    }

    public static void unregisterHudElement(ClickableWidget widget) {
        HUD_ELEMENTS.removeIf(pair -> pair.getRight().equals(widget));
    }

    @Override
    public void onInitialize() {
        FileSystem.loadConfig();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.currentScreen instanceof TitleScreen) {
                if (addedNotificationDisplay)
                    return;
                REGULAR = Cigarette.tryGetTr(false);
                addedNotificationDisplay = true;
            }
        });

    HudLayerRegistrationCallback.EVENT
        .register(layeredDrawer -> layeredDrawer.attachLayerAfter(IdentifiedLayer.MISC_OVERLAYS,
            Identifier.of("cigarette", "hud_after_misc_overlays"), (drawContext, tickDelta) -> {
                Mouse m = MinecraftClient.getInstance().mouse;
                Window w = MinecraftClient.getInstance().getWindow();
                for (Pair<Vector4f, ClickableWidget> pair : HUD_ELEMENTS) {
                Vector4f dimensions = pair.getLeft();
                ClickableWidget widget = pair.getRight();
                int x = (int) dimensions.x;
                int y = (int) dimensions.y;
                int width = (int) dimensions.z;
                int height = (int) dimensions.w;
                widget.setDimensions(width, height);
                widget.setPosition(x, y);
                widget.render(drawContext, (int) m.getScaledX(w), (int) m.getScaledY(w),
                    tickDelta.getDynamicDeltaTicks());
                }
                if (NOTIFICATION_DISPLAY != null) {
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