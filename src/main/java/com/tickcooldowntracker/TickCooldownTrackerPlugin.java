package com.tickcooldowntracker;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Tick Cooldown Tracker",
	description = "Track configurable item cooldowns by exact game ticks",
	tags = {"tick", "cooldown", "item", "timer", "overlay"}
)
@Slf4j
public class TickCooldownTrackerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private TickCooldownTrackerConfig config;

	@Inject
	private TickCooldownOverlay overlay;

	@Inject
	private TickCooldownItemOverlay itemOverlay;

	private final Map<String, CooldownState> cooldowns = new HashMap<>();
	private List<CooldownDefinition> definitions = new ArrayList<>();
	private Set<String> trackedActions = new HashSet<>();

	@Provides
	TickCooldownTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TickCooldownTrackerConfig.class);
	}

	@Override
	protected void startUp()
	{
		reloadConfig();
		overlayManager.add(overlay);
		overlayManager.add(itemOverlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(itemOverlay);
		cooldowns.clear();
		definitions = new ArrayList<>();
		trackedActions = new HashSet<>();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!TickCooldownTrackerConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		reloadConfig();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gameState = event.getGameState();
		if (gameState == GameState.LOGIN_SCREEN || gameState == GameState.HOPPING)
		{
			cooldowns.clear();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		int readyVisibleTicks = getReadyVisibleTicks();
		int currentTick = getCurrentTick();
		cooldowns.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTick, readyVisibleTicks));
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!event.isItemOp() || !shouldTrackWidget(event.getWidget()) || !shouldTrackAction(event.getMenuOption()))
		{
			return;
		}

		int itemId = event.getItemId();
		CooldownDefinition definition = findDefinition(itemId);
		if (definition == null)
		{
			return;
		}

		String stateKey = stateKey(definition, itemId);
		CooldownState state = cooldowns.get(stateKey);
		if (state == null)
		{
			state = new CooldownState(definition, itemId, displayName(definition, itemId), getCurrentTick());
			cooldowns.put(stateKey, state);
		}
		else
		{
			state.restart(getCurrentTick());
		}

		log.debug("Started {} cooldown for {} ticks", state.getDisplayName(), definition.getTicks());
	}

	int getCurrentTick()
	{
		return client.getTickCount();
	}

	CooldownState getStateForItem(int itemId)
	{
		CooldownDefinition definition = findDefinition(itemId);
		if (definition == null)
		{
			return null;
		}

		CooldownState state = cooldowns.get(stateKey(definition, itemId));
		if (state == null || state.isExpired(getCurrentTick(), getReadyVisibleTicks()))
		{
			return null;
		}

		return state;
	}

	List<CooldownState> getVisibleStates()
	{
		int currentTick = getCurrentTick();
		int readyVisibleTicks = getReadyVisibleTicks();
		List<CooldownState> visibleStates = new ArrayList<>();
		for (CooldownState state : cooldowns.values())
		{
			if (!state.isExpired(currentTick, readyVisibleTicks))
			{
				visibleStates.add(state);
			}
		}

		visibleStates.sort(Comparator
			.comparing((CooldownState state) -> !state.isActive(currentTick))
			.thenComparingInt(state -> state.getRemainingTicks(currentTick))
			.thenComparing(CooldownState::getDisplayName));
		return visibleStates;
	}

	private void reloadConfig()
	{
		definitions = CooldownDefinition.parseAll(config.cooldownDefinitions());
		trackedActions = parseActions(config.trackedActions());
		cooldowns.keySet().removeIf(key -> definitions.stream().noneMatch(definition -> key.equals(stateKey(definition, -1))));
	}

	private Set<String> parseActions(String rawActions)
	{
		Set<String> actions = new HashSet<>();
		if (rawActions == null || rawActions.trim().isEmpty())
		{
			return actions;
		}

		for (String action : rawActions.split("[,;\\r\\n]+"))
		{
			String normalizedAction = normalizeAction(action);
			if (!normalizedAction.isEmpty())
			{
				actions.add(normalizedAction);
			}
		}
		return actions;
	}

	private boolean shouldTrackAction(String menuOption)
	{
		String action = normalizeAction(menuOption);
		return trackedActions.isEmpty() || trackedActions.contains(action);
	}

	private String normalizeAction(String action)
	{
		return Text.removeTags(action).trim().toLowerCase(Locale.ROOT);
	}

	private boolean shouldTrackWidget(Widget widget)
	{
		if (widget == null)
		{
			return config.trackInventory() && config.trackEquipment();
		}

		int widgetId = widget.getId();
		int interfaceId = WidgetUtil.componentToInterface(widgetId);

		if (config.trackInventory() && (widgetId == InterfaceID.Inventory.ITEMS || interfaceId == InterfaceID.INVENTORY))
		{
			return true;
		}

		return config.trackEquipment() && (widgetId == InterfaceID.EquipmentSide.ITEMS || interfaceId == InterfaceID.WORNITEMS);
	}

	private CooldownDefinition findDefinition(int itemId)
	{
		if (itemId <= 0)
		{
			return null;
		}

		int canonicalItemId = canonicalize(itemId);
		String normalizedName = normalizedItemName(canonicalItemId);
		for (CooldownDefinition definition : definitions)
		{
			int canonicalDefinitionId = definition.getItemId() == null ? -1 : canonicalize(definition.getItemId());
			if (definition.matches(itemId, canonicalItemId, normalizedName, canonicalDefinitionId))
			{
				return definition;
			}
		}

		return null;
	}

	private String stateKey(CooldownDefinition definition, int clickedItemId)
	{
		int canonicalDefinitionId = definition.getItemId() == null ? -1 : canonicalize(definition.getItemId());
		if (canonicalDefinitionId <= 0 && clickedItemId > 0)
		{
			canonicalDefinitionId = canonicalize(clickedItemId);
		}

		return definition.stateKey(canonicalDefinitionId);
	}

	private String displayName(CooldownDefinition definition, int itemId)
	{
		String itemName = itemName(canonicalize(itemId));
		if (itemName != null && !itemName.isEmpty() && !"null".equalsIgnoreCase(itemName))
		{
			return itemName;
		}

		return definition.getLabel();
	}

	private String normalizedItemName(int itemId)
	{
		String itemName = itemName(itemId);
		return itemName == null ? "" : CooldownDefinition.normalize(itemName);
	}

	private String itemName(int itemId)
	{
		try
		{
			ItemComposition itemComposition = itemManager.getItemComposition(itemId);
			return itemComposition == null ? null : itemComposition.getName();
		}
		catch (RuntimeException ex)
		{
			return null;
		}
	}

	private int canonicalize(int itemId)
	{
		try
		{
			return itemManager.canonicalize(itemId);
		}
		catch (RuntimeException ex)
		{
			return itemId;
		}
	}

	private int getReadyVisibleTicks()
	{
		if (!config.showReadyHighlight() && !config.showReadyInPanel())
		{
			return 0;
		}

		return config.readyVisibleTicks();
	}
}
