package mrquackduck.messagesonhead.classes;

import me.clip.placeholderapi.PlaceholderAPI;
import mrquackduck.messagesonhead.configuration.Configuration;
import mrquackduck.messagesonhead.configuration.Permissions;
import mrquackduck.messagesonhead.utils.ColorUtils;
import mrquackduck.messagesonhead.utils.EntityUtils;
import mrquackduck.messagesonhead.MessagesOnHeadPlugin;
import mrquackduck.messagesonhead.services.ToggleManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Represents a stack of displayed messages above player's head
 */
public class MessageStack {
    private final MessagesOnHeadPlugin plugin;
    private final Configuration config;
    private final ToggleManager toggleManager;
    private final Player player;
    private final List<Entity> entities = new ArrayList<>();
    private final List<DisplayedMessage> displayedMessages = new ArrayList<>();
    public static final String customEntityTag = "moh-entity";
    
    // Zmniejszony limit dla lepszej wydajności przy 200+ graczach
    private static final int MAX_MESSAGES_PER_PLAYER = 3;

    public MessageStack(Player player, MessagesOnHeadPlugin plugin, ToggleManager toggleManager) {
        this.player = player;
        this.plugin = plugin;
        this.config = new Configuration(plugin);
        this.toggleManager = toggleManager;
        scaffoldExistingStackEntities();
    }

    private void scaffoldExistingStackEntities() {
        Entity currentEntity = player;
        while (!currentEntity.getPassengers().isEmpty()) {
            var passengers = currentEntity.getPassengers();
            var needToBreak = true;
            for (Entity passenger : passengers) {
                if (EntityUtils.hasScoreboardTagCaseInvariant(passenger, customEntityTag)) {
                    currentEntity = passenger;
                    entities.add(passenger);
                    needToBreak = false;
                }
            }

            if (needToBreak) break;
        }
    }

    public void deleteAllRelatedEntities() {
        for (Entity entity : entities) {
            try {
                entity.remove();
            } catch (Exception ignored) {}
        }
        entities.clear();
        displayedMessages.clear();
    }

    public void pushMessage(String text) {
        if (text.isEmpty()) return;

        // Run the entire logic on the main server thread to avoid async errors
        new BukkitRunnable() {
            @Override
            public void run() {
                // Usuń najstarsze wiadomości jeśli przekroczono limit
                while (displayedMessages.size() >= MAX_MESSAGES_PER_PLAYER) {
                    if (!displayedMessages.isEmpty()) {
                        removeDisplayedMessage(displayedMessages.get(0));
                    }
                }
                
                var secondsToExist = calculateTimeForMessageToExist(text);
                var minSymbolsForTimer = config.minSymbolsForTimer();

                List<String> lines = mrquackduck.messagesonhead.utils.StringUtils.splitTextIntoLines(text, config.symbolsPerLine(), config.symbolsLimit());
                Collections.reverse(lines); // Reverse the stack from bottom to top

                Entity currentEntityToSitOn = getEntityToSitOn();
                List<Entity> newEntities = new ArrayList<>(lines.size() * 2);
                
                // Pobierz listę graczy z wyłączonym widokiem raz, nie dla każdego entity
                Set<Player> toggledOffPlayers = getToggledOffPlayers();

                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    boolean isLastLine = (i == 0); // Since lines are reversed, first index points on the last line
                    boolean needToShowTimer = isLastLine && text.length() >= minSymbolsForTimer;

                    var middleEntityHeight = config.gapBetweenMessages();
                    if (currentEntityToSitOn.getType() == EntityType.PLAYER) {
                        middleEntityHeight = config.gapAboveHead();
                    }

                    final var middleEntity = spawnMiddleEntity(middleEntityHeight, toggledOffPlayers);
                    final var textDisplay = spawnTextDisplay(player.getLocation(), line, secondsToExist, needToShowTimer, toggledOffPlayers);
                    middleEntity.addPassenger(textDisplay);
                    currentEntityToSitOn.addPassenger(middleEntity);

                    newEntities.add(middleEntity);
                    newEntities.add(textDisplay);

                    currentEntityToSitOn = textDisplay;
                }

                DisplayedMessage newDisplayedMessage = new DisplayedMessage(newEntities);
                displayedMessages.add(newDisplayedMessage);
                entities.addAll(newEntities);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        removeDisplayedMessage(newDisplayedMessage);
                    }
                }.runTaskLater(plugin, Math.round(secondsToExist * 20) + 2);
            }
        }.runTask(plugin);
    }

    private void removeDisplayedMessage(DisplayedMessage displayedMessage) {
        if (!displayedMessages.contains(displayedMessage)) return;
        var currentDisplayedMessageIndex = displayedMessages.indexOf(displayedMessage);

        // Remove all entities in the displayed message
        for (Entity entity : displayedMessage.entities) {
            try {
                entity.remove();
            } catch (Exception ignored) {}
            entities.remove(entity);
        }

        displayedMessages.remove(displayedMessage);

        // Return if there are no entities left
        if (entities.isEmpty()) return;

        // The current displayedMessage index turns into the next displayedMessage index because the current displayedMessage was deleted from the list
        var nextDisplayedMessageIndex = currentDisplayedMessageIndex;
        if (nextDisplayedMessageIndex >= displayedMessages.size()) return; // Return if no further displayed messages left
        var nextDisplayedMessage = displayedMessages.get(nextDisplayedMessageIndex);

        // Determining the message displayedMessage standing before in the list
        var prevDisplayedMessageIndex = nextDisplayedMessageIndex - 1;
        if (prevDisplayedMessageIndex >= 0) {
            // If there's a potential displayedMessage, transfer the message there
            var prevDisplayedMessage = displayedMessages.get(prevDisplayedMessageIndex);
            prevDisplayedMessage.entities.get(prevDisplayedMessage.entities.size() - 1).addPassenger(nextDisplayedMessage.entities.get(0));
            return;
        }

        var nextEntity = nextDisplayedMessage.entities.get(0);
        if (nextEntity.getType() == EntityType.INTERACTION) {
            ((Interaction)nextEntity).setInteractionHeight(config.gapAboveHead());
        }

        player.addPassenger(nextEntity);
    }

    private TextDisplay spawnTextDisplay(Location location, String text, double secondsToExist, boolean showTimer, Set<Player> toggledOffPlayers) {
        if (showTimer && !config.isTimerEnabled()) showTimer = false;
        location.setY(location.y() + 50); // Setting higher Y coordinate to prevent the message appearing from bottom

        final var textDisplay = (TextDisplay) Objects.requireNonNull(location.getWorld()).spawnEntity(location, EntityType.TEXT_DISPLAY);
        if (config.isBackgroundEnabled()) textDisplay.setBackgroundColor(Color.fromARGB(ColorUtils.hexToARGB(config.backgroundColor(),config.backgroundTransparencyPercentage())));
        if (!config.visibleToSender()) player.hideEntity(plugin, textDisplay);
        textDisplay.setDefaultBackground(!config.isBackgroundEnabled());
        textDisplay.setBillboard(config.pivotAxis());
        textDisplay.setRotation(location.getYaw(), 0);
        textDisplay.setShadowed(config.isShadowed());
        textDisplay.setLineWidth(Integer.MAX_VALUE);
        textDisplay.addScoreboardTag(customEntityTag);

        // Użyj przekazanej listy graczy zamiast pobierać ją ponownie
        hideFromToggledOffViewers(textDisplay, toggledOffPlayers);

        var textToBeDisplayed = Component.text(text).color(TextColor.fromHexString(config.textColor()));
        if (config.isPlaceholderApiIntegrationEnabled()) {
            text = config.lineFormat()
                    .replace("[defaultColor]", config.textColor())
                    .replace("[colorPlaceholder]", config.colorPlaceholder())
                    .replace("[message]", text);

            text = applyColorPlaceholders(text);
            textToBeDisplayed = LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        }

        if (showTimer) {
            // Rejestracja w globalnym timerze zamiast tworzenia osobnego taska
            TimerManager.getInstance(plugin).registerTextDisplay(textDisplay, textToBeDisplayed, secondsToExist, config.timerFormat(), config.timerColor());
        } else {
            textDisplay.text(textToBeDisplayed);
        }

        return textDisplay;
    }

    private String applyColorPlaceholders(String text) {
        String resolvedColorCode = PlaceholderAPI.setPlaceholders(player, config.colorPlaceholder());
        if (resolvedColorCode.isEmpty()) {
            // Remove color code if placeholder returns empty
            text = text.replace('&' + config.colorPlaceholder(), "");
        }
        else {
            // Replace placeholder with actual color code
            text = text.replace(config.colorPlaceholder(), resolvedColorCode);
        }

        text = text.replace("&&", "&");
        return text;
    }

    private Entity spawnMiddleEntity(float height, Set<Player> toggledOffPlayers) {
        var location = player.getLocation();
        location.setY(location.y() + 50); // Setting higher Y coordinate to prevent the message appearing from bottom

        var entity = Objects.requireNonNull(location.getWorld()).spawn(location, Interaction.class);
        if (!config.visibleToSender()) player.hideEntity(plugin, entity);
        entity.setInteractionWidth(0);
        entity.setInteractionHeight(height);
        entity.setInvulnerable(true);
        entity.setGravity(false);
        entity.addScoreboardTag(customEntityTag);

        // Użyj przekazanej listy graczy zamiast pobierać ją ponownie
        hideFromToggledOffViewers(entity, toggledOffPlayers);

        return entity;
    }
    
    /**
     * Pobiera listę graczy z wyłączonym widokiem - wywoływane raz na wiadomość
     */
    private Set<Player> getToggledOffPlayers() {
        Set<Player> result = new HashSet<>();
        if (toggleManager != null) {
            for (UUID uuid : toggleManager.getToggledOffOnline()) {
                Player viewer = Bukkit.getPlayer(uuid);
                if (viewer != null && viewer.hasPermission(Permissions.TOGGLE)) {
                    result.add(viewer);
                }
            }
        }
        return result;
    }

    private void hideFromToggledOffViewers(Entity entity, Set<Player> toggledOffPlayers) {
        for (Player viewer : toggledOffPlayers) {
            viewer.hideEntity(plugin, entity);
        }
    }

    private double calculateTimeForMessageToExist(String message) {
        double initialTime = config.timeToExist();
        if (config.isScalingEnabled()) {
            var scalingCoefficient = config.scalingCoefficient();
            initialTime += (scalingCoefficient * message.length());
        }

        return initialTime;
    }

    /**
     * Returns the highest entity that can have new messages stacked on top of it (either the player or the last message entity)
     */
    private Entity getEntityToSitOn() {
        if (entities.isEmpty()) return player;
        else return entities.get(entities.size() - 1);
    }
}
