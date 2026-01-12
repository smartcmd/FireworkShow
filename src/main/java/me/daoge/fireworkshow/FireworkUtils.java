package me.daoge.fireworkshow;

import org.allaymc.api.utils.DyeColor;
import org.allaymc.api.world.FireworkExplosion;
import org.allaymc.api.world.data.FireworkType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for firework creation and configuration parsing.
 */
public final class FireworkUtils {
    private FireworkUtils() {}

    private static final Map<String, FireworkType> TYPE_NAME_MAP = new HashMap<>();
    private static final Map<String, DyeColor> COLOR_NAME_MAP = new HashMap<>();

    static {
        for (FireworkType type : FireworkType.values()) {
            TYPE_NAME_MAP.put(type.name().toLowerCase(), type);
        }
        // Add aliases from PocketMine-MP
        TYPE_NAME_MAP.put("small_ball", FireworkType.SMALL_SPHERE);
        TYPE_NAME_MAP.put("large_ball", FireworkType.HUGE_SPHERE);
        TYPE_NAME_MAP.put("creeper", FireworkType.CREEPER_HEAD);

        for (DyeColor color : DyeColor.values()) {
            String name = color.name().toLowerCase();
            COLOR_NAME_MAP.put(name, color);
            // Also allow without underscores
            COLOR_NAME_MAP.put(name.replace("_", ""), color);
        }
    }

    /**
     * Parse a firework type from string (name or ordinal).
     */
    public static FireworkType parseFireworkType(String raw) {
        if (raw == null || raw.isEmpty()) {
            return FireworkType.SMALL_SPHERE;
        }

        String needle = raw.toLowerCase().trim();

        // Try name lookup
        FireworkType type = TYPE_NAME_MAP.get(needle);
        if (type != null) {
            return type;
        }

        // Try ordinal
        try {
            int ordinal = Integer.parseInt(raw);
            FireworkType[] values = FireworkType.values();
            if (ordinal >= 0 && ordinal < values.length) {
                return values[ordinal];
            }
        } catch (NumberFormatException ignored) {}

        return FireworkType.SMALL_SPHERE;
    }

    /**
     * Parse a dye color from string (name or ordinal).
     */
    public static DyeColor parseDyeColor(String raw) {
        if (raw == null || raw.isEmpty()) {
            return DyeColor.WHITE;
        }

        String needle = raw.toLowerCase().trim().replace(" ", "_").replace("-", "_");

        // Try name lookup
        DyeColor color = COLOR_NAME_MAP.get(needle);
        if (color != null) {
            return color;
        }

        // Try ordinal
        try {
            int ordinal = Integer.parseInt(raw);
            return DyeColor.from(ordinal);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {}

        return DyeColor.WHITE;
    }

    /**
     * Parse a comma-separated list of color names to a list of DyeColor.
     */
    public static List<DyeColor> parseDyeColorList(String raw) {
        List<DyeColor> result = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return result;
        }

        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                DyeColor color = parseDyeColor(trimmed);
                result.add(color);
            }
        }
        return result;
    }

    /**
     * Serialize a FireworkExplosion to a Map for config storage.
     */
    public static Map<String, Object> serializeExplosion(FireworkExplosion explosion) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", explosion.type().name());

        List<String> colors = new ArrayList<>();
        for (DyeColor c : explosion.colors()) {
            colors.add(c.name());
        }
        result.put("colors", colors);

        List<String> fade = new ArrayList<>();
        for (DyeColor c : explosion.fadeColors()) {
            fade.add(c.name());
        }
        result.put("fade", fade);

        result.put("twinkle", explosion.flicker());
        result.put("trail", explosion.trail());

        return result;
    }

    /**
     * Check if the given time of day is night time.
     * Night is approximately from 13000 to 23000 ticks.
     */
    public static boolean isNightTime(int timeOfDay) {
        return timeOfDay >= 13000 || timeOfDay < 0;
    }

    /**
     * Create a FireworkExplosion from configuration.
     */
    public static FireworkExplosion createExplosion(FireworkType type, List<DyeColor> colors,
                                                     List<DyeColor> fadeColors, boolean flicker, boolean trail) {
        return new FireworkExplosion(type, colors, fadeColors, flicker, trail);
    }
}
