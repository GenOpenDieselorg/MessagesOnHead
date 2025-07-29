package mrquackduck.messagesonhead.commands;

import mrquackduck.messagesonhead.configuration.Configuration;
import mrquackduck.messagesonhead.services.ToggleManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ToggleCommand implements CommandExecutor {
    private final Configuration config;
    private final ToggleManager toggleManager;

    public ToggleCommand(Configuration config, ToggleManager toggleManager) {
        this.config = config;
        this.toggleManager = toggleManager;
    }

    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(config.getMessage("only-players"));
            return true;
        }

        Player player = (Player) commandSender;
        boolean currentlyOn = toggleManager.toggle(player);

        if (currentlyOn) player.sendMessage(config.getMessage("visibility-toggled-on"));
        else player.sendMessage(config.getMessage("visibility-toggled-off"));

        return true;
    }
}
