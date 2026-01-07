package mrquackduck.messagesonhead.services;

import mrquackduck.messagesonhead.MessagesOnHeadPlugin;
import mrquackduck.messagesonhead.classes.MessageStack;
import mrquackduck.messagesonhead.utils.EntityUtils;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

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
        
        // Następnie usuń wszystkie pozostałe entity z tagiem
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                try {
                    if (EntityUtils.hasScoreboardTagCaseInvariant(entity, customEntityTag)) {
                        entity.remove();
                    }
                } catch (Exception ignored) {}
            }
        }
    }
    
    /**
     * Zwraca liczbę aktywnych stacków (dla debugowania)
     */
    public int getActiveStacksCount() {
        return playersStacks.size();
    }
}
