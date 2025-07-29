package mrquackduck.messagesonhead;

import com.tchristofferson.configupdater.ConfigUpdater;
import mrquackduck.messagesonhead.services.MessageStackRepository;
import mrquackduck.messagesonhead.commands.MohCommand;
import mrquackduck.messagesonhead.listeners.PlayerConnectionListener;
import mrquackduck.messagesonhead.listeners.SendMessageListener;
import mrquackduck.messagesonhead.services.ToggleManager;
import mrquackduck.messagesonhead.utils.MessageColorizer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MessagesOnHeadPlugin extends JavaPlugin {
    private Logger logger;
    private MessageStackRepository messageStackRepository;
    private static Map<String, String> messages = new HashMap<>();
    private ToggleManager toggleManager;

    @Override
    public void onEnable() {
        // Setting up a logger
        logger = getLogger();

        // Setup message stack repository
        this.messageStackRepository = new MessageStackRepository(this);
        this.toggleManager = new ToggleManager(getDataFolder());

        // Register listeners
        getServer().getPluginManager().registerEvents(new SendMessageListener(messageStackRepository), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(messageStackRepository, toggleManager), this);

        // Starting the plugin
        try { start(); }
        catch (RuntimeException e) { getLogger().log(Level.SEVERE, e.getMessage()); }

        // Registering commands
        Objects.requireNonNull(getServer().getPluginCommand("messagesonhead")).setExecutor(new MohCommand(this, messageStackRepository));
    }

    @Override
    public void onDisable() {
        // Remove all entities related to the plugin
        messageStackRepository.cleanUp();
    }

    private void start() {
        // Save default configuration
        saveDefaultConfig();

        // Updating the config with missing key-pairs (and removing redundant ones if present)
        File configFile = new File(getDataFolder(), "config.yml");
        try { ConfigUpdater.update(this, "config.yml", configFile, new ArrayList<>()); }
        catch (IOException e) { e.printStackTrace(); }

        // Reloading the messages from config
        setupMessages();

        // Remove all entities related to the plugin
        messageStackRepository.cleanUp();

        // Load toggled state for all currently online players
        for (Player player : getServer().getOnlinePlayers()) {
            toggleManager.onPlayerJoin(player);
        }
    }

    public void reload() {
        // Reloading the config
        reloadConfig();

        // Reload toggled-off list from file
        if (toggleManager != null) toggleManager.reload();

        // Starting the plugin again
        start();

        logger.info("Plugin restarted!");
    }

    private void setupMessages() {
        messages = new HashMap<>();

        // Getting the messages from the config
        ConfigurationSection configSection = getConfig().getConfigurationSection("messages");
        if (configSection != null) {
            // Adding these messages to dictionary
            Map<String, Object> messages = configSection.getValues(true);
            for (Map.Entry<String, Object> pair : messages.entrySet()) {
                MessagesOnHeadPlugin.messages.put(pair.getKey(), pair.getValue().toString());
            }
        }

        saveDefaultConfig();
    }

    // Returns a message from the config by key
    public static String getMessage(String key) {
        if (messages == null) return String.format("Message %s wasn't found (messages list is null)", key);
        if (messages.get(key) == null) return String.format("Message %s wasn't found", key);

        return MessageColorizer.colorize(messages.get(key).replace("<prefix>", messages.get("prefix")));
    }

    public ToggleManager getToggleManager() {
        return toggleManager;
    }
}
