package mrquackduck.messagesonhead.listeners;

import mrquackduck.messagesonhead.classes.MessageStack;
import mrquackduck.messagesonhead.classes.TimerManager;
import mrquackduck.messagesonhead.services.MessageStackRepository;
import mrquackduck.messagesonhead.utils.EntityUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveFromWorldEvent;
import org.bukkit.event.world.EntitiesLoadEvent;

/**
 * Region-bezpieczne sprzątanie encji pluginu.
 * <p>
 * Na Folii nie wolno skanować {@code world.getEntities()} z dowolnego wątku, więc zbiorczy
 * skan przy starcie nie jest możliwy. Zamiast tego reagujemy na zdarzenia, które Folia
 * dostarcza na wątku właściwego regionu encji:
 * <ul>
 *     <li>{@link EntitiesLoadEvent} – gdy ładowane są encje chunka, usuwamy osierocone
 *         encje pluginu (tag {@code moh-entity}), których nie zna repozytorium. To czyści
 *         "duchy" pozostałe po crashu lub niegracefulnym wyłączeniu serwera.</li>
 *     <li>{@link EntityRemoveFromWorldEvent} – gdy encja znika z innego powodu niż nasza
 *         własna logika (np. /kill, inny plugin), odpinamy jej timer i przestajemy ją
 *         śledzić, by nie zostawiać martwych wpisów w pamięci.</li>
 * </ul>
 */
public class EntityCleanupListener implements Listener {
    private final MessageStackRepository repository;

    public EntityCleanupListener(MessageStackRepository repository) {
        this.repository = repository;
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (!EntityUtils.hasScoreboardTagCaseInvariant(entity, MessageStack.customEntityTag)) continue;
            // Encje, które plugin aktualnie obsługuje, są w repozytorium - tych nie ruszamy.
            // Pozostałe to sieroty z poprzedniej sesji -> usuwamy (jesteśmy na wątku regionu encji).
            if (repository.isTracked(entity.getUniqueId())) continue;
            try {
                entity.remove();
            } catch (Exception ignored) {}
        }
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        Entity entity = event.getEntity();
        if (!EntityUtils.hasScoreboardTagCaseInvariant(entity, MessageStack.customEntityTag)) return;

        if (entity instanceof TextDisplay textDisplay) {
            TimerManager.unregisterIfRunning(textDisplay);
        }
        repository.untrack(entity);
    }
}
