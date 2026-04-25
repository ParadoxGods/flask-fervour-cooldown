package com.tickcooldowntracker;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(TickCooldownTrackerConfig.GROUP)
public interface TickCooldownTrackerConfig extends Config
{
	String GROUP = "tickcooldowntracker";

	enum CooldownValueMode
	{
		AUTO,
		TICKS,
		SECONDS,
		END_TICK
	}

	@ConfigSection(
		name = "Flask cooldown",
		description = "Flask of Fervour cooldown tracking.",
		position = 0
	)
	String cooldownSection = "cooldownSection";

	@ConfigSection(
		name = "Overlay",
		description = "Movable on-screen cooldown panel.",
		position = 1
	)
	String overlaySection = "overlaySection";

	@ConfigSection(
		name = "Item highlights",
		description = "Inventory and equipment item overlays.",
		position = 2
	)
	String itemSection = "itemSection";

	@ConfigItem(
		keyName = "cooldownValueMode",
		name = "Cooldown value mode",
		description = "How the client cooldown value should be interpreted. Auto should work from a fresh flask use; use Debug raw value if this looks wrong.",
		position = 0,
		section = cooldownSection
	)
	default CooldownValueMode cooldownValueMode()
	{
		return CooldownValueMode.AUTO;
	}

	@Range(max = 100)
	@ConfigItem(
		keyName = "readyVisibleTicks",
		name = "Panel ready ticks",
		description = "How many game ticks to keep showing READY in the panel after the cooldown reaches zero.",
		position = 1,
		section = cooldownSection
	)
	default int readyVisibleTicks()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "showDebugRawValue",
		name = "Debug raw value",
		description = "Show the raw Flask cooldown varp and detected conversion mode in the panel.",
		position = 2,
		section = cooldownSection
	)
	default boolean showDebugRawValue()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPanel",
		name = "Show panel",
		description = "Show the movable on-screen cooldown panel.",
		position = 0,
		section = overlaySection
	)
	default boolean showPanel()
	{
		return true;
	}

	@Range(min = 90, max = 300)
	@ConfigItem(
		keyName = "panelWidth",
		name = "Panel width",
		description = "Preferred width of the cooldown panel.",
		position = 1,
		section = overlaySection
	)
	default int panelWidth()
	{
		return 170;
	}

	@ConfigItem(
		keyName = "showReadyInPanel",
		name = "Show ready in panel",
		description = "Keep finished cooldowns in the panel during the ready display window.",
		position = 2,
		section = overlaySection
	)
	default boolean showReadyInPanel()
	{
		return true;
	}

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
