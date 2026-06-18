package mrquackduck.messagesonhead.utils;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * Pomocnik do planowania zadań kompatybilny zarówno z Paperem, jak i z Folią.
 * <p>
 * Folia zastępuje pojedynczy wątek główny wątkami regionów, przez co klasyczny
 * {@link org.bukkit.scheduler.BukkitScheduler} oraz {@code BukkitRunnable} rzucają
 * wyjątkami. Metody poniżej delegują do schedulerów regionów/encji/async, które
 * istnieją w API Papera i zachowują się poprawnie na obu platformach:
 * <ul>
 *     <li>na Paperze zadania trafiają na wątek główny,</li>
 *     <li>na Folii – na wątek regionu właściwego dla danej encji/lokalizacji.</li>
 * </ul>
 */
public final class Scheduler {
    private static final boolean FOLIA = detectFolia();

    private Scheduler() {}

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** Czy serwer działa na Folii. */
    public static boolean isFolia() {
        return FOLIA;
    }

    /** Uruchamia zadanie w następnym ticku regionu właściwego dla encji. */
    public static void runForEntity(Plugin plugin, Entity entity, Runnable task) {
        entity.getScheduler().run(plugin, scheduled -> task.run(), null);
    }

    /** Uruchamia zadanie po {@code delayTicks} tickach na regionie właściwym dla encji. */
    public static void runForEntityLater(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
        entity.getScheduler().runDelayed(plugin, scheduled -> task.run(), null, Math.max(1, delayTicks));
    }

    /**
     * Cyklicznie powtarzane zadanie przypięte do regionu encji. Folia automatycznie
     * anuluje je po usunięciu encji. Zwraca {@code null}, jeśli encja została już usunięta.
     */
    public static ScheduledTask runForEntityAtFixedRate(Plugin plugin, Entity entity, Consumer<ScheduledTask> task,
                                                         long initialDelayTicks, long periodTicks) {
        return entity.getScheduler().runAtFixedRate(plugin, task, null,
                Math.max(1, initialDelayTicks), Math.max(1, periodTicks));
    }

    /** Uruchamia zadanie poza wątkami głównymi/regionów (asynchronicznie). */
    public static void runAsync(Plugin plugin, Runnable task) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, scheduled -> task.run());
    }
}
