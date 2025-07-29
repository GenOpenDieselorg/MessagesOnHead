package mrquackduck.messagesonhead.commands;

import mrquackduck.messagesonhead.services.ToggleManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ToggleCommand implements CommandExecutor {
    private final ToggleManager toggleManager;

    public ToggleCommand(ToggleManager toggleManager) {
        this.toggleManager = toggleManager;
    }

    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) commandSender;
        boolean currentlyOn = toggleManager.toggle(player);

        if (currentlyOn) player.sendMessage("You will now see messages over other player's heads");
        else player.sendMessage("You will no longer see messages over other player's heads");

        return true;
    }
}
