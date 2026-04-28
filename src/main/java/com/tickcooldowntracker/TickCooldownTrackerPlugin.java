package com.tickcooldowntracker;

import com.google.inject.Provides;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.Skill;
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
	private static final Pattern COOLDOWN_MINUTES_PATTERN = Pattern.compile("(\\d+)\\s+minutes?");
	private static final Pattern COOLDOWN_SECONDS_PATTERN = Pattern.compile("(\\d+)\\s+seconds?");
	private static final int PENDING_FLASK_CLICK_TICKS = 5;
	private static final int FLASK_EXPLOSION_TICKS = 4;
	private static final int MAX_REASONABLE_COOLDOWN_TICKS = FlaskCooldownState.DEFAULT_FULL_COOLDOWN_TICKS + 10;

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
	private TickCooldownItemOverlay itemOverlay;

	private final FlaskCooldownState cooldownState = new FlaskCooldownState();
	private final CooldownSavingsTracker savingsTracker = new CooldownSavingsTracker();
	private int pendingFlaskClickTick = -1;
	private int lastFlaskUseTick = -1;
	private boolean cooldownVarpSeen;

	@Provides
	TickCooldownTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TickCooldownTrackerConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(itemOverlay);
		syncCooldownFromVarps();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(itemOverlay);
		cooldownState.reset();
		savingsTracker.reset();
		pendingFlaskClickTick = -1;
		lastFlaskUseTick = -1;
		cooldownVarpSeen = false;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gameState = event.getGameState();
		if (gameState == GameState.LOGIN_SCREEN || gameState == GameState.HOPPING)
		{
			cooldownState.reset();
			savingsTracker.reset();
			cooldownVarpSeen = false;
		}
		else if (gameState == GameState.LOGGED_IN)
		{
			pendingFlaskClickTick = -1;
			syncCooldownFromVarps();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		boolean syncedFromVarps = syncCooldownFromVarps();
		boolean wasActive = cooldownState.isActive();
		cooldownState.advanceTo(client.getTickCount());
		if (!syncedFromVarps && wasActive && !cooldownState.isActive())
		{
			notifyCooldownReadyIfNeeded(savingsTracker.complete(client.getTickCount()));
		}
		else if (!syncedFromVarps && cooldownState.isActive())
		{
			savingsTracker.markSnapshot(cooldownState.getCooldownTicks(), client.getTickCount());
		}
		if (pendingFlaskClickTick >= 0 && client.getTickCount() - pendingFlaskClickTick > PENDING_FLASK_CLICK_TICKS)
		{
			pendingFlaskClickTick = -1;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int varpId = event.getVarpId();
		if (varpId == VarPlayerID.LEAGUE_RELIC_FLASK_OF_FERVOUR_COOLDOWN || varpId == VarPlayerID.MAP_CLOCK)
		{
			syncCooldownFromVarps();
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (cooldownVarpSeen || !cooldownState.isActive() || event.getActor() == client.getLocalPlayer())
		{
			return;
		}

		Actor actor = event.getActor();
		Hitsplat hitsplat = event.getHitsplat();
		int damage = hitsplat.getAmount();
		if (!isCooldownReducingDamage(actor, hitsplat, damage))
		{
			return;
		}

		if (isLikelyOwnFlaskExplosion(damage))
		{
			return;
		}

		int savedTicks = cooldownState.reduceFromDamage(damage, client.getTickCount());
		savingsTracker.addSavedTicks(savedTicks);
		if (cooldownState.isReady())
		{
			notifyCooldownReadyIfNeeded(savingsTracker.complete(client.getTickCount()));
		}
		else if (savedTicks > 0)
		{
			savingsTracker.markSnapshot(cooldownState.getCooldownTicks(), client.getTickCount());
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!isFlaskActivation(event))
		{
			return;
		}

		syncCooldownFromVarps();
		boolean activeBeforeClick = cooldownState.isActive();
		pendingFlaskClickTick = client.getTickCount();
		if (!activeBeforeClick)
		{
			lastFlaskUseTick = client.getTickCount();
			savingsTracker.startCycle();
			cooldownState.startCooldownOnNextTick(client.getTickCount());
			savingsTracker.markSnapshot(FlaskCooldownState.DEFAULT_FULL_COOLDOWN_TICKS, client.getTickCount() + 1);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (pendingFlaskClickTick < 0 || !isCooldownMessageType(event.getType()))
		{
			return;
		}

		String message = Text.removeTags(event.getMessage()).toLowerCase(Locale.ROOT);
		int seconds = parseCooldownSeconds(message);
		if (seconds < 0)
		{
			return;
		}

		if (!syncCooldownFromVarps())
		{
			int remainingTicks = secondsToTicks(seconds);
			cooldownState.setCooldownTicks(remainingTicks, client.getTickCount());
			if (remainingTicks > 0)
			{
				savingsTracker.markSnapshot(remainingTicks, client.getTickCount());
			}
			else
			{
				notifyCooldownReadyIfNeeded(savingsTracker.complete(client.getTickCount()));
			}
		}
		pendingFlaskClickTick = -1;
		lastFlaskUseTick = -1;
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

	private boolean syncCooldownFromVarps()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return false;
		}

		int cooldownEndTick = client.getVarpValue(VarPlayerID.LEAGUE_RELIC_FLASK_OF_FERVOUR_COOLDOWN);
		if (cooldownEndTick <= 0)
		{
			if (cooldownVarpSeen && !isWaitingForActivationVarp())
			{
				applyCooldownSnapshot(0, currentCooldownClock());
				pendingFlaskClickTick = -1;
			}
			return cooldownVarpSeen;
		}

		int mapClock = client.getVarpValue(VarPlayerID.MAP_CLOCK);
		if (mapClock <= 0)
		{
			return false;
		}

		int remainingTicks = decodeCooldownTicks(cooldownEndTick, mapClock);
		if (remainingTicks > MAX_REASONABLE_COOLDOWN_TICKS)
		{
			return false;
		}

		cooldownVarpSeen = true;
		if (remainingTicks <= 0 && isWaitingForActivationVarp())
		{
			return true;
		}

		applyCooldownSnapshot(remainingTicks, mapClock);
		pendingFlaskClickTick = -1;
		if (remainingTicks > 0)
		{
			lastFlaskUseTick = -1;
		}
		return true;
	}

	private void applyCooldownSnapshot(int remainingTicks, int clock)
	{
		notifyCooldownReadyIfNeeded(savingsTracker.observe(remainingTicks, clock));
		cooldownState.setCooldownTicks(remainingTicks, client.getTickCount());
	}

	private int currentCooldownClock()
	{
		int mapClock = client.getVarpValue(VarPlayerID.MAP_CLOCK);
		return mapClock > 0 ? mapClock : client.getTickCount();
	}

	static int decodeCooldownTicks(int cooldownEndTick, int mapClock)
	{
		return Math.max(0, cooldownEndTick - mapClock);
	}

	private boolean isWaitingForActivationVarp()
	{
		return lastFlaskUseTick >= 0 && client.getTickCount() - lastFlaskUseTick <= PENDING_FLASK_CLICK_TICKS;
	}

	private void notifyCooldownReadyIfNeeded(boolean shouldNotify)
	{
		if (!shouldNotify)
		{
			return;
		}

		int savedTicks = savingsTracker.getSavedTicks();
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Flask cooldown saved: "
			+ formatSavedSeconds(savedTicks) + "s (" + savedTicks + " " + tickLabel(savedTicks) + ").", null);
	}

	private boolean isFlaskActivation(MenuOptionClicked event)
	{
		if (!event.isItemOp() || !isFlaskItem(event.getItemId()))
		{
			return false;
		}

		String option = Text.removeTags(event.getMenuOption()).toLowerCase(Locale.ROOT);
		return event.getItemOp() == 1
			|| option.contains("drink")
			|| option.contains("activate")
			|| option.contains("use")
			|| option.contains("consume");
	}

	private boolean isCooldownReducingDamage(Actor actor, Hitsplat hitsplat, int damage)
	{
		if (damage <= 0)
		{
			return false;
		}

		if (hitsplat.isMine())
		{
			return true;
		}

		// Thorn/reflect damage may not be flagged as "mine", but it is applied to
		// the NPC currently attacking the local player and still reduces the flask.
		return hitsplat.isOthers() && actor.getInteracting() == client.getLocalPlayer();
	}

	private boolean isLikelyOwnFlaskExplosion(int damage)
	{
		if (lastFlaskUseTick < 0 || client.getTickCount() - lastFlaskUseTick > FLASK_EXPLOSION_TICKS)
		{
			return false;
		}

		int prayer = client.getRealSkillLevel(Skill.PRAYER);
		int pvmExplosionDamage = (int) Math.floor(prayer * 0.6);
		int pvpExplosionDamage = (int) Math.floor(prayer * 0.3);
		return damage == pvmExplosionDamage || damage == pvpExplosionDamage;
	}

	private static boolean isCooldownMessageType(ChatMessageType type)
	{
		return type == ChatMessageType.GAMEMESSAGE || type == ChatMessageType.ENGINE || type == ChatMessageType.SPAM;
	}

	private static int parseCooldownSeconds(String message)
	{
		if (!message.contains("cooldown") && !message.contains("wait") && !message.contains("again") && !message.contains("ready"))
		{
			return -1;
		}

		int seconds = 0;
		boolean matched = false;

		Matcher minutesMatcher = COOLDOWN_MINUTES_PATTERN.matcher(message);
		if (minutesMatcher.find())
		{
			seconds += Integer.parseInt(minutesMatcher.group(1)) * 60;
			matched = true;
		}

		Matcher secondsMatcher = COOLDOWN_SECONDS_PATTERN.matcher(message);
		if (secondsMatcher.find())
		{
			seconds += Integer.parseInt(secondsMatcher.group(1));
			matched = true;
		}

		return matched ? seconds : -1;
	}

	private static int secondsToTicks(int seconds)
	{
		return (seconds * 5 + 2) / 3;
	}

	static String formatSavedSeconds(int savedTicks)
	{
		int tenthsOfSeconds = Math.max(0, savedTicks) * 6;
		if (tenthsOfSeconds % 10 == 0)
		{
			return Integer.toString(tenthsOfSeconds / 10);
		}
		return String.format(Locale.ROOT, "%.1f", tenthsOfSeconds / 10.0);
	}

	private static String tickLabel(int ticks)
	{
		return ticks == 1 ? "tick" : "ticks";
	}
}
