package mrquackduck.messagesonhead.commands;

import mrquackduck.messagesonhead.MessagesOnHeadPlugin;
import mrquackduck.messagesonhead.configuration.Configuration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class ReloadCommand implements CommandExecutor {
    private final MessagesOnHeadPlugin plugin;
    private final Configuration config;

    public ReloadCommand(MessagesOnHeadPlugin plugin, Configuration config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        try {
            plugin.reload();
            commandSender.sendMessage((config.getMessage("reloaded")));
        }
        catch (Exception ex) {
            commandSender.sendMessage(config.getMessage("error-during-reload"));
            plugin.getLogger().log(Level.SEVERE, ex.getMessage());
        }

        return true;
    }
}
