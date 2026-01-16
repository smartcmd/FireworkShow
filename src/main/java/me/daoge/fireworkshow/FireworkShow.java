package me.daoge.fireworkshow;

import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.interfaces.EntityFireworksRocket;
import org.allaymc.api.entity.type.EntityTypes;
import org.allaymc.api.message.I18n;
import org.allaymc.api.plugin.Plugin;
import org.allaymc.api.registry.Registries;
import org.allaymc.api.server.Server;
import org.allaymc.api.utils.DyeColor;
import org.allaymc.api.utils.config.Config;
import org.allaymc.api.utils.config.ConfigSection;
import org.allaymc.api.world.Dimension;
import org.allaymc.api.world.explosion.FireworkExplosion;
import org.allaymc.api.world.World;
import org.allaymc.api.world.data.FireworkType;
import org.joml.Vector3d;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * FireworkShow plugin - Spawns fireworks at configured positions.
 */
public class FireworkShow extends Plugin {
    private static FireworkShow instance;

    private Config config;
    private final Map<String, List<FireworkPosition>> positionsByWorld = new ConcurrentHashMap<>();
    private final Set<String> loadedWorlds = ConcurrentHashMap.newKeySet();

    private FireworkShowUI ui;

    @Override
    public void onLoad() {
        instance = this;
        pluginLogger.info("FireworkShow is loading...");
    }

    @Override
    public void onEnable() {
        loadConfig();

        ui = new FireworkShowUI(this);

        // Register command
        Registries.COMMANDS.register(new FireworkShowCommand(this, ui));

        // Register event listener
        Server.getInstance().getEventBus().registerListener(new FireworkShowEventListener(this));

        // Start tasks for already loaded worlds
        for (World world : Server.getInstance().getWorldPool().getWorlds().values()) {
            onWorldLoaded(world);
        }

        pluginLogger.info(I18n.get().tr(TrKeys.PLUGIN_ENABLED,
                positionsByWorld.values().stream().mapToInt(List::size).sum()));
    }

    @Override
    public void onDisable() {
        stopAllTasks();
        savePositionsToConfig();
        pluginLogger.info(I18n.get().tr(TrKeys.PLUGIN_DISABLED));
    }

    public static FireworkShow getInstance() {
        return instance;
    }

    public FireworkShowUI getUI() {
        return ui;
    }

    // ===================== Config Management =====================

    private void loadConfig() {
        File configFile = new File(getPluginContainer().dataFolder().toFile(), "config.yml");

        ConfigSection defaults = new ConfigSection();
        defaults.set("configVersion", 1);
        defaults.set("positions", new ArrayList<>());

        config = new Config(configFile, Config.YAML, defaults);
        loadConfigPositions();
    }

    @SuppressWarnings("unchecked")
    private void loadConfigPositions() {
        List<?> entries = config.getList("positions");
        if (entries == null) return;

        int counter = 0;
        for (Object entry : entries) {
            if (!(entry instanceof Map<?, ?> map)) continue;

            try {
                String worldName = getString(map, "worldName", getString(map, "world", "world"));
                int x = getInt(map, "x", 0);
                int y = getInt(map, "y", 64);
                int z = getInt(map, "z", 0);
                boolean enabled = getBoolean(map, "enabled", true);
                boolean nightOnly = getBoolean(map, "nightOnly", false);
                int spawnTick = getInt(map, "spawnTick", 40);
                int flight = getInt(map, "flightTimeMultiplier", 1);

                List<FireworkExplosion> explosions = new ArrayList<>();
                Object rawExplosions = map.get("explosions");
                if (rawExplosions instanceof List<?> explosionList) {
                    for (Object expObj : explosionList) {
                        if (!(expObj instanceof Map<?, ?> expMap)) continue;

                        try {
                            String typeRaw = getString(expMap, "type", "SMALL_SPHERE");
                            FireworkType type = FireworkUtils.parseFireworkType(typeRaw);

                            List<DyeColor> colors = parseColorList(expMap.get("colors"));
                            List<DyeColor> fadeColors = parseColorList(expMap.get("fade"));
                            boolean twinkle = getBoolean(expMap, "twinkle", false);
                            boolean trail = getBoolean(expMap, "trail", false);

                            if (!colors.isEmpty()) {
                                explosions.add(new FireworkExplosion(type, colors, fadeColors, twinkle, trail));
                            }
                        } catch (Exception e) {
                            pluginLogger.debug("Invalid explosion config: {}", e.getMessage());
                        }
                    }
                }

                FireworkPosition pos = new FireworkPosition(worldName, new Vector3d(x, y, z),
                        enabled, nightOnly, spawnTick, Math.max(1, Math.min(127, flight)), explosions);
                positionsByWorld.computeIfAbsent(worldName, k -> new ArrayList<>()).add(pos);
                counter++;
            } catch (Exception e) {
                pluginLogger.debug("Invalid position config: {}", e.getMessage());
            }
        }
        pluginLogger.debug("Loaded {} firework positions from config.", counter);
    }

    private List<DyeColor> parseColorList(Object raw) {
        List<DyeColor> result = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    DyeColor color = FireworkUtils.parseDyeColor(item.toString());
                    result.add(color);
                }
            }
        }
        return result;
    }

    private String getString(Map<?, ?> map, String key, String def) {
        Object val = map.get(key);
        return val != null ? val.toString() : def;
    }

    private int getInt(Map<?, ?> map, String key, int def) {
        Object val = map.get(key);
        if (val instanceof Number num) return num.intValue();
        if (val != null) {
            try {
                return Integer.parseInt(val.toString());
            } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private boolean getBoolean(Map<?, ?> map, String key, boolean def) {
        Object val = map.get(key);
        if (val instanceof Boolean b) return b;
        if (val != null) return Boolean.parseBoolean(val.toString());
        return def;
    }

    public void savePositionsToConfig() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, List<FireworkPosition>> entry : positionsByWorld.entrySet()) {
            for (FireworkPosition p : entry.getValue()) {
                Map<String, Object> posMap = new LinkedHashMap<>();
                posMap.put("worldName", p.getWorldName());
                posMap.put("x", p.getFloorX());
                posMap.put("y", p.getFloorY());
                posMap.put("z", p.getFloorZ());
                posMap.put("enabled", p.isEnabled());
                posMap.put("nightOnly", p.isNightOnly());
                posMap.put("spawnTick", p.getSpawnTick());
                posMap.put("flightTimeMultiplier", p.getFlightTimeMultiplier());

                List<Map<String, Object>> explosionsList = new ArrayList<>();
                for (FireworkExplosion e : p.getExplosions()) {
                    explosionsList.add(FireworkUtils.serializeExplosion(e));
                }
                posMap.put("explosions", explosionsList);

                out.add(posMap);
            }
        }
        config.set("positions", out);
        config.save();
    }

    // ===================== Position Management =====================

    public Map<String, List<FireworkPosition>> getPositionsByWorld() {
        return positionsByWorld;
    }

    public void addPosition(FireworkPosition pos) {
        positionsByWorld.computeIfAbsent(pos.getWorldName(), k -> new ArrayList<>()).add(pos);
        if (pos.isEnabled() && loadedWorlds.contains(pos.getWorldName())) {
            schedulePositionTask(pos);
        }
    }

    public boolean removePosition(String worldName, int index) {
        List<FireworkPosition> list = positionsByWorld.get(worldName);
        if (list == null || index < 0 || index >= list.size()) return false;

        FireworkPosition pos = list.remove(index);
        cancelTaskForPosition(pos);

        if (list.isEmpty()) {
            positionsByWorld.remove(worldName);
        }
        return true;
    }

    public boolean togglePosition(String worldName, int index) {
        List<FireworkPosition> list = positionsByWorld.get(worldName);
        if (list == null || index < 0 || index >= list.size()) return false;

        FireworkPosition pos = list.get(index);
        pos.setEnabled(!pos.isEnabled());

        if (pos.isEnabled()) {
            if (loadedWorlds.contains(worldName) && pos.getTaskHandle() == null) {
                schedulePositionTask(pos);
            }
        } else {
            cancelTaskForPosition(pos);
        }
        return true;
    }

    // ===================== Task Management =====================

    private void schedulePositionTask(FireworkPosition pos) {
        if (pos.getTaskHandle() != null) return;

        World world = Server.getInstance().getWorldPool().getWorld(pos.getWorldName());
        if (world == null) return;

        Dimension dimension = world.getOverWorld();
        if (dimension == null) return;

        dimension.getScheduler().scheduleRepeating(this, () -> {
            if (!pos.isEnabled()) return true;

            World w = Server.getInstance().getWorldPool().getWorld(pos.getWorldName());
            if (w == null) return true;

            // Night only check
            if (pos.isNightOnly()) {
                int timeOfDay = w.getWorldData().getTimeOfDay();
                if (!FireworkUtils.isNightTime(timeOfDay)) return true;
            }

            // Check if chunk is loaded
            Dimension dim = w.getOverWorld();
            int chunkX = pos.getFloorX() >> 4;
            int chunkZ = pos.getFloorZ() >> 4;
            if (!dim.getChunkManager().isChunkLoaded(chunkX, chunkZ)) return true;

            spawnFirework(dim, pos);
            return true;
        }, Math.max(1, pos.getSpawnTick()));
    }

    private void cancelTaskForPosition(FireworkPosition pos) {
        // Task scheduling in Allay doesn't provide direct cancellation handle via scheduleRepeating return
        // The task will stop when returning false or when the plugin is disabled
        pos.setTaskHandle(null);
    }

    private void stopAllTasks() {
        for (List<FireworkPosition> positions : positionsByWorld.values()) {
            for (FireworkPosition pos : positions) {
                cancelTaskForPosition(pos);
            }
        }
    }

    // ===================== World Events =====================

    public void onWorldLoaded(World world) {
        String worldName = world.getName();
        if (loadedWorlds.contains(worldName)) return;

        loadedWorlds.add(worldName);

        List<FireworkPosition> positions = positionsByWorld.get(worldName);
        if (positions == null) return;

        for (FireworkPosition pos : positions) {
            if (pos.isEnabled()) {
                schedulePositionTask(pos);
            }
        }

        pluginLogger.debug("Started firework tasks for world: {}", worldName);
    }

    public void onWorldUnloaded(World world) {
        String worldName = world.getName();
        if (!loadedWorlds.contains(worldName)) return;

        List<FireworkPosition> positions = positionsByWorld.get(worldName);
        if (positions != null) {
            for (FireworkPosition pos : positions) {
                cancelTaskForPosition(pos);
            }
        }

        loadedWorlds.remove(worldName);
        pluginLogger.debug("Stopped firework tasks for world: {}", worldName);
    }

    // ===================== Firework Spawning =====================

    public void spawnFirework(Dimension dimension, FireworkPosition pos) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Calculate random duration: ((flight + 1) * 10) + random(0..12)
        int randomDuration = ((pos.getFlightTimeMultiplier() + 1) * 10) + random.nextInt(13);

        // Random yaw for variety
        float yaw = random.nextFloat() * 360f;

        // Create firework entity
        EntityFireworksRocket firework = EntityTypes.FIREWORKS_ROCKET.createEntity(
                EntityInitInfo.builder()
                        .dimension(dimension)
                        .pos(pos.getPos().x() + 0.5, pos.getPos().y() + 1, pos.getPos().z() + 0.5)
                        .rot(yaw, 90f) // pitch 90 = straight up
                        .build()
        );

        // Set existence ticks (flight duration)
        firework.setExistenceTicks(randomDuration);

        // Set explosions if configured
        List<FireworkExplosion> explosions = pos.getExplosions();
        if (!explosions.isEmpty()) {
            firework.setExplosions(new HashSet<>(explosions));
        }

        // Spawn the entity
        dimension.getEntityManager().addEntity(firework);
    }
}
