package mrquackduck.messagesonhead.commands;

import mrquackduck.messagesonhead.configuration.Configuration;
import mrquackduck.messagesonhead.configuration.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class InfoCommand  implements CommandExecutor {
    private static final String NEW_LINE = "\n";
    private final Configuration config;

    public InfoCommand(Configuration config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        StringBuilder infoToShow = new StringBuilder();
        infoToShow.append(config.getMessage("info-title"));

        if (commandSender.hasPermission(Permissions.TOGGLE)) {
            infoToShow.append(NEW_LINE).append(config.getMessage("info-toggle-command-description"));
        }

        if (commandSender.hasPermission(Permissions.ADMIN)) {
            infoToShow.append(NEW_LINE).append(config.getMessage("info-reload-command-description"));
            infoToShow.append(NEW_LINE).append(config.getMessage("info-say-command-description"));
        }

        commandSender.sendMessage(infoToShow.toString());
        return true;
    }
}
