package mrquackduck.messagesonhead.commands;

import mrquackduck.messagesonhead.MessagesOnHeadPlugin;
import mrquackduck.messagesonhead.ToggleManager;
import mrquackduck.messagesonhead.classes.MessageStackRepository;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MohCommand implements CommandExecutor, TabCompleter {
    private final MessagesOnHeadPlugin plugin;
    private final MessageStackRepository messageStackRepository;
    private final ToggleManager toggleManager;

    public MohCommand(MessagesOnHeadPlugin plugin, MessageStackRepository messageStackRepository) {
        this.plugin = plugin;
        this.messageStackRepository = messageStackRepository;
        this.toggleManager = plugin.getToggleManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            return new InfoCommand().onCommand(commandSender, command, s, args);
        }
        else if (args[0].equalsIgnoreCase("say") && args.length >= 3 && commandSender.hasPermission("messagesonhead.admin")) {
            return new SayCommand(plugin, messageStackRepository).onCommand(commandSender, command, s, args);
        }
        else if (args[0].equalsIgnoreCase("reload") && commandSender.hasPermission("messagesonhead.admin")) {
            return new ReloadCommand(plugin).onCommand(commandSender, command, s, args);
        }
        else if (args[0].equalsIgnoreCase("toggle")) {
            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage("This command can only be used by players.");
                return true;
            }
            Player player = (Player) commandSender;
            boolean currentlyOff = toggleManager.isToggledOff(player);
            toggleManager.setToggled(player, !currentlyOff);
            if (currentlyOff) {
                player.sendMessage("You will now see messages over other player's heads");
            } else {
                player.sendMessage("You will no longer see messages over other player's heads");
            }
            return true;
        }

        commandSender.sendMessage(MessagesOnHeadPlugin.getMessage("command-not-found"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        List<String> options = new ArrayList<>();
        List<String> completions = new ArrayList<>();

        if (args.length == 2 && args[0].equals("say") && commandSender.hasPermission("messagesonhead.admin")) {
            for (Player p : Bukkit.getOnlinePlayers()) options.add(p.getName());
            StringUtil.copyPartialMatches(args[1], options, completions);
            return completions;
        }
        if (args.length != 1) return completions;

        options.add("toggle");
        if (commandSender.hasPermission("messagesonhead.admin")) {
            options.add("reload");
            options.add("info");
            options.add("say");
        }

        StringUtil.copyPartialMatches(args[0], options, completions);
        return completions;
    }
}
