package io.github.waqfs;

import io.github.waqfs.agent.BedwarsAgent;
import io.github.waqfs.agent.MurderMysteryAgent;
import io.github.waqfs.config.Config;
import io.github.waqfs.config.FileSystem;
import io.github.waqfs.lib.ChatLogger;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cigarette implements ModInitializer {
    public static final String MOD_ID = "cigarette";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ChatLogger CHAT_LOGGER = new ChatLogger();
    public static Config CONFIG = new Config();
    public static BedwarsAgent BEDWARS_AGENT = new BedwarsAgent();
    public static MurderMysteryAgent MURDER_MYSTERY_AGENT = new MurderMysteryAgent();

    @Override
    public void onInitialize() {
        FileSystem.loadConfig();
    }
}