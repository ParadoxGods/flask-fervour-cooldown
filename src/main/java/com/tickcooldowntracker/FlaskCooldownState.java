package com.tickcooldowntracker;

final class FlaskCooldownState
{
	static final int DEFAULT_FULL_COOLDOWN_TICKS = 300;

	private int cooldownTicks;
	private int highestObservedCooldownTicks = DEFAULT_FULL_COOLDOWN_TICKS;
	private int readySinceTick = -1;

	void sync(int varpValue, int currentTick)
	{
		int updatedCooldownTicks = Math.max(0, varpValue);
		if (updatedCooldownTicks > 0)
		{
			cooldownTicks = updatedCooldownTicks;
			highestObservedCooldownTicks = Math.max(highestObservedCooldownTicks, updatedCooldownTicks);
			readySinceTick = -1;
			return;
		}

		if (cooldownTicks > 0 && readySinceTick < 0)
		{
			readySinceTick = currentTick;
		}
		cooldownTicks = 0;
	}

	void reset()
	{
		cooldownTicks = 0;
		highestObservedCooldownTicks = DEFAULT_FULL_COOLDOWN_TICKS;
		readySinceTick = -1;
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

	double getCooldownRatio()
	{
		if (highestObservedCooldownTicks <= 0)
		{
			return 0;
		}

		return Math.min(1, Math.max(0, (double) cooldownTicks / highestObservedCooldownTicks));
	}
}
