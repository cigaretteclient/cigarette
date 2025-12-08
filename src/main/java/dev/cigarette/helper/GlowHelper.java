package dev.cigarette.helper;

import java.util.HashMap;
import java.util.UUID;

public class GlowHelper {
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
            GlowHelper.UUID_COLORS.put(entityUUID, color);
        }

        public void removeGlow(UUID entityUUID) {
            this.uuidColors.remove(entityUUID);
            GlowHelper.UUID_COLORS.remove(entityUUID);
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
                GlowHelper.UUID_COLORS.remove(entityUUID);
            }
            uuidColors.clear();
        }
    }
}
