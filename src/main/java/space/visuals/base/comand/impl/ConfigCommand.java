package space.visuals.base.comand.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import space.visuals.base.comand.api.CommandAbstract;
import space.visuals.base.config.ConfigManager;
import space.visuals.Zenith;
import space.visuals.utility.game.other.MessageUtil;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ConfigCommand extends CommandAbstract {
    public ConfigCommand() {
        super("cfg");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {

        // .cfg dir — открывает папку configs
        builder.then(literal("dir").executes(context -> {
            new Thread(() -> {
                try {
                    ConfigManager.configDirectory.mkdirs();
                    Runtime.getRuntime().exec(new String[]{"explorer", ConfigManager.configDirectory.getAbsolutePath()});
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "§aОткрываю папку configs");
            return SINGLE_SUCCESS;
        }));

        // .cfg save <name>
        builder.then(literal("save")
            .then(arg("name", StringArgumentType.word()).executes(context -> {
                String name = StringArgumentType.getString(context, "name");
                boolean ok = Zenith.getInstance().getConfigManager().saveConfig(name);
                MessageUtil.displayMessage(ok ? MessageUtil.LogLevel.INFO : MessageUtil.LogLevel.WARN,
                    ok ? "§aКонфиг §e" + name + "§a сохранён" : "§cОшибка при сохранении §e" + name);
                return SINGLE_SUCCESS;
            }))
        );

        // .cfg load <name>
        builder.then(literal("load")
            .then(arg("name", StringArgumentType.word())
                .suggests((context, suggestionsBuilder) -> {
                    // Получаем список всех конфигов
                    java.util.List<String> configs = Zenith.getInstance().getConfigManager().configNames();
                    // Добавляем их как предложения
                    for (String config : configs) {
                        suggestionsBuilder.suggest(config);
                    }
                    return suggestionsBuilder.buildFuture();
                })
                .executes(context -> {
                    String name = StringArgumentType.getString(context, "name");
                    boolean ok = Zenith.getInstance().getConfigManager().loadConfig(name);
                    MessageUtil.displayMessage(ok ? MessageUtil.LogLevel.INFO : MessageUtil.LogLevel.WARN,
                        ok ? "§aКонфиг §e" + name + "§a загружен" : "§cКонфиг §e" + name + "§c не найден");
                    return SINGLE_SUCCESS;
                }))
        );

        // .cfg del <name>
        builder.then(literal("del")
            .then(arg("name", StringArgumentType.word())
                .suggests((context, suggestionsBuilder) -> {
                    // Получаем список всех конфигов
                    java.util.List<String> configs = Zenith.getInstance().getConfigManager().configNames();
                    // Добавляем их как предложения
                    for (String config : configs) {
                        suggestionsBuilder.suggest(config);
                    }
                    return suggestionsBuilder.buildFuture();
                })
                .executes(context -> {
                    String name = StringArgumentType.getString(context, "name");
                    boolean ok = Zenith.getInstance().getConfigManager().deleteConfig(name);
                    MessageUtil.displayMessage(ok ? MessageUtil.LogLevel.INFO : MessageUtil.LogLevel.WARN,
                        ok ? "§aКонфиг §e" + name + "§a удалён" : "§cКонфиг §e" + name + "§c не найден");
                    return SINGLE_SUCCESS;
                }))
        );

        // .cfg list — список всех конфигов
        builder.then(literal("list").executes(context -> {
            java.util.List<String> names = Zenith.getInstance().getConfigManager().configNames();
            if (names.isEmpty()) {
                MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "§7Нет сохранённых конфигов");
            } else {
                MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "§aКонфиги: §e" + String.join("§7, §e", names));
            }
            return SINGLE_SUCCESS;
        }));
    }
}
