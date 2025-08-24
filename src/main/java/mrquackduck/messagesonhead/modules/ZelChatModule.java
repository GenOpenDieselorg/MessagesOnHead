package mrquackduck.messagesonhead.modules;

import it.pino.zelchat.api.message.ChatMessage;
import it.pino.zelchat.api.message.state.MessageState;
import it.pino.zelchat.api.module.ChatModule;
import it.pino.zelchat.api.module.annotation.ChatModuleSettings;
import it.pino.zelchat.api.module.priority.ModulePriority;
import mrquackduck.messagesonhead.MessagesOnHeadPlugin;
import mrquackduck.messagesonhead.configuration.Permissions;
import mrquackduck.messagesonhead.services.MessageStackRepository;
import mrquackduck.messagesonhead.utils.ColorUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@ChatModuleSettings(pluginOwner = "MessagesOnHead", priority = ModulePriority.NORMAL)
public class ZelChatModule implements ChatModule {
    private final MessagesOnHeadPlugin plugin;
    private final MessageStackRepository messageStackRepository;

    public ZelChatModule(MessagesOnHeadPlugin plugin, MessageStackRepository messageStackRepository) {
        this.plugin = plugin;
        this.messageStackRepository = messageStackRepository;
    }

    @Override
    public void handleChatMessage(final @NotNull ChatMessage chatMessage) {
        // Pobranie stanu wiadomości
        MessageState state = chatMessage.getState();

        // Sprawdzenie, czy wiadomość jest zablokowana (anulowana)
        if (state == MessageState.CANCELLED || state == MessageState.FILTERED_CANCELLED) {
            // Wiadomość zablokowana, nic nie rób.
            return;
        }

        // Kontynuuj, jeśli wiadomość nie jest zablokowana
        Player player = chatMessage.getBukkitPlayer();

        if (!player.hasPermission(Permissions.SHOW)) return;
        if (player.getGameMode() == GameMode.SPECTATOR) return;

        var messageStack = messageStackRepository.getMessageStack(player);
        var plainMessage = chatMessage.getRawMessage();
        var sanitizedMessage = ColorUtils.removeColorCodes(plainMessage);
        messageStack.pushMessage(sanitizedMessage);
    }
}
