package mrquackduck.messagesonhead.services;

import mrquackduck.messagesonhead.MessagesOnHeadPlugin;
import mrquackduck.messagesonhead.classes.MessageStack;
import mrquackduck.messagesonhead.utils.EntityUtils;
import mrquackduck.messagesonhead.utils.Scheduler;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static mrquackduck.messagesonhead.classes.MessageStack.customEntityTag;

public class MessageStackRepository {
    private final MessagesOnHeadPlugin plugin;
    private final ToggleManager toggleManager;
    // Zmiana na ConcurrentHashMap dla thread-safety
    private final Map<UUID, MessageStack> playersStacks = new ConcurrentHashMap<>();
    // UUID wszystkich encji aktualnie zarządzanych przez plugin (żywe wiadomości).
    // Pozwala odróżnić nasze encje od osieroconych "duchów" z poprzedniej sesji
    // przy czyszczeniu region-bezpiecznym na Folii (patrz EntityCleanupListener).
    private final Set<UUID> trackedEntityIds = ConcurrentHashMap.newKeySet();

    public MessageStackRepository(MessagesOnHeadPlugin plugin, ToggleManager toggleManager) {
        this.plugin = plugin;
        this.toggleManager = toggleManager;
    }

    public MessageStack getMessageStack(Player player) {
        return playersStacks.computeIfAbsent(player.getUniqueId(),
            uuid -> new MessageStack(player, plugin, toggleManager, this));
    }

    /** Oznacza encję jako zarządzaną przez plugin. */
    public void track(Entity entity) {
        trackedEntityIds.add(entity.getUniqueId());
    }

    /** Usuwa encję z listy zarządzanych. */
    public void untrack(Entity entity) {
        trackedEntityIds.remove(entity.getUniqueId());
    }

    /** Czy encja jest aktualnie żywą encją pluginu. */
    public boolean isTracked(UUID entityId) {
        return trackedEntityIds.contains(entityId);
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
        // Najpierw wyczyść wszystkie stacki.
        // requestCleanup() planuje usuwanie na regionie właściwego gracza, dzięki czemu
        // listy encji każdego stacku tykane są wyłącznie z wątku regionu tego gracza
        // (a nie z wątku komendy /reload) - to eliminuje wyścig na Folii.
        for (Map.Entry<UUID, MessageStack> entry : playersStacks.entrySet()) {
            try {
                entry.getValue().requestCleanup();
            } catch (Exception ignored) {}
        }
        playersStacks.clear();

        // Skan po wszystkich encjach świata nie jest bezpieczny wątkowo na Folii
        // (każda encja należy do swojego regionu). Encje pluginu i tak są pasażerami
        // graczy i zostają usunięte powyżej, więc na Folii pomijamy zbiorczy skan.
        if (Scheduler.isFolia()) return;

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
        // Wyczyść stacki region-bezpiecznie (patrz uwaga w cleanUp()).
        for (Map.Entry<UUID, MessageStack> entry : playersStacks.entrySet()) {
            try {
                entry.getValue().requestCleanup();
            } catch (Exception ignored) {}
        }
        playersStacks.clear();

        // Skan po encjach świata nie jest bezpieczny wątkowo na Folii - pomijamy go tam.
        if (Scheduler.isFolia()) return;

        // Potem asynchronicznie znajdź pozostałe entity
        Scheduler.runAsync(plugin, () -> {
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

            // Usuń każdą encję na regionie, do którego należy
            for (Entity entity : entitiesToRemove) {
                try {
                    Scheduler.runForEntity(plugin, entity, entity::remove);
                } catch (Exception ignored) {}
            }
        });
    }
    
    /**
     * Zwraca liczbę aktywnych stacków (dla debugowania)
     */
    public int getActiveStacksCount() {
        return playersStacks.size();
    }
}
