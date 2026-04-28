package com.tickcooldowntracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CooldownSavingsTrackerTest
{
	@Test
	public void normalTickProgressDoesNotCountAsSavedTime()
	{
		CooldownSavingsTracker tracker = new CooldownSavingsTracker();

		assertFalse(tracker.observe(300, 1000));
		assertFalse(tracker.observe(299, 1001));
		assertFalse(tracker.observe(250, 1050));

		assertEquals(0, tracker.getSavedTicks());
	}

	@Test
	public void extraCooldownDropsCountAsSavedTime()
	{
		CooldownSavingsTracker tracker = new CooldownSavingsTracker();

		tracker.observe(300, 1000);
		tracker.observe(295, 1001);
		tracker.observe(292, 1002);

		assertEquals(6, tracker.getSavedTicks());
	}

	@Test
	public void reportsCompletionOnce()
	{
		CooldownSavingsTracker tracker = new CooldownSavingsTracker();

		tracker.observe(10, 1000);
		tracker.observe(4, 1001);

		assertTrue(tracker.observe(0, 1002));
		assertFalse(tracker.observe(0, 1003));
		assertEquals(8, tracker.getSavedTicks());
	}

	@Test
	public void manualSavedTicksAreIncluded()
	{
		CooldownSavingsTracker tracker = new CooldownSavingsTracker();

		tracker.markSnapshot(10, 100);
		tracker.addSavedTicks(3);

		assertTrue(tracker.complete(101));
		assertEquals(3, tracker.getSavedTicks());
	}
}
