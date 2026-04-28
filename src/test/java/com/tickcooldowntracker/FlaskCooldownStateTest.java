package com.tickcooldowntracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FlaskCooldownStateTest
{
	@Test
	public void startsFullCooldown()
	{
		FlaskCooldownState state = new FlaskCooldownState();

		state.startCooldown(10);

		assertTrue(state.isActive());
		assertEquals(300, state.getCooldownTicks());
	}

	@Test
	public void startsFullCooldownFromNextServerTick()
	{
		FlaskCooldownState state = new FlaskCooldownState();

		state.startCooldownOnNextTick(10);
		state.advanceTo(11);

		assertEquals(300, state.getCooldownTicks());
		state.advanceTo(12);
		assertEquals(299, state.getCooldownTicks());
	}

	@Test
	public void advancesLocalCountdownBetweenRawUpdates()
	{
		FlaskCooldownState state = new FlaskCooldownState();

		state.setCooldownTicks(300, 10);
		state.advanceTo(11);

		assertEquals(299, state.getCooldownTicks());
	}

	@Test
	public void reducesOneTickPerTenDamage()
	{
		FlaskCooldownState state = new FlaskCooldownState();

		state.setCooldownTicks(300, 10);

		assertEquals(0, state.reduceFromDamage(9, 10));
		assertEquals(300, state.getCooldownTicks());
		assertEquals(1, state.reduceFromDamage(10, 10));
		assertEquals(299, state.getCooldownTicks());
		assertEquals(4, state.reduceFromDamage(40, 10));
		assertEquals(295, state.getCooldownTicks());
	}

	@Test
	public void calibratesRemainingTicksFromExternalSource()
	{
		FlaskCooldownState state = new FlaskCooldownState();

		state.startCooldown(10);
		state.setCooldownTicks(45, 20);

		assertTrue(state.isActive());
		assertEquals(45, state.getCooldownTicks());
	}

	@Test
	public void decodesAbsoluteCooldownEndTickFromMapClock()
	{
		assertEquals(27, TickCooldownTrackerPlugin.decodeCooldownTicks(253627, 253600));
		assertEquals(0, TickCooldownTrackerPlugin.decodeCooldownTicks(253600, 253600));
		assertEquals(0, TickCooldownTrackerPlugin.decodeCooldownTicks(253500, 253600));
	}

	@Test
	public void formatsSavedSecondsFromTicks()
	{
		assertEquals("0", TickCooldownTrackerPlugin.formatSavedSeconds(0));
		assertEquals("0.6", TickCooldownTrackerPlugin.formatSavedSeconds(1));
		assertEquals("3", TickCooldownTrackerPlugin.formatSavedSeconds(5));
		assertEquals("12", TickCooldownTrackerPlugin.formatSavedSeconds(20));
	}

	@Test
	public void keepsProgressRatioBounded()
	{
		FlaskCooldownState state = new FlaskCooldownState();

		state.setCooldownTicks(600, 1);
		assertEquals(1.0, state.getCooldownRatio(), 0.0001);

		state.setCooldownTicks(300, 2);
		assertEquals(0.5, state.getCooldownRatio(), 0.0001);
	}
}
