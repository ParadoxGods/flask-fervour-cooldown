package com.tickcooldowntracker;

final class FlaskCooldownState
{
	static final int DEFAULT_FULL_COOLDOWN_TICKS = 300;
	private static final int MAX_REASONABLE_COOLDOWN_TICKS = 1200;

	private int cooldownTicks;
	private int highestObservedCooldownTicks = DEFAULT_FULL_COOLDOWN_TICKS;
	private int readySinceTick = -1;
	private int lastClientTick = -1;
	private int rawValue = -1;
	private TickCooldownTrackerConfig.CooldownValueMode resolvedMode;

	void sync(int varpValue, int currentTick, TickCooldownTrackerConfig.CooldownValueMode configuredMode)
	{
		boolean wasActive = cooldownTicks > 0 || rawValue > 0;
		advanceTo(currentTick);

		int updatedRawValue = Math.max(0, varpValue);
		if (updatedRawValue > 0)
		{
			TickCooldownTrackerConfig.CooldownValueMode mode = resolveMode(updatedRawValue, currentTick, configuredMode);
			int updatedCooldownTicks = convertToTicks(updatedRawValue, currentTick, mode);
			if (updatedRawValue != rawValue || updatedCooldownTicks < cooldownTicks || cooldownTicks == 0)
			{
				cooldownTicks = updatedCooldownTicks;
			}

			highestObservedCooldownTicks = Math.max(highestObservedCooldownTicks, cooldownTicks);
			rawValue = updatedRawValue;
			readySinceTick = -1;
			return;
		}

		if (wasActive && readySinceTick < 0)
		{
			readySinceTick = currentTick;
		}
		cooldownTicks = 0;
		rawValue = updatedRawValue;
	}

	void reset()
	{
		cooldownTicks = 0;
		highestObservedCooldownTicks = DEFAULT_FULL_COOLDOWN_TICKS;
		readySinceTick = -1;
		lastClientTick = -1;
		rawValue = -1;
		resolvedMode = null;
	}

	int reduceFromDamage(int damage, int currentTick)
	{
		advanceTo(currentTick);
		int reductionTicks = Math.max(0, damage / 10);
		if (reductionTicks == 0 || cooldownTicks == 0)
		{
			return 0;
		}

		cooldownTicks = Math.max(0, cooldownTicks - reductionTicks);
		if (cooldownTicks == 0 && readySinceTick < 0)
		{
			readySinceTick = currentTick;
		}
		return reductionTicks;
	}

	boolean isActive()
	{
		return cooldownTicks > 0;
	}

	boolean isReady()
	{
		return cooldownTicks == 0;
	}

	boolean isRecentlyReady(int currentTick, int readyVisibleTicks)
	{
		return readySinceTick >= 0 && currentTick - readySinceTick < readyVisibleTicks;
	}

	int getCooldownTicks()
	{
		return cooldownTicks;
	}

	int getRawValue()
	{
		return Math.max(0, rawValue);
	}

	String getModeLabel()
	{
		return resolvedMode == null ? "unknown" : resolvedMode.name().toLowerCase();
	}

	double getCooldownRatio()
	{
		if (highestObservedCooldownTicks <= 0)
		{
			return 0;
		}

		return Math.min(1, Math.max(0, (double) cooldownTicks / highestObservedCooldownTicks));
	}

	private void advanceTo(int currentTick)
	{
		if (lastClientTick >= 0 && currentTick > lastClientTick && cooldownTicks > 0)
		{
			cooldownTicks = Math.max(0, cooldownTicks - (currentTick - lastClientTick));
		}
		lastClientTick = currentTick;
	}

	private TickCooldownTrackerConfig.CooldownValueMode resolveMode(
		int updatedRawValue,
		int currentTick,
		TickCooldownTrackerConfig.CooldownValueMode configuredMode)
	{
		if (configuredMode != TickCooldownTrackerConfig.CooldownValueMode.AUTO)
		{
			resolvedMode = configuredMode;
			return configuredMode;
		}

		if (resolvedMode != null)
		{
			return resolvedMode;
		}

		if (updatedRawValue > currentTick && updatedRawValue - currentTick <= MAX_REASONABLE_COOLDOWN_TICKS)
		{
			resolvedMode = TickCooldownTrackerConfig.CooldownValueMode.END_TICK;
		}
		else if (updatedRawValue > 220)
		{
			resolvedMode = TickCooldownTrackerConfig.CooldownValueMode.TICKS;
		}
		else
		{
			resolvedMode = TickCooldownTrackerConfig.CooldownValueMode.SECONDS;
		}

		return resolvedMode;
	}

	private int convertToTicks(int updatedRawValue, int currentTick, TickCooldownTrackerConfig.CooldownValueMode mode)
	{
		switch (mode)
		{
			case END_TICK:
				return Math.max(0, updatedRawValue - currentTick);
			case SECONDS:
				return (int) Math.round(updatedRawValue * 5.0 / 3.0);
			case TICKS:
			case AUTO:
			default:
				return updatedRawValue;
		}
	}
}
