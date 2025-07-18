package io.github.waqfs.lib;

import java.util.HashMap;
import java.util.UUID;

public class Glow {
    private static HashMap<UUID, Integer> UUID_COLORS = new HashMap<>();

    public static boolean hasGlow(UUID entityUUID) {
        return UUID_COLORS.containsKey(entityUUID);
    }

    public static int getGlowColor(UUID entityUUID) {
        Integer color = UUID_COLORS.get(entityUUID);
        if (color == null) return 0;
        return color;
    }

    public static class Context {
        private HashMap<UUID, Integer> uuidColors = new HashMap<>();

        public void addGlow(UUID entityUUID, int color) {
            this.uuidColors.put(entityUUID, color);
            Glow.UUID_COLORS.put(entityUUID, color);
        }

        public void removeGlow(UUID entityUUID) {
            this.uuidColors.remove(entityUUID);
            Glow.UUID_COLORS.remove(entityUUID);
        }

        public boolean hasGlow(UUID entityUUID) {
            return this.uuidColors.containsKey(entityUUID);
        }

        public int getGlowColor(UUID entityUUID) {
            Integer color = this.uuidColors.get(entityUUID);
            if (color == null) return 0;
            return color;
        }

        public void removeAll() {
            for (UUID entityUUID : uuidColors.keySet()) {
                Glow.UUID_COLORS.remove(entityUUID);
            }
            uuidColors.clear();
        }
    }
}
