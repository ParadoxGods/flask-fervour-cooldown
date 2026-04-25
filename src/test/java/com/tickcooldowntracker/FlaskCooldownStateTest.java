package com.tickcooldowntracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
	public void advancesLocalCountdownBetweenRawUpdates()
	{
		FlaskCooldownState state = new FlaskCooldownState();

		state.setCooldownTicks(300, 10);
		state.advanceTo(11);

		assertEquals(299, state.getCooldownTicks());
	}

	@Test
	public void marksRecentlyReadyWhenCooldownDropsToZero()
	{
		FlaskCooldownState state = new FlaskCooldownState();

		state.setCooldownTicks(2, 10);
		state.advanceTo(12);

		assertFalse(state.isActive());
		assertTrue(state.isReady());
		assertTrue(state.isRecentlyReady(16, 5));
		assertFalse(state.isRecentlyReady(17, 5));
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
	public void keepsProgressRatioBounded()
	{
		FlaskCooldownState state = new FlaskCooldownState();

		state.setCooldownTicks(600, 1);
		assertEquals(1.0, state.getCooldownRatio(), 0.0001);

		state.setCooldownTicks(300, 2);
		assertEquals(0.5, state.getCooldownRatio(), 0.0001);
	}
}
