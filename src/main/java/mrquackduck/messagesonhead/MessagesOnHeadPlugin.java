package mrquackduck.messagesonhead;

import com.tchristofferson.configupdater.ConfigUpdater;
import mrquackduck.messagesonhead.configuration.Configuration;
import mrquackduck.messagesonhead.listeners.ZelChatListener;
import mrquackduck.messagesonhead.services.MessageStackRepository;
import mrquackduck.messagesonhead.commands.MohCommand;
import mrquackduck.messagesonhead.listeners.PlayerConnectionListener;
import mrquackduck.messagesonhead.listeners.SendMessageListener;
import mrquackduck.messagesonhead.services.ToggleManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MessagesOnHeadPlugin extends JavaPlugin {
    private final Logger logger = getLogger();
    private final Configuration config = new Configuration(this);
    private MessageStackRepository messageStackRepository;
    private ToggleManager toggleManager;

    @Override
    public void onEnable() {
        // Setup services
        this.toggleManager = new ToggleManager(getDataFolder());
        this.messageStackRepository = new MessageStackRepository(this, toggleManager);

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(messageStackRepository, toggleManager), this);

        // Sprawdź, czy ZelChat jest na serwerze i zarejestruj odpowiedni listener
        if (Bukkit.getPluginManager().isPluginEnabled("ZelChat")) {
            getServer().getPluginManager().registerEvents(new ZelChatListener(messageStackRepository), this);
            logger.info("ZelChat detected. Hooked into ZelChat events.");
        } else {
            getServer().getPluginManager().registerEvents(new SendMessageListener(messageStackRepository), this);
            logger.info("ZelChat not found. Using default chat event listener.");
        }

        // Starting the plugin
        try { start(); }
        catch (RuntimeException e) { getLogger().log(Level.SEVERE, e.getMessage()); }

        // Registering commands
        Objects.requireNonNull(getServer().getPluginCommand("messagesonhead")).setExecutor(new MohCommand(this, config, messageStackRepository, toggleManager));
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
}
