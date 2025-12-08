package dev.cigarette.helper;

import java.util.HashMap;
import java.util.UUID;

/**
 * Helper class for managing overridden glow effects on entities.
 */
public class GlowHelper {
    private static final HashMap<UUID, Integer> UUID_COLORS = new HashMap<>();

    /**
     * {@return whether the entity has an overridden glow effect} This checks globally across all contexts, use {@link GlowHelper.Context#hasGlow} for context-specific checks.
     *
     * @param entityUUID The UUID of the entity to check
     */
    public static boolean hasGlow(UUID entityUUID) {
        return UUID_COLORS.containsKey(entityUUID);
    }

    /**
     * {@return the custom glow color for the entity, or 0 if none is set} This checks globally across all contexts, use {@link GlowHelper.Context#getGlowColor} for context-specific checks.
     *
     * @param entityUUID The UUID of the entity
     */
    public static int getGlowColor(UUID entityUUID) {
        Integer color = UUID_COLORS.get(entityUUID);
        if (color == null) return 0;
        return color;
    }

    /**
     * Context for managing glow effects on a module or feature basis.
     */
    public static class Context {
        private HashMap<UUID, Integer> uuidColors = new HashMap<>();

        /**
         * Attach a custom glow color to an entity.
         *
         * @param entityUUID The UUID of the entity
         * @param rgb        The color to set for the glow effect
         */
        public void addGlow(UUID entityUUID, int rgb) {
            this.uuidColors.put(entityUUID, rgb);
            GlowHelper.UUID_COLORS.put(entityUUID, rgb);
        }

        /**
         * Removes the custom glow effect from an entity.
         *
         * @param entityUUID The UUID of the entity
         */
        public void removeGlow(UUID entityUUID) {
            this.uuidColors.remove(entityUUID);
            GlowHelper.UUID_COLORS.remove(entityUUID);
        }

        /**
         * {@return whether the entity has an overridden glow effect in this context}
         *
         * @param entityUUID The UUID of the entity to check
         */
        public boolean hasGlow(UUID entityUUID) {
            return this.uuidColors.containsKey(entityUUID);
        }

        /**
         * {@return the custom glow color for the entity in this context, or 0 if none is set}
         *
         * @param entityUUID The UUID of the entity
         */
        public int getGlowColor(UUID entityUUID) {
            Integer color = this.uuidColors.get(entityUUID);
            if (color == null) return 0;
            return color;
        }

        /**
         * Removes all custom glow effects managed by this context.
         */
        public void removeAll() {
            for (UUID entityUUID : uuidColors.keySet()) {
                GlowHelper.UUID_COLORS.remove(entityUUID);
            }
            uuidColors.clear();
        }
    }
}
