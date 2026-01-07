package mrquackduck.messagesonhead.services;

import mrquackduck.messagesonhead.MessagesOnHeadPlugin;
import mrquackduck.messagesonhead.classes.MessageStack;
import mrquackduck.messagesonhead.utils.EntityUtils;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static mrquackduck.messagesonhead.classes.MessageStack.customEntityTag;

public class MessageStackRepository {
    private final MessagesOnHeadPlugin plugin;
    private final ToggleManager toggleManager;
    // Zmiana na ConcurrentHashMap dla thread-safety
    private final Map<UUID, MessageStack> playersStacks = new ConcurrentHashMap<>();

    public MessageStackRepository(MessagesOnHeadPlugin plugin, ToggleManager toggleManager) {
        this.plugin = plugin;
        this.toggleManager = toggleManager;
    }

    public MessageStack getMessageStack(Player player) {
        return playersStacks.computeIfAbsent(player.getUniqueId(), 
            uuid -> new MessageStack(player, plugin, toggleManager));
    }

    public void resetPlayerMessageStack(Player player) {
        UUID uuid = player.getUniqueId();
        MessageStack playerMessageStack = playersStacks.remove(uuid);
        
        if (playerMessageStack != null) {
            try {
                playerMessageStack.deleteAllRelatedEntities();
            } catch (Exception e) {
                plugin.getLogger().warning("Error while cleaning up message stack for player " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    public void cleanUp() {
        // Najpierw wyczyść wszystkie stacki
        for (Map.Entry<UUID, MessageStack> entry : playersStacks.entrySet()) {
            try {
                entry.getValue().deleteAllRelatedEntities();
            } catch (Exception ignored) {}
        }
        playersStacks.clear();
        
        // Batch removal - zbierz wszystkie entity do usunięcia, potem usuń
        List<Entity> entitiesToRemove = new ArrayList<>();
        
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                try {
                    if (EntityUtils.hasScoreboardTagCaseInvariant(entity, customEntityTag)) {
                        entitiesToRemove.add(entity);
                    }
                } catch (Exception ignored) {}
            }
        }
        
        // Usuń zebrane entity w jednym przebiegu
        for (Entity entity : entitiesToRemove) {
            try {
                entity.remove();
            } catch (Exception ignored) {}
        }
    }
    
    /**
     * Asynchroniczny cleanup dla dużych serwerów - nie blokuje głównego wątku przy szukaniu
     */
    public void cleanUpAsync() {
        // Najpierw synchronicznie wyczyść stacki (szybkie)
        for (Map.Entry<UUID, MessageStack> entry : playersStacks.entrySet()) {
            try {
                entry.getValue().deleteAllRelatedEntities();
            } catch (Exception ignored) {}
        }
        playersStacks.clear();
        
        // Potem asynchronicznie znajdź pozostałe entity
        new BukkitRunnable() {
            @Override
            public void run() {
                List<Entity> entitiesToRemove = new ArrayList<>();
                
                for (World world : plugin.getServer().getWorlds()) {
                    for (Entity entity : world.getEntities()) {
                        try {
                            if (EntityUtils.hasScoreboardTagCaseInvariant(entity, customEntityTag)) {
                                entitiesToRemove.add(entity);
                            }
                        } catch (Exception ignored) {}
                    }
                }
                
                // Usuń na głównym wątku
                if (!entitiesToRemove.isEmpty()) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (Entity entity : entitiesToRemove) {
                                try {
                                    entity.remove();
                                } catch (Exception ignored) {}
                            }
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
    }
    
    /**
     * Zwraca liczbę aktywnych stacków (dla debugowania)
     */
    public int getActiveStacksCount() {
        return playersStacks.size();
    }
}
