package space.visuals.base.comand.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import space.visuals.Zenith;
import space.visuals.base.comand.api.CommandAbstract;
import space.visuals.utility.game.other.MessageUtil;
import space.visuals.utility.interfaces.IMinecraft;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class WaypointCommand extends CommandAbstract implements IMinecraft {

    public WaypointCommand() {
        super("way");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {

        // Показываем помощь если вызвали просто .way
        builder.executes(ctx -> {
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                "Использование: .way add/del/clear/list/here");
            return SINGLE_SUCCESS;
        });
        builder.then(literal("add")
            .then(arg("name", StringArgumentType.word())
                .then(arg("x", IntegerArgumentType.integer())
                    .then(arg("y", IntegerArgumentType.integer())
                        .then(arg("z", IntegerArgumentType.integer())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                int x = IntegerArgumentType.getInteger(ctx, "x");
                                int y = IntegerArgumentType.getInteger(ctx, "y");
                                int z = IntegerArgumentType.getInteger(ctx, "z");
                                Zenith.getInstance().getWaypointManager().add(name, x, y, z);
                                return SINGLE_SUCCESS;
                            })
                        )
                    )
                )
                // .way add <name> — добавить на текущей позиции
                .executes(ctx -> {
                    if (mc.player == null) return SINGLE_SUCCESS;
                    String name = StringArgumentType.getString(ctx, "name");
                    Zenith.getInstance().getWaypointManager().add(
                        name,
                        mc.player.getBlockX(),
                        mc.player.getBlockY(),
                        mc.player.getBlockZ()
                    );
                    return SINGLE_SUCCESS;
                })
            )
        );

        // .way del <name>
        builder.then(literal("del")
            .then(arg("name", StringArgumentType.word())
                .executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "name");
                    Zenith.getInstance().getWaypointManager().remove(name);
                    return SINGLE_SUCCESS;
                })
            )
        );

        // .way clear
        builder.then(literal("clear")
            .executes(ctx -> {
                Zenith.getInstance().getWaypointManager().clear();
                return SINGLE_SUCCESS;
            })
        );

        // .way list
        builder.then(literal("list")
            .executes(ctx -> {
                var waypoints = Zenith.getInstance().getWaypointManager().getWaypoints();
                if (waypoints.isEmpty()) {
                    MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "Меток нет");
                } else {
                    waypoints.forEach(w -> MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                        w.name + " → " + String.format("%.0f %.0f %.0f", w.pos.x, w.pos.y, w.pos.z)));
                }
                return SINGLE_SUCCESS;
            })
        );

        // .way here — добавить метку на текущей позиции с авто-именем
        builder.then(literal("here")
            .executes(ctx -> {
                if (mc.player == null) return SINGLE_SUCCESS;
                String name = "Метка " + (Zenith.getInstance().getWaypointManager().getWaypoints().size() + 1);
                Zenith.getInstance().getWaypointManager().add(
                    name,
                    mc.player.getBlockX(),
                    mc.player.getBlockY(),
                    mc.player.getBlockZ()
                );
                return SINGLE_SUCCESS;
            })
        );
    }
}
