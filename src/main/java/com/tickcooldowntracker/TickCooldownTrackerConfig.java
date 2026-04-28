package com.tickcooldowntracker;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(TickCooldownTrackerConfig.GROUP)
public interface TickCooldownTrackerConfig extends Config
{
	String GROUP = "tickcooldowntracker";

	@ConfigSection(
		name = "Item highlights",
		description = "Inventory and equipment item overlays.",
		position = 0
	)
	String itemSection = "itemSection";

	@ConfigItem(
		keyName = "showItemTicks",
		name = "Show item tick text",
		description = "Draw the remaining tick count directly over tracked items.",
		position = 0,
		section = itemSection
	)
	default boolean showItemTicks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showCooldownFill",
		name = "Cooldown fill",
		description = "Shade items while their cooldown is active.",
		position = 1,
		section = itemSection
	)
	default boolean showCooldownFill()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showProgressBar",
		name = "Progress bar",
		description = "Draw a shrinking tick bar at the bottom of items on cooldown.",
		position = 2,
		section = itemSection
	)
	default boolean showProgressBar()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showReadyHighlight",
		name = "Ready highlight",
		description = "Highlight tracked items when their cooldown reaches zero.",
		position = 3,
		section = itemSection
	)
	default boolean showReadyHighlight()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "cooldownFillColor",
		name = "Cooldown fill color",
		description = "Fill color used while an item is cooling down.",
		position = 4,
		section = itemSection
	)
	default Color cooldownFillColor()
	{
		return new Color(180, 35, 35, 85);
	}

	@Alpha
	@ConfigItem(
		keyName = "readyColor",
		name = "Ready color",
		description = "Highlight color used when a cooldown reaches zero.",
		position = 5,
		section = itemSection
	)
	default Color readyColor()
	{
		return new Color(35, 210, 90, 150);
	}

	@ConfigItem(
		keyName = "activeTextColor",
		name = "Active text color",
		description = "Text color for active cooldown tick counts.",
		position = 6,
		section = itemSection
	)
	default Color activeTextColor()
	{
		return Color.WHITE;
	}
}
