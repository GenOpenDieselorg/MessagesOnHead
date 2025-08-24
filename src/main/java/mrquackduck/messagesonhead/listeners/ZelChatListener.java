package mrquackduck.messagesonhead.listeners;

import it.pino.zelchat.api.events.ZelChatEvent;
import it.pino.zelchat.api.message.state.MessageState;
import mrquackduck.messagesonhead.services.MessageStackRepository;
import mrquackduck.messagesonhead.configuration.Permissions;
import mrquackduck.messagesonhead.utils.ColorUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ZelChatListener implements Listener {
    private final MessageStackRepository messageStackRepository;

    public ZelChatListener(MessageStackRepository messageStackRepository) {
        this.messageStackRepository = messageStackRepository;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMessageSent(ZelChatEvent event) {
        // Sprawdzamy stan wiadomości z ZelChat
        MessageState state = event.getChatMessage().getState();
        if (state == MessageState.CANCELLED || state == MessageState.FILTERED_CANCELLED) {
            return; // Nie pokazuj wiadomości, jeśli została anulowana
        }

        Player player = event.getChatMessage().getBukkitPlayer();

        if (!player.hasPermission(Permissions.SHOW)) return;
        if (player.getGameMode() == GameMode.SPECTATOR) return;

        var messageStack = messageStackRepository.getMessageStack(player);

        var plainMessage = event.getChatMessage().getRawMessage();
        var sanitizedMessage = ColorUtils.removeColorCodes(plainMessage);
        messageStack.pushMessage(sanitizedMessage);
    }
}
