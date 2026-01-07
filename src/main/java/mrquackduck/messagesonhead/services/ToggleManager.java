package mrquackduck.messagesonhead.services;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ToggleManager {
    private final Set<UUID> toggledOffOnline = ConcurrentHashMap.newKeySet();
    private final Set<UUID> toggledOffEveryone = ConcurrentHashMap.newKeySet();
    private final File file;
    private final JavaPlugin plugin;
    
    // Flaga zapobiegająca wielokrotnym równoczesnym zapisom
    private final AtomicBoolean saveScheduled = new AtomicBoolean(false);
    
    // Cache dla szybszego sprawdzania
    private volatile Set<UUID> toggledOffOnlineCache = Collections.emptySet();

    public ToggleManager(File dataFolder, JavaPlugin plugin) {
        this.file = new File(dataFolder, "toggled-off-players.txt");
        this.plugin = plugin;
        load();
    }
    
    // Konstruktor dla kompatybilności wstecznej
    public ToggleManager(File dataFolder) {
        this.file = new File(dataFolder, "toggled-off-players.txt");
        this.plugin = null;
        load();
    }

    public boolean toggle(Player player) {
        UUID uuid = player.getUniqueId();

        boolean hasPlayerToggledOff = isToggledOff(player);
        if (!hasPlayerToggledOff) {
            toggledOffEveryone.add(uuid);
            toggledOffOnline.add(uuid);
        } else {
            toggledOffEveryone.remove(uuid);
            toggledOffOnline.remove(uuid);
        }

        updateCache();
        saveAsync();
        return hasPlayerToggledOff;
    }

    public Set<UUID> getToggledOffOnline() {
        return toggledOffOnlineCache;
    }

    public void onPlayerJoin(Player player) {
        if (isToggledOffAll(player)) {
            toggledOffOnline.add(player.getUniqueId());
            updateCache();
        }
    }

    public void onPlayerQuit(Player player) {
        if (toggledOffOnline.remove(player.getUniqueId())) {
            updateCache();
        }
    }

    public void reload() {
        toggledOffOnline.clear();
        load();
        updateCache();
    }

    private void load() {
        toggledOffEveryone.clear();
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    toggledOffEveryone.add(UUID.fromString(line.trim()));
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (IOException ignored) {}
    }

    private void saveAsync() {
        // Jeśli już zaplanowano zapis, nie planuj kolejnego
        if (!saveScheduled.compareAndSet(false, true)) {
            return;
        }
        
        if (plugin != null) {
            // Asynchroniczny zapis
            new BukkitRunnable() {
                @Override
                public void run() {
                    saveSync();
                    saveScheduled.set(false);
                }
            }.runTaskAsynchronously(plugin);
        } else {
            // Fallback do synchronicznego zapisu
            saveSync();
            saveScheduled.set(false);
        }
    }
    
    private void saveSync() {
        try {
            // Utwórz folder jeśli nie istnieje
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
                for (UUID uuid : toggledOffEveryone) {
                    writer.write(uuid.toString());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            if (plugin != null) {
                plugin.getLogger().warning("Failed to save toggled-off players: " + e.getMessage());
            }
        }
    }
    
    private void updateCache() {
        toggledOffOnlineCache = Set.copyOf(toggledOffOnline);
    }

    private boolean isToggledOff(Player player) {
        return toggledOffOnline.contains(player.getUniqueId());
    }

    private boolean isToggledOffAll(Player player) {
        return toggledOffEveryone.contains(player.getUniqueId());
    }
}
