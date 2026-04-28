package com.tickcooldowntracker;

final class FlaskCooldownState
{
	static final int DEFAULT_FULL_COOLDOWN_TICKS = 300;

	private int cooldownTicks;
	private int highestObservedCooldownTicks = DEFAULT_FULL_COOLDOWN_TICKS;
	private int lastClientTick = -1;

	void startCooldown(int currentTick)
	{
		setCooldownTicks(DEFAULT_FULL_COOLDOWN_TICKS, currentTick);
	}

	void startCooldownOnNextTick(int currentTick)
	{
		setCooldownTicks(DEFAULT_FULL_COOLDOWN_TICKS, currentTick + 1);
	}

	void setCooldownTicks(int remainingTicks, int currentTick)
	{
		advanceTo(currentTick);
		cooldownTicks = Math.max(0, remainingTicks);
		highestObservedCooldownTicks = Math.max(highestObservedCooldownTicks, cooldownTicks);
	}

	void reset()
	{
		cooldownTicks = 0;
		highestObservedCooldownTicks = DEFAULT_FULL_COOLDOWN_TICKS;
		lastClientTick = -1;
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

	void advanceTo(int currentTick)
	{
		if (lastClientTick >= 0 && currentTick > lastClientTick && cooldownTicks > 0)
		{
			cooldownTicks = Math.max(0, cooldownTicks - (currentTick - lastClientTick));
		}
		lastClientTick = currentTick;
	}
}
