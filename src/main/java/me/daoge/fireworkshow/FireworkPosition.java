package me.daoge.fireworkshow;

import lombok.Getter;
import lombok.Setter;
import org.allaymc.api.world.FireworkExplosion;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * Represents a firework spawn position with its configuration.
 */
@Getter
@Setter
public class FireworkPosition {
    private String worldName;
    private Vector3d pos;
    private boolean enabled;
    private boolean nightOnly;
    private int spawnTick;
    private int flightTimeMultiplier;
    private List<FireworkExplosion> explosions;

    private transient ScheduledFuture<?> taskHandle;

    public FireworkPosition(String worldName, Vector3d pos) {
        this(worldName, pos, true, false, 40, 1, new ArrayList<>());
    }

    public FireworkPosition(String worldName, Vector3d pos, boolean enabled, boolean nightOnly,
                            int spawnTick, int flightTimeMultiplier, List<FireworkExplosion> explosions) {
        this.worldName = worldName;
        this.pos = pos;
        this.enabled = enabled;
        this.nightOnly = nightOnly;
        this.spawnTick = spawnTick;
        this.flightTimeMultiplier = flightTimeMultiplier;
        this.explosions = explosions != null ? new ArrayList<>(explosions) : new ArrayList<>();
    }

    public int getFloorX() {
        return (int) Math.floor(pos.x());
    }

    public int getFloorY() {
        return (int) Math.floor(pos.y());
    }

    public int getFloorZ() {
        return (int) Math.floor(pos.z());
    }
}
