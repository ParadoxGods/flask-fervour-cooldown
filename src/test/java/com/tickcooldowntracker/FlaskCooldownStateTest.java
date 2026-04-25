package com.tickcooldowntracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FlaskCooldownStateTest
{
	@Test
	public void followsVarpTicksExactly()
	{
		FlaskCooldownState state = new FlaskCooldownState();

		state.sync(300, 10);
		assertTrue(state.isActive());
		assertEquals(300, state.getCooldownTicks());

		state.sync(127, 42);
		assertEquals(127, state.getCooldownTicks());
	}

	@Test
	public void marksRecentlyReadyWhenCooldownDropsToZero()
	{
		FlaskCooldownState state = new FlaskCooldownState();

		state.sync(2, 10);
		state.sync(0, 12);

		assertFalse(state.isActive());
		assertTrue(state.isReady());
		assertTrue(state.isRecentlyReady(16, 5));
		assertFalse(state.isRecentlyReady(17, 5));
	}

	@Test
	public void keepsProgressRatioBounded()
	{
		FlaskCooldownState state = new FlaskCooldownState();

		state.sync(600, 1);
		assertEquals(1.0, state.getCooldownRatio(), 0.0001);

		state.sync(300, 2);
		assertEquals(0.5, state.getCooldownRatio(), 0.0001);
	}
}
