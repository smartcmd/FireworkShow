package me.daoge.fireworkshow;

import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.form.Forms;
import org.allaymc.api.message.I18n;
import org.allaymc.api.player.Player;
import org.allaymc.api.utils.DyeColor;
import org.allaymc.api.world.explosion.FireworkExplosion;
import org.allaymc.api.world.data.FireworkType;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UI forms for managing firework positions in-game.
 */
public class FireworkShowUI {
    private final FireworkShow plugin;

    public FireworkShowUI(FireworkShow plugin) {
        this.plugin = plugin;
    }

    private String tr(String key, Object... args) {
        return I18n.get().tr(key, args);
    }

    public void openMainForm(EntityPlayer entityPlayer) {
        Player player = entityPlayer.getController();
        if (player == null) return;

        Map<String, List<FireworkPosition>> positions = plugin.getPositionsByWorld();

        var form = Forms.simple()
                .title(tr(TrKeys.UI_MAIN_TITLE));

        List<int[]> items = new ArrayList<>();
        List<String> worldNames = new ArrayList<>(positions.keySet());

        if (positions.isEmpty()) {
            form.content(tr(TrKeys.UI_MAIN_EMPTY));
        } else {
            form.content(tr(TrKeys.UI_MAIN_CONTENT));
            for (int w = 0; w < worldNames.size(); w++) {
                String world = worldNames.get(w);
                List<FireworkPosition> list = positions.get(world);
                for (int i = 0; i < list.size(); i++) {
                    FireworkPosition pos = list.get(i);
                    String status = pos.isEnabled() ? tr(TrKeys.UI_MAIN_STATUS_ON) : tr(TrKeys.UI_MAIN_STATUS_OFF);
                    String label = status + " " + world + " #" + i +
                            " (" + pos.getFloorX() + "," + pos.getFloorY() + "," + pos.getFloorZ() + ")";
                    int finalW = w;
                    int finalI = i;
                    form.button(label).onClick(btn -> openEditForm(entityPlayer, worldNames.get(finalW), finalI));
                    items.add(new int[]{w, i});
                }
            }
        }

        form.button(tr(TrKeys.UI_MAIN_ADD)).onClick(btn -> openAddForm(entityPlayer));
        form.sendTo(player);
    }

    public void openAddForm(EntityPlayer entityPlayer) {
        Player player = entityPlayer.getController();
        if (player == null) return;

        var loc = entityPlayer.getLocation();
        String worldName = entityPlayer.getDimension().getWorld().getName();

        List<String> typeNames = Arrays.stream(FireworkType.values())
                .map(Enum::name)
                .collect(Collectors.toList());

        Forms.custom()
                .title(tr(TrKeys.UI_ADD_TITLE))
                .label(tr(TrKeys.UI_ADD_HEADER_POSITION))
                .input(tr(TrKeys.UI_ADD_WORLD), worldName, worldName)
                .input("X", String.valueOf((int) loc.x()), String.valueOf((int) loc.x()))
                .input("Y", String.valueOf((int) loc.y()), String.valueOf((int) loc.y()))
                .input("Z", String.valueOf((int) loc.z()), String.valueOf((int) loc.z()))
                .label(tr(TrKeys.UI_ADD_HEADER_TIMING))
                .input(tr(TrKeys.UI_ADD_SPAWNTICK), "40", "40")
                .input(tr(TrKeys.UI_ADD_FLIGHT), "1", "1")
                .toggle(tr(TrKeys.UI_ADD_NIGHTONLY), false)
                .toggle(tr(TrKeys.UI_ADD_ENABLED), true)
                .label(tr(TrKeys.UI_ADD_HEADER_EXPLOSION))
                .dropdown(tr(TrKeys.UI_ADD_EXPLOSIONTYPE), typeNames, 0)
                .input(tr(TrKeys.UI_ADD_COLORS), "red,yellow", "red,yellow")
                .input(tr(TrKeys.UI_ADD_FADECOLORS), "", "")
                .toggle(tr(TrKeys.UI_ADD_TWINKLE), true)
                .toggle(tr(TrKeys.UI_ADD_TRAIL), true)
                .onResponse(responses -> {
                    try {
                        // Label elements take up indices: 0=posLabel, 5=timingLabel, 10=explosionLabel
                        String world = responses.get(1);
                        int x = Integer.parseInt(responses.get(2));
                        int y = Integer.parseInt(responses.get(3));
                        int z = Integer.parseInt(responses.get(4));
                        int spawnTick = Integer.parseInt(responses.get(6));
                        int flight = Math.max(1, Math.min(127, Integer.parseInt(responses.get(7))));
                        boolean nightOnly = Boolean.parseBoolean(responses.get(8));
                        boolean enabled = Boolean.parseBoolean(responses.get(9));

                        // Explosion settings
                        int typeIndex = Integer.parseInt(responses.get(11));
                        String colorsRaw = responses.get(12);
                        String fadeRaw = responses.get(13);
                        boolean twinkle = Boolean.parseBoolean(responses.get(14));
                        boolean trail = Boolean.parseBoolean(responses.get(15));

                        List<FireworkExplosion> explosions = new ArrayList<>();
                        List<DyeColor> colors = FireworkUtils.parseDyeColorList(colorsRaw);
                        if (!colors.isEmpty()) {
                            FireworkType type = FireworkType.values()[typeIndex];
                            List<DyeColor> fadeColors = FireworkUtils.parseDyeColorList(fadeRaw);
                            explosions.add(new FireworkExplosion(type, colors, fadeColors, twinkle, trail));
                        }

                        FireworkPosition pos = new FireworkPosition(world, new Vector3d(x, y, z),
                                enabled, nightOnly, spawnTick, flight, explosions);
                        plugin.addPosition(pos);
                        plugin.savePositionsToConfig();

                        player.sendMessage(tr(TrKeys.UI_ADD_SUCCESS));
                        openMainForm(entityPlayer);
                    } catch (Exception e) {
                        player.sendMessage(tr(TrKeys.UI_ERROR, e.getMessage()));
                    }
                })
                .onClose(reason -> openMainForm(entityPlayer))
                .sendTo(player);
    }

    private void openEditForm(EntityPlayer entityPlayer, String worldName, int index) {
        Player player = entityPlayer.getController();
        if (player == null) return;

        List<FireworkPosition> list = plugin.getPositionsByWorld().get(worldName);
        if (list == null || index >= list.size()) {
            player.sendMessage(tr(TrKeys.UI_NOTFOUND));
            openMainForm(entityPlayer);
            return;
        }

        FireworkPosition pos = list.get(index);

        String statusText = pos.isEnabled() ?
                tr(TrKeys.UI_EDIT_INFO_STATUS_ENABLED) :
                tr(TrKeys.UI_EDIT_INFO_STATUS_DISABLED);

        String content = tr(TrKeys.UI_EDIT_INFO_WORLD, worldName, index) + "\n" +
                tr(TrKeys.UI_EDIT_INFO_POSITION, pos.getFloorX(), pos.getFloorY(), pos.getFloorZ()) + "\n" +
                tr(TrKeys.UI_EDIT_INFO_STATUS, statusText) + "\n" +
                tr(TrKeys.UI_EDIT_INFO_NIGHTONLY, pos.isNightOnly()) + "\n" +
                tr(TrKeys.UI_EDIT_INFO_SPAWNTICK, pos.getSpawnTick()) + "\n" +
                tr(TrKeys.UI_EDIT_INFO_FLIGHT, pos.getFlightTimeMultiplier()) + "\n" +
                tr(TrKeys.UI_EDIT_INFO_EXPLOSIONS, pos.getExplosions().size());

        var form = Forms.simple()
                .title(tr(TrKeys.UI_EDIT_TITLE))
                .content(content);

        form.button(tr(TrKeys.UI_EDIT_SETTINGS)).onClick(btn -> openEditSettingsForm(entityPlayer, worldName, index));
        form.button(tr(TrKeys.UI_EDIT_EXPLOSIONS)).onClick(btn -> openExplosionsForm(entityPlayer, worldName, index));
        form.button(pos.isEnabled() ? tr(TrKeys.UI_EDIT_DISABLE) : tr(TrKeys.UI_EDIT_ENABLE)).onClick(btn -> {
            plugin.togglePosition(worldName, index);
            plugin.savePositionsToConfig();
            player.sendMessage(pos.isEnabled() ?
                    tr(TrKeys.COMMAND_TOGGLE_DISABLED) :
                    tr(TrKeys.COMMAND_TOGGLE_ENABLED));
            openEditForm(entityPlayer, worldName, index);
        });
        form.button(tr(TrKeys.UI_EDIT_DELETE)).onClick(btn -> {
            Forms.modal()
                    .title(tr(TrKeys.UI_DELETE_TITLE))
                    .content(tr(TrKeys.UI_DELETE_CONTENT))
                    .trueButton(tr(TrKeys.UI_DELETE_CONFIRM), () -> {
                        plugin.removePosition(worldName, index);
                        plugin.savePositionsToConfig();
                        player.sendMessage(tr(TrKeys.UI_DELETE_SUCCESS));
                        openMainForm(entityPlayer);
                    })
                    .falseButton(tr(TrKeys.UI_DELETE_CANCEL), () -> openEditForm(entityPlayer, worldName, index))
                    .sendTo(player);
        });
        form.button(tr(TrKeys.UI_EDIT_BACK)).onClick(btn -> openMainForm(entityPlayer));
        form.sendTo(player);
    }

    private void openEditSettingsForm(EntityPlayer entityPlayer, String worldName, int index) {
        Player player = entityPlayer.getController();
        if (player == null) return;

        List<FireworkPosition> list = plugin.getPositionsByWorld().get(worldName);
        if (list == null || index >= list.size()) {
            openMainForm(entityPlayer);
            return;
        }

        FireworkPosition pos = list.get(index);

        Forms.custom()
                .title(tr(TrKeys.UI_SETTINGS_TITLE))
                .input(tr(TrKeys.UI_ADD_WORLD), pos.getWorldName(), pos.getWorldName())
                .input("X", String.valueOf(pos.getFloorX()), String.valueOf(pos.getFloorX()))
                .input("Y", String.valueOf(pos.getFloorY()), String.valueOf(pos.getFloorY()))
                .input("Z", String.valueOf(pos.getFloorZ()), String.valueOf(pos.getFloorZ()))
                .input(tr(TrKeys.UI_ADD_SPAWNTICK), String.valueOf(pos.getSpawnTick()), String.valueOf(pos.getSpawnTick()))
                .input(tr(TrKeys.UI_ADD_FLIGHT), String.valueOf(pos.getFlightTimeMultiplier()), String.valueOf(pos.getFlightTimeMultiplier()))
                .toggle(tr(TrKeys.UI_ADD_NIGHTONLY), pos.isNightOnly())
                .toggle(tr(TrKeys.UI_ADD_ENABLED), pos.isEnabled())
                .onResponse(responses -> {
                    try {
                        String newWorld = responses.get(0);
                        int x = Integer.parseInt(responses.get(1));
                        int y = Integer.parseInt(responses.get(2));
                        int z = Integer.parseInt(responses.get(3));
                        int spawnTick = Integer.parseInt(responses.get(4));
                        int flight = Math.max(1, Math.min(127, Integer.parseInt(responses.get(5))));
                        boolean nightOnly = Boolean.parseBoolean(responses.get(6));
                        boolean enabled = Boolean.parseBoolean(responses.get(7));

                        String oldWorld = pos.getWorldName();
                        pos.setPos(new Vector3d(x, y, z));
                        pos.setSpawnTick(spawnTick);
                        pos.setFlightTimeMultiplier(flight);
                        pos.setNightOnly(nightOnly);
                        pos.setEnabled(enabled);

                        // Handle world change
                        if (!newWorld.equals(oldWorld)) {
                            plugin.removePosition(oldWorld, index);
                            pos.setWorldName(newWorld);
                            plugin.addPosition(pos);
                        }

                        plugin.savePositionsToConfig();
                        player.sendMessage(tr(TrKeys.UI_SETTINGS_SUCCESS));
                        openEditForm(entityPlayer, newWorld, index);
                    } catch (Exception e) {
                        player.sendMessage(tr(TrKeys.UI_ERROR, e.getMessage()));
                    }
                })
                .onClose(reason -> openEditForm(entityPlayer, worldName, index))
                .sendTo(player);
    }

    private void openExplosionsForm(EntityPlayer entityPlayer, String worldName, int index) {
        Player player = entityPlayer.getController();
        if (player == null) return;

        List<FireworkPosition> list = plugin.getPositionsByWorld().get(worldName);
        if (list == null || index >= list.size()) {
            openMainForm(entityPlayer);
            return;
        }

        FireworkPosition pos = list.get(index);
        List<FireworkExplosion> explosions = pos.getExplosions();

        var form = Forms.simple()
                .title(tr(TrKeys.UI_EXPLOSIONS_TITLE))
                .content(tr(TrKeys.UI_EXPLOSIONS_CONTENT, worldName, index));

        for (int i = 0; i < explosions.size(); i++) {
            FireworkExplosion exp = explosions.get(i);
            String colorStr = exp.colors().stream()
                    .map(DyeColor::name)
                    .collect(Collectors.joining(","));
            String label = "#" + i + ": " + exp.type().name() + " [" + colorStr + "]";
            int finalI = i;
            form.button(label).onClick(btn -> openEditExplosionForm(entityPlayer, worldName, index, finalI));
        }

        form.button(tr(TrKeys.UI_EXPLOSIONS_ADD)).onClick(btn -> openAddExplosionForm(entityPlayer, worldName, index));
        form.button(tr(TrKeys.UI_EDIT_BACK)).onClick(btn -> openEditForm(entityPlayer, worldName, index));
        form.sendTo(player);
    }

    private void openAddExplosionForm(EntityPlayer entityPlayer, String worldName, int index) {
        Player player = entityPlayer.getController();
        if (player == null) return;

        List<String> typeNames = Arrays.stream(FireworkType.values())
                .map(Enum::name)
                .collect(Collectors.toList());

        Forms.custom()
                .title(tr(TrKeys.UI_EXPLOSION_ADD_TITLE))
                .dropdown(tr(TrKeys.UI_EXPLOSION_ADD_TYPE), typeNames, 0)
                .input(tr(TrKeys.UI_EXPLOSION_ADD_COLORS), "red,yellow", "red,yellow")
                .input(tr(TrKeys.UI_EXPLOSION_ADD_FADECOLORS), "", "")
                .toggle(tr(TrKeys.UI_EXPLOSION_ADD_TWINKLE), false)
                .toggle(tr(TrKeys.UI_EXPLOSION_ADD_TRAIL), false)
                .onResponse(responses -> {
                    try {
                        int typeIndex = Integer.parseInt(responses.get(0));
                        String colorsRaw = responses.get(1);
                        String fadeRaw = responses.get(2);
                        boolean twinkle = Boolean.parseBoolean(responses.get(3));
                        boolean trail = Boolean.parseBoolean(responses.get(4));

                        List<DyeColor> colors = FireworkUtils.parseDyeColorList(colorsRaw);
                        if (colors.isEmpty()) {
                            player.sendMessage(tr(TrKeys.UI_EXPLOSION_ADD_ERROR_NOCOLORS));
                            return;
                        }

                        FireworkType type = FireworkType.values()[typeIndex];
                        List<DyeColor> fadeColors = FireworkUtils.parseDyeColorList(fadeRaw);
                        FireworkExplosion explosion = new FireworkExplosion(type, colors, fadeColors, twinkle, trail);

                        List<FireworkPosition> list = plugin.getPositionsByWorld().get(worldName);
                        if (list != null && index < list.size()) {
                            list.get(index).getExplosions().add(explosion);
                            plugin.savePositionsToConfig();
                            player.sendMessage(tr(TrKeys.UI_EXPLOSION_ADD_SUCCESS));
                        }
                        openExplosionsForm(entityPlayer, worldName, index);
                    } catch (Exception e) {
                        player.sendMessage(tr(TrKeys.UI_ERROR, e.getMessage()));
                    }
                })
                .onClose(reason -> openExplosionsForm(entityPlayer, worldName, index))
                .sendTo(player);
    }

    private void openEditExplosionForm(EntityPlayer entityPlayer, String worldName, int posIndex, int expIndex) {
        Player player = entityPlayer.getController();
        if (player == null) return;

        List<FireworkPosition> list = plugin.getPositionsByWorld().get(worldName);
        if (list == null || posIndex >= list.size()) {
            openMainForm(entityPlayer);
            return;
        }

        FireworkPosition pos = list.get(posIndex);
        List<FireworkExplosion> explosions = pos.getExplosions();
        if (expIndex >= explosions.size()) {
            openExplosionsForm(entityPlayer, worldName, posIndex);
            return;
        }

        FireworkExplosion exp = explosions.get(expIndex);

        List<String> typeNames = Arrays.stream(FireworkType.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        int currentTypeIndex = exp.type().ordinal();

        String currentColors = exp.colors().stream()
                .map(DyeColor::name)
                .collect(Collectors.joining(","));
        String currentFade = exp.fadeColors().stream()
                .map(DyeColor::name)
                .collect(Collectors.joining(","));

        Forms.custom()
                .title(tr(TrKeys.UI_EXPLOSION_EDIT_TITLE, expIndex))
                .dropdown(tr(TrKeys.UI_EXPLOSION_ADD_TYPE), typeNames, currentTypeIndex)
                .input(tr(TrKeys.UI_EXPLOSION_ADD_COLORS), currentColors, currentColors)
                .input(tr(TrKeys.UI_EXPLOSION_ADD_FADECOLORS), currentFade, currentFade)
                .toggle(tr(TrKeys.UI_EXPLOSION_ADD_TWINKLE), exp.flicker())
                .toggle(tr(TrKeys.UI_EXPLOSION_ADD_TRAIL), exp.trail())
                .toggle(tr(TrKeys.UI_EXPLOSION_EDIT_DELETE), false)
                .onResponse(responses -> {
                    try {
                        boolean delete = Boolean.parseBoolean(responses.get(5));
                        if (delete) {
                            explosions.remove(expIndex);
                            plugin.savePositionsToConfig();
                            player.sendMessage(tr(TrKeys.UI_EXPLOSION_EDIT_DELETED));
                            openExplosionsForm(entityPlayer, worldName, posIndex);
                            return;
                        }

                        int typeIndex = Integer.parseInt(responses.get(0));
                        String colorsRaw = responses.get(1);
                        String fadeRaw = responses.get(2);
                        boolean twinkle = Boolean.parseBoolean(responses.get(3));
                        boolean trail = Boolean.parseBoolean(responses.get(4));

                        List<DyeColor> colors = FireworkUtils.parseDyeColorList(colorsRaw);
                        if (colors.isEmpty()) {
                            player.sendMessage(tr(TrKeys.UI_EXPLOSION_ADD_ERROR_NOCOLORS));
                            return;
                        }

                        FireworkType type = FireworkType.values()[typeIndex];
                        List<DyeColor> fadeColors = FireworkUtils.parseDyeColorList(fadeRaw);
                        FireworkExplosion newExp = new FireworkExplosion(type, colors, fadeColors, twinkle, trail);

                        explosions.set(expIndex, newExp);
                        plugin.savePositionsToConfig();
                        player.sendMessage(tr(TrKeys.UI_EXPLOSION_EDIT_SUCCESS));
                        openExplosionsForm(entityPlayer, worldName, posIndex);
                    } catch (Exception e) {
                        player.sendMessage(tr(TrKeys.UI_ERROR, e.getMessage()));
                    }
                })
                .onClose(reason -> openExplosionsForm(entityPlayer, worldName, posIndex))
                .sendTo(player);
    }
}
