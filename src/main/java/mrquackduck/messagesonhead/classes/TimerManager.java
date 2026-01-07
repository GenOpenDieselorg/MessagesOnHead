package mrquackduck.messagesonhead.classes;

import mrquackduck.messagesonhead.MessagesOnHeadPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Globalny manager timerów - jeden task dla wszystkich wiadomości
 * Zamiast tworzyć osobny BukkitRunnable dla każdego TextDisplay
 */
public class TimerManager {
    private static TimerManager instance;
    private final MessagesOnHeadPlugin plugin;
    private final Map<TextDisplay, TimerData> activeTimers = new ConcurrentHashMap<>();
    private BukkitTask globalTask;

    private TimerManager(MessagesOnHeadPlugin plugin) {
        this.plugin = plugin;
        startGlobalTask();
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

    private void startGlobalTask() {
        if (globalTask != null && !globalTask.isCancelled()) return;

        globalTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeTimers.isEmpty()) return;

                Iterator<Map.Entry<TextDisplay, TimerData>> iterator = activeTimers.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<TextDisplay, TimerData> entry = iterator.next();
                    TextDisplay display = entry.getKey();
                    TimerData data = entry.getValue();

                    // Usuń jeśli entity jest martwe lub czas minął
                    if (display.isDead() || data.timeLeft < -0.1) {
                        iterator.remove();
                        continue;
                    }

                    // Aktualizuj tekst
                    String timerText = String.format(data.timerFormat, Math.max(0.0, data.timeLeft));
                    display.text(data.baseText.append(Component.text(timerText).color(TextColor.fromHexString(data.timerColor))));

                    // Zmniejsz czas (co 4 ticki = 0.2s zamiast co 2 ticki = 0.1s dla lepszej wydajności)
                    data.timeLeft -= 0.2;
                }
            }
        }.runTaskTimer(plugin, 1, 4); // Co 4 ticki zamiast 2 - mniejsze obciążenie
    }

    public void registerTextDisplay(TextDisplay display, TextComponent baseText, double secondsToExist, String timerFormat, String timerColor) {
        activeTimers.put(display, new TimerData(baseText, secondsToExist, timerFormat, timerColor));
    }

    public void unregisterTextDisplay(TextDisplay display) {
        activeTimers.remove(display);
    }

    private void stop() {
        if (globalTask != null && !globalTask.isCancelled()) {
            globalTask.cancel();
        }
        activeTimers.clear();
    }

    public int getActiveTimersCount() {
        return activeTimers.size();
    }

    private static class TimerData {
        final TextComponent baseText;
        final String timerFormat;
        final String timerColor;
        double timeLeft;

        TimerData(TextComponent baseText, double timeLeft, String timerFormat, String timerColor) {
            this.baseText = baseText;
            this.timeLeft = timeLeft;
            this.timerFormat = timerFormat;
            this.timerColor = timerColor;
        }
    }
}
