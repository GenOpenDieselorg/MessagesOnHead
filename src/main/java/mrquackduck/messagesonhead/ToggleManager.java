package mrquackduck.messagesonhead;

import org.bukkit.entity.Player;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ToggleManager {
    private final Set<UUID> toggledOffOnline = ConcurrentHashMap.newKeySet();
    private final Set<UUID> toggledOffEveryone = ConcurrentHashMap.newKeySet();
    private final File file;

    public ToggleManager(File dataFolder) {
        this.file = new File(dataFolder, "toggled-off-players.txt");
        load();
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

    private void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            for (UUID uuid : toggledOffEveryone) {
                writer.write(uuid.toString());
                writer.newLine();
            }
        } catch (IOException ignored) {}
    }

    public void onPlayerJoin(Player player) {
        if (isToggledOffAll(player)) {
            toggledOffOnline.add(player.getUniqueId());
        }
    }

    public void onPlayerQuit(Player player) {
        toggledOffOnline.remove(player.getUniqueId());
    }

    public boolean isToggledOff(Player player) {
        return toggledOffOnline.contains(player.getUniqueId());
    }

    public void setToggled(Player player, boolean off) {
        UUID uuid = player.getUniqueId();
        if (off) {
            toggledOffEveryone.add(uuid);
            toggledOffOnline.add(uuid);
        } else {
            toggledOffEveryone.remove(uuid);
            toggledOffOnline.remove(uuid);
        }
        save();
    }

    public boolean isToggledOffAll(Player player) {
        return toggledOffEveryone.contains(player.getUniqueId());
    }

    public Set<UUID> getToggledOffOnline() {
        return toggledOffOnline;
    }

    public void reload() {
        toggledOffOnline.clear();
        load();
    }
}
