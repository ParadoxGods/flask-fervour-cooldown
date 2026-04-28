package com.tickcooldowntracker;

final class CooldownSavingsTracker
{
	private int savedTicks;
	private int previousRemainingTicks = -1;
	private int previousClock = -1;
	private boolean completionReported = true;

	void reset()
	{
		savedTicks = 0;
		previousRemainingTicks = -1;
		previousClock = -1;
		completionReported = true;
	}

	void startCycle()
	{
		savedTicks = 0;
		previousRemainingTicks = -1;
		previousClock = -1;
		completionReported = false;
	}

	void addSavedTicks(int ticks)
	{
		savedTicks += Math.max(0, ticks);
	}

	void markSnapshot(int remainingTicks, int clock)
	{
		remainingTicks = Math.max(0, remainingTicks);
		if (remainingTicks > 0 && (previousRemainingTicks <= 0 || remainingTicks > previousRemainingTicks))
		{
			startCycle();
		}
		previousRemainingTicks = remainingTicks;
		previousClock = clock;
		if (remainingTicks > 0)
		{
			completionReported = false;
		}
	}

	boolean complete(int clock)
	{
		boolean completed = previousRemainingTicks > 0 && !completionReported;
		previousRemainingTicks = 0;
		previousClock = clock;
		completionReported = true;
		return completed;
	}

	boolean observe(int remainingTicks, int clock)
	{
		remainingTicks = Math.max(0, remainingTicks);
		if (remainingTicks > 0 && (previousRemainingTicks <= 0 || remainingTicks > previousRemainingTicks))
		{
			startCycle();
		}
		else if (previousRemainingTicks > 0)
		{
			int elapsedTicks = previousClock >= 0 ? Math.max(0, clock - previousClock) : 0;
			int expectedRemainingTicks = Math.max(0, previousRemainingTicks - elapsedTicks);
			int savedSinceLastSnapshot = expectedRemainingTicks - remainingTicks;
			if (savedSinceLastSnapshot > 0)
			{
				savedTicks += savedSinceLastSnapshot;
			}
		}

		boolean completed = previousRemainingTicks > 0 && remainingTicks == 0 && !completionReported;
		previousRemainingTicks = remainingTicks;
		previousClock = clock;
		if (completed)
		{
			completionReported = true;
		}
		return completed;
	}

	int getSavedTicks()
	{
		return savedTicks;
	}
}
