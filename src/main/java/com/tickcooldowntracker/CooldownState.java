package com.tickcooldowntracker;

final class CooldownState
{
	private final CooldownDefinition definition;
	private final int itemId;
	private final String displayName;
	private int startTick;
	private int readyTick;

	CooldownState(CooldownDefinition definition, int itemId, String displayName, int currentTick)
	{
		this.definition = definition;
		this.itemId = itemId;
		this.displayName = displayName;
		restart(currentTick);
	}

	void restart(int currentTick)
	{
		startTick = currentTick;
		readyTick = currentTick + definition.getTicks();
	}

	int getRemainingTicks(int currentTick)
	{
		return Math.max(0, readyTick - currentTick);
	}

	boolean isActive(int currentTick)
	{
		return getRemainingTicks(currentTick) > 0;
	}

	boolean isExpired(int currentTick, int readyVisibleTicks)
	{
		return !isActive(currentTick) && currentTick - readyTick >= readyVisibleTicks;
	}

	double getRemainingRatio(int currentTick)
	{
		if (definition.getTicks() <= 0)
		{
			return 0;
		}

		return Math.min(1, Math.max(0, (double) getRemainingTicks(currentTick) / definition.getTicks()));
	}

	CooldownDefinition getDefinition()
	{
		return definition;
	}

	int getItemId()
	{
		return itemId;
	}

	String getDisplayName()
	{
		return displayName;
	}

	int getStartTick()
	{
		return startTick;
	}

	int getReadyTick()
	{
		return readyTick;
	}
}
