package space.visuals.base.comand.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import space.visuals.Zenith;
import space.visuals.base.comand.api.CommandAbstract;

import space.visuals.utility.game.other.MessageUtil;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ConfigCommand extends CommandAbstract {
    public ConfigCommand() {
        super("config");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {

        builder.then(literal("dir").executes(context -> {
            try {
                java.awt.Desktop.getDesktop().open(space.visuals.base.config.ConfigManager.configDirectory);
                MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "§aОткрываю папку с конфигами");
            } catch (Exception e) {
                MessageUtil.displayMessage(MessageUtil.LogLevel.WARN, "§cНе удалось открыть папку: " + e.getMessage());
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("save").executes(context -> {
            boolean success = Zenith.getInstance().getConfigManager().saveConfig("confeg");
            if (success) {
                MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                    "§aКонфигурация сохранена");
            } else {
                MessageUtil.displayMessage(MessageUtil.LogLevel.WARN,
                    "§cОшибка при сохранении конфигурации");
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("load").executes(context -> {
            boolean success = Zenith.getInstance().getConfigManager().loadConfig("confeg");
            if (success) {
                MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                    "§aКонфигурация загружена");
            } else {
                MessageUtil.displayMessage(MessageUtil.LogLevel.WARN,
                    "§cОшибка при загрузке конфигурации");
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("help").executes(context -> {
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                "§6Использование: §r.config <list/save/load/help>");
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                "§6Команды:§r");
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                "§e.config save §7- сохранить конфигурацию");
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                "§e.config load §7- загрузить конфигурацию");
            return SINGLE_SUCCESS;
        }));
    }
}
