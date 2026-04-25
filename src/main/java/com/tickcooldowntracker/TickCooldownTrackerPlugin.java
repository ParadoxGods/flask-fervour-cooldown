package com.tickcooldowntracker;

import com.google.inject.Provides;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Flask Fervour Cooldown",
	description = "Track the Leagues Flask of Fervour cooldown by exact game ticks",
	tags = {"leagues", "flask", "fervour", "cooldown", "tick"}
)
public class TickCooldownTrackerPlugin extends Plugin
{
	private static final Pattern COOLDOWN_SECONDS_PATTERN = Pattern.compile("(?:wait|cooldown).*?(\\d+)\\s+seconds?");
	private static final int PENDING_FLASK_CLICK_TICKS = 3;

	private static final Set<Integer> FLASK_ITEM_IDS = Set.of(
		ItemID.LEAGUE_FLASK_OF_FERVOUR,
		ItemID.LEAGUE_FLASK_OF_FERVOUR_EMPTY,
		33240,
		33242
	);

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

	private final FlaskCooldownState cooldownState = new FlaskCooldownState();
	private int pendingFlaskClickTick = -1;

	@Provides
	TickCooldownTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TickCooldownTrackerConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		overlayManager.add(itemOverlay);
		syncCooldownFromClient();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(itemOverlay);
		cooldownState.reset();
		pendingFlaskClickTick = -1;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gameState = event.getGameState();
		if (gameState == GameState.LOGIN_SCREEN || gameState == GameState.HOPPING)
		{
			cooldownState.reset();
		}
		else if (gameState == GameState.LOGGED_IN)
		{
			syncCooldownFromClient();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		syncCooldownFromClient();
		if (pendingFlaskClickTick >= 0 && client.getTickCount() - pendingFlaskClickTick > PENDING_FLASK_CLICK_TICKS)
		{
			pendingFlaskClickTick = -1;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarpId() == VarPlayerID.LEAGUE_RELIC_FLASK_OF_FERVOUR_COOLDOWN)
		{
			syncCooldown(event.getValue());
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (!cooldownState.isActive() || event.getActor() == client.getLocalPlayer())
		{
			return;
		}

		Hitsplat hitsplat = event.getHitsplat();
		if (!hitsplat.isMine())
		{
			return;
		}

		int damage = hitsplat.getAmount();
		cooldownState.reduceFromDamage(damage, client.getTickCount());
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!event.isItemOp() || !isFlaskItem(event.getItemId()))
		{
			return;
		}

		pendingFlaskClickTick = client.getTickCount();
		if (!cooldownState.isActive())
		{
			cooldownState.startCooldown(client.getTickCount());
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (pendingFlaskClickTick < 0 || event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = Text.removeTags(event.getMessage()).toLowerCase(Locale.ROOT);
		Matcher matcher = COOLDOWN_SECONDS_PATTERN.matcher(message);
		if (!matcher.find())
		{
			return;
		}

		int seconds = Integer.parseInt(matcher.group(1));
		cooldownState.setCooldownTicks(secondsToTicks(seconds), client.getTickCount());
		pendingFlaskClickTick = -1;
	}

	boolean isFlaskItem(int itemId)
	{
		if (FLASK_ITEM_IDS.contains(itemId))
		{
			return true;
		}

		try
		{
			return FLASK_ITEM_IDS.contains(itemManager.canonicalize(itemId));
		}
		catch (RuntimeException ex)
		{
			return false;
		}
	}

	boolean isCooldownActive()
	{
		return cooldownState.isActive();
	}

	boolean shouldShowReadyPanel()
	{
		return cooldownState.isReady() || cooldownState.isRecentlyReady(client.getTickCount(), config.readyVisibleTicks());
	}

	boolean shouldShowReadyItem()
	{
		return cooldownState.isReady();
	}

	int getCooldownTicks()
	{
		return cooldownState.getCooldownTicks();
	}

	double getCooldownRatio()
	{
		return cooldownState.getCooldownRatio();
	}

	private void syncCooldownFromClient()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		syncCooldown(client.getVarpValue(VarPlayerID.LEAGUE_RELIC_FLASK_OF_FERVOUR_COOLDOWN));
	}

	private void syncCooldown(int rawCooldownValue)
	{
		cooldownState.sync(rawCooldownValue, client.getTickCount(), config.cooldownValueMode());
	}

	private static int secondsToTicks(int seconds)
	{
		return (seconds * 5 + 2) / 3;
	}
}
