package me.daoge.fireworkshow;

import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.eventbus.event.world.WorldLoadEvent;
import org.allaymc.api.eventbus.event.world.WorldUnloadEvent;

/**
 * Event listener for world load/unload events.
 */
public class FireworkShowEventListener {
    private final FireworkShow plugin;

    public FireworkShowEventListener(FireworkShow plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    private void onWorldLoad(WorldLoadEvent event) {
        plugin.onWorldLoaded(event.getWorld());
    }

    @EventHandler
    private void onWorldUnload(WorldUnloadEvent event) {
        plugin.onWorldUnloaded(event.getWorld());
    }
}
