package mrquackduck.messagesonhead.commands;

import mrquackduck.messagesonhead.MessagesOnHeadPlugin;
import mrquackduck.messagesonhead.configuration.Permissions;
import mrquackduck.messagesonhead.services.ToggleManager;
import mrquackduck.messagesonhead.services.MessageStackRepository;
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

    public MohCommand(MessagesOnHeadPlugin plugin, MessageStackRepository messageStackRepository, ToggleManager toggleManager) {
        this.plugin = plugin;
        this.messageStackRepository = messageStackRepository;
        this.toggleManager = toggleManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!allowedToRunAtLeastOneCommand(commandSender)) {
            commandSender.sendMessage(MessagesOnHeadPlugin.getMessage("command-not-found"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            return new InfoCommand().onCommand(commandSender, command, s, args);
        }
        else if (args[0].equalsIgnoreCase("say") && args.length >= 3 && commandSender.hasPermission(Permissions.ADMIN)) {
            return new SayCommand(plugin, messageStackRepository).onCommand(commandSender, command, s, args);
        }
        else if (args[0].equalsIgnoreCase("reload") && commandSender.hasPermission(Permissions.ADMIN)) {
            return new ReloadCommand(plugin).onCommand(commandSender, command, s, args);
        }
        else if (args[0].equalsIgnoreCase("toggle") && commandSender.hasPermission(Permissions.TOGGLE)) {
            return new ToggleCommand(toggleManager).onCommand(commandSender, command, s, args);
        }

        commandSender.sendMessage(MessagesOnHeadPlugin.getMessage("command-not-found"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        List<String> options = new ArrayList<>();
        List<String> completions = new ArrayList<>();

        if (args.length == 2 && args[0].equals("say") && commandSender.hasPermission(Permissions.ADMIN)) {
            for (Player p : Bukkit.getOnlinePlayers()) options.add(p.getName());
            StringUtil.copyPartialMatches(args[1], options, completions);
            return completions;
        }
        if (args.length != 1) return completions;

        if (allowedToRunAtLeastOneCommand(commandSender)) {
            options.add("info");
        }

        if (commandSender.hasPermission(Permissions.TOGGLE)) {
            options.add("toggle");
        }

        if (commandSender.hasPermission(Permissions.ADMIN)) {
            options.add("reload");
            options.add("say");
        }

        StringUtil.copyPartialMatches(args[0], options, completions);
        return completions;
    }

    private boolean allowedToRunAtLeastOneCommand(CommandSender commandSender) {
        return commandSender.hasPermission(Permissions.TOGGLE)
                || commandSender.hasPermission(Permissions.ADMIN);
    }
}
