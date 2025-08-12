package io.github.waqfs;

import io.github.waqfs.agent.BedwarsAgent;
import io.github.waqfs.agent.DevWidget;
import io.github.waqfs.agent.MurderMysteryAgent;
import io.github.waqfs.agent.ZombiesAgent;
import io.github.waqfs.config.Config;
import io.github.waqfs.config.FileSystem;
import io.github.waqfs.lib.ChatLogger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cigarette implements ModInitializer {
    public static final String MOD_ID = "cigarette";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ChatLogger CHAT_LOGGER = new ChatLogger();
    public static final boolean IN_DEV_ENVIRONMENT = FabricLoader.getInstance().isDevelopmentEnvironment();
    public static Config CONFIG = new Config();
    public static BedwarsAgent BEDWARS_AGENT = new BedwarsAgent(DevWidget.bedwarsAgent);
    public static MurderMysteryAgent MURDER_MYSTERY_AGENT = new MurderMysteryAgent(DevWidget.murderMysteryAgent);
    public static ZombiesAgent ZOMBIES_AGENT = new ZombiesAgent(DevWidget.zombiesAgent);

    @Override
    public void onInitialize() {
        FileSystem.loadConfig();
    }
}