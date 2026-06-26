package mrquackduck.messagesonhead.listeners;

import mrquackduck.messagesonhead.services.ToggleManager;
import mrquackduck.messagesonhead.services.MessageStackRepository;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerConnectionListener implements Listener {
    private final MessageStackRepository messageStackRepository;
    private final ToggleManager toggleManager;

    public PlayerConnectionListener(MessageStackRepository messageStackRepository, ToggleManager toggleManager) {
        this.messageStackRepository = messageStackRepository;
        this.toggleManager = toggleManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        toggleManager.onPlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        messageStackRepository.resetPlayerMessageStack(event.getPlayer());
        toggleManager.onPlayerQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        messageStackRepository.resetPlayerMessageStack(event.getPlayer());
    }
}
