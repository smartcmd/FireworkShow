package me.daoge.fireworkshow;

import org.allaymc.api.command.Command;
import org.allaymc.api.command.SenderType;
import org.allaymc.api.command.tree.CommandTree;
import org.allaymc.api.message.I18n;
import org.joml.Vector3d;

import java.util.List;
import java.util.Map;

/**
 * Command handler for /fireworkshow.
 */
public class FireworkShowCommand extends Command {
    private final FireworkShow plugin;
    private final FireworkShowUI ui;

    public FireworkShowCommand(FireworkShow plugin, FireworkShowUI ui) {
        super("fireworkshow", TrKeys.COMMAND_DESCRIPTION, "fireworkshow.command.fireworkshow");
        this.plugin = plugin;
        this.ui = ui;
        aliases.add("fwshow");
        aliases.add("fws");
    }

    @Override
    public void prepareCommandTree(CommandTree tree) {
        tree.getRoot()
                // ui subcommand (player only - opens form)
                .key("ui")
                .exec((context, player) -> {
                    ui.openMainForm(player);
                    return context.success();
                }, SenderType.PLAYER)
                .root()
                // list subcommand
                .key("list")
                .exec(context -> {
                    Map<String, List<FireworkPosition>> positions = plugin.getPositionsByWorld();
                    if (positions.isEmpty()) {
                        context.addOutput(TrKeys.COMMAND_LIST_EMPTY);
                        return context.success();
                    }

                    context.addOutput(TrKeys.COMMAND_LIST_HEADER);
                    for (Map.Entry<String, List<FireworkPosition>> entry : positions.entrySet()) {
                        String world = entry.getKey();
                        List<FireworkPosition> list = entry.getValue();
                        context.addOutput(TrKeys.COMMAND_LIST_WORLD, world);
                        for (int i = 0; i < list.size(); i++) {
                            FireworkPosition pos = list.get(i);
                            String status = pos.isEnabled() ?
                                    I18n.get().tr(TrKeys.COMMAND_LIST_ENABLED) :
                                    I18n.get().tr(TrKeys.COMMAND_LIST_DISABLED);
                            String nightOnly = pos.isNightOnly() ?
                                    I18n.get().tr(TrKeys.COMMAND_LIST_NIGHTONLY) : "";
                            context.addOutput(TrKeys.COMMAND_LIST_POSITION,
                                    i, pos.getFloorX(), pos.getFloorY(), pos.getFloorZ(),
                                    status + nightOnly);
                        }
                    }
                    return context.success();
                })
                .root()
                // add subcommand with args (console compatible)
                .key("add")
                .str("world")
                .intNum("x")
                .intNum("y")
                .intNum("z")
                .exec(context -> {
                    String world = context.getResult(1);
                    int x = context.getResult(2);
                    int y = context.getResult(3);
                    int z = context.getResult(4);

                    FireworkPosition pos = new FireworkPosition(world, new Vector3d(x, y, z));
                    plugin.addPosition(pos);
                    plugin.savePositionsToConfig();

                    context.addOutput(TrKeys.COMMAND_ADD_SUCCESS, x, y, z, world);
                    return context.success();
                })
                .root()
                // remove subcommand
                .key("remove")
                .str("world")
                .intNum("index")
                .exec(context -> {
                    String world = context.getResult(1);
                    int index = context.getResult(2);

                    if (plugin.removePosition(world, index)) {
                        plugin.savePositionsToConfig();
                        context.addOutput(TrKeys.COMMAND_REMOVE_SUCCESS);
                    } else {
                        context.addOutput(TrKeys.COMMAND_REMOVE_NOTFOUND);
                    }
                    return context.success();
                })
                .root()
                // toggle subcommand
                .key("toggle")
                .str("world")
                .intNum("index")
                .exec(context -> {
                    String world = context.getResult(1);
                    int index = context.getResult(2);

                    if (plugin.togglePosition(world, index)) {
                        plugin.savePositionsToConfig();
                        List<FireworkPosition> positions = plugin.getPositionsByWorld().get(world);
                        if (positions != null && index < positions.size()) {
                            boolean enabled = positions.get(index).isEnabled();
                            context.addOutput(enabled ? TrKeys.COMMAND_TOGGLE_ENABLED : TrKeys.COMMAND_TOGGLE_DISABLED);
                        } else {
                            context.addOutput(TrKeys.COMMAND_TOGGLE_ENABLED);
                        }
                    } else {
                        context.addOutput(TrKeys.COMMAND_TOGGLE_NOTFOUND);
                    }
                    return context.success();
                });
    }
}
