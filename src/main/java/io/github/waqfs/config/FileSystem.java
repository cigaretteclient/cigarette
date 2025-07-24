package io.github.waqfs.config;

import io.github.waqfs.Cigarette;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class FileSystem {
    private static final String CONFIG_NAME = "cigarette.toml";
    private static final String CONFIG_PATH = FabricLoader.getInstance().getConfigDir().getFileSystem().getPath(CONFIG_NAME).toString();
    private static final File CONFIG_FILE = new File(CONFIG_PATH);

    private static final Map<String, Object> OPTIONS = new HashMap<>();
    private static final Map<String, Consumer<Object>> OPTION_CALLBACKS = new HashMap<>();

    public static void registerUpdate(String toggle, Consumer<Object> callback) {
        OPTION_CALLBACKS.put(toggle, callback);
    }

    public static void updateState(String toggle, Object state) {
        OPTIONS.put(toggle, state);
        saveConfig();
    }

    private static Integer parseInt(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
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
                    String value = parts[1].trim();
                    Object parsedValue = null;
                    if (value.equals("true")) parsedValue = true;
                    else if (value.equals("false")) parsedValue = false;
                    else if (value.startsWith("\"") && value.endsWith("\""))
                        parsedValue = value.substring(1, value.length() - 2);
                    else if (value.matches("^[\\d.]+$")) parsedValue = FileSystem.parseInt(value);

                    if (OPTION_CALLBACKS.get(key) != null) {
                        @Nullable Object previous = OPTIONS.getOrDefault(key, null);
                        System.out.println("Cigarette -> config." + key + " = " + parsedValue);
                        if (previous == null || previous.equals(parsedValue)) {
                            OPTION_CALLBACKS.get(key).accept(parsedValue);
                        }
                    }
                    OPTIONS.put(key, parsedValue);
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
            for (Map.Entry<String, Object> entry : OPTIONS.entrySet()) {
                if (entry.getValue() instanceof String) {
                    writer.write(entry.getKey() + " = \"" + entry.getValue() + "\"\n");
                } else {
                    writer.write(entry.getKey() + " = " + entry.getValue() + "\n");
                }
            }
        } catch (Exception error) {
            Cigarette.LOGGER.error("An error occurred saving the configuration file.");
            error.printStackTrace();
        }
    }
}
