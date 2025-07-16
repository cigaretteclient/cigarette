package io.github.waqfs;

import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ConfigHandler {
    private static final String CONFIG_NAME = "cigarette.toml";
    private static final String CONFIG_PATH = FabricLoader.getInstance().getConfigDir().getFileSystem().getPath(CONFIG_NAME).toString();
    private static final File CONFIG_FILE = new File(CONFIG_PATH);

    private static final Map<String, Boolean> TOGGLES = new HashMap<>();
    private static final Map<String, Consumer<Boolean>> TOGGLE_CALLBACKS = new HashMap<>();

    public static void registerUpdate(String toggle, Consumer<Boolean> callback) {
        TOGGLE_CALLBACKS.put(toggle, callback);
    }

    public static void updateState(String toggle, boolean state) {
        TOGGLES.put(toggle, state);
        saveConfig();
    }

    public static void loadConfig() {
        if (!CONFIG_FILE.exists()) {
            try {
                CONFIG_FILE.createNewFile();
            } catch (Exception error) {
                Cigarette.LOGGER.error("An error occurred creating the configuration file.");
                error.printStackTrace();
                return;
            }
            loadConfig();
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    boolean value = Boolean.parseBoolean(parts[1].trim());
                    if (TOGGLE_CALLBACKS.get(key) != null) {
                        @Nullable Boolean previous = TOGGLES.get(key);
                        if (previous == null || previous != value) {
                            TOGGLE_CALLBACKS.get(key).accept(value);
                        }
                    }
                    TOGGLES.put(key, value);
                }
            }
        } catch (Exception error) {
            Cigarette.LOGGER.error("An error occurred loading the configuration file.");
            error.printStackTrace();
        }
    }

    public static void saveConfig() {
        if (!CONFIG_FILE.exists()) {
            try {
                CONFIG_FILE.createNewFile();
            } catch (Exception error) {
                Cigarette.LOGGER.error("An error occurred creating the configuration file.");
                error.printStackTrace();
                return;
            }
        }
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            for (Map.Entry<String, Boolean> entry : TOGGLES.entrySet()) {
                writer.write(entry.getKey() + " = " + entry.getValue() + "\n");
            }
        } catch (Exception error) {
            Cigarette.LOGGER.error("An error occurred saving the configuration file.");
            error.printStackTrace();
        }
    }
}
