package mrquackduck.messagesonhead.classes;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import mrquackduck.messagesonhead.MessagesOnHeadPlugin;
import mrquackduck.messagesonhead.utils.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.TextDisplay;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Globalny manager timerów.
 * <p>
 * Każdy TextDisplay aktualizowany jest osobnym, cyklicznym zadaniem przypiętym do
 * regionu encji. Dzięki temu kod działa zarówno na Paperze, jak i na Folii – na Folii
 * encji nie wolno modyfikować z innego regionu niż ten, który ją posiada, więc jeden
 * wspólny task globalny nie byłby poprawny.
 */
public class TimerManager {
    private static TimerManager instance;
    private final MessagesOnHeadPlugin plugin;
    private final Map<TextDisplay, TimerData> activeTimers = new ConcurrentHashMap<>();

    // Interwał aktualizacji w tickach (10 ticków = 0.5s - lepszy balans wydajność/płynność)
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final double UPDATE_INTERVAL_SECONDS = UPDATE_INTERVAL_TICKS / 20.0;

    private TimerManager(MessagesOnHeadPlugin plugin) {
        this.plugin = plugin;
    }

    public static synchronized TimerManager getInstance(MessagesOnHeadPlugin plugin) {
        if (instance == null) {
            instance = new TimerManager(plugin);
        }
        return instance;
    }

    public static synchronized void shutdown() {
        if (instance != null) {
            instance.stop();
            instance = null;
        }
    }

    public static synchronized void unregisterIfRunning(TextDisplay display) {
        if (instance != null) {
            instance.unregisterTextDisplay(display);
        }
    }

    public void registerTextDisplay(TextDisplay display, TextComponent baseText, double secondsToExist, String timerFormat, String timerColor) {
        TimerData data = new TimerData(baseText, secondsToExist, timerFormat, timerColor);
        activeTimers.put(display, data);

        // Zadanie przypięte do regionu encji - aktualizuje wyłącznie ten jeden TextDisplay.
        ScheduledTask task = Scheduler.runForEntityAtFixedRate(plugin, display, scheduledTask -> {
            TimerData current = activeTimers.get(display);

            // Usuń jeśli wpis zniknął, encja jest martwa lub czas minął
            if (current == null || display.isDead() || current.timeLeft < -0.1) {
                activeTimers.remove(display);
                scheduledTask.cancel();
                return;
            }

            // Aktualizuj tekst
            String timerText = String.format(current.timerFormat, Math.max(0.0, current.timeLeft));
            display.text(current.baseText.append(Component.text(timerText).color(current.timerColorParsed)));

            // Zmniejsz czas
            current.timeLeft -= UPDATE_INTERVAL_SECONDS;
        }, 1, UPDATE_INTERVAL_TICKS);

        // runAtFixedRate zwraca null, jeśli encja została już usunięta
        if (task == null) {
            activeTimers.remove(display);
        } else {
            data.task = task;
        }
    }

    public void unregisterTextDisplay(TextDisplay display) {
        TimerData data = activeTimers.remove(display);
        if (data != null && data.task != null) {
            data.task.cancel();
        }
    }

    private void stop() {
        for (TimerData data : activeTimers.values()) {
            if (data.task != null) data.task.cancel();
        }
        activeTimers.clear();
    }

    public int getActiveTimersCount() {
        return activeTimers.size();
    }

    private static class TimerData {
        final TextComponent baseText;
        final String timerFormat;
        final TextColor timerColorParsed;
        double timeLeft;
        ScheduledTask task;

        TimerData(TextComponent baseText, double timeLeft, String timerFormat, String timerColor) {
            this.baseText = baseText;
            this.timeLeft = timeLeft;
            this.timerFormat = timerFormat;
            // Parsuj kolor raz przy tworzeniu zamiast przy każdej aktualizacji
            this.timerColorParsed = TextColor.fromHexString(timerColor);
        }
    }
}
