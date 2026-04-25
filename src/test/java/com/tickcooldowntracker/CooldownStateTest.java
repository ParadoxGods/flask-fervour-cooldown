package com.tickcooldowntracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CooldownStateTest
{
	@Test
	public void tracksRemainingTicksFromStartTick()
	{
		CooldownDefinition definition = CooldownDefinition.parseAll("100: 5").get(0);
		CooldownState state = new CooldownState(definition, 100, "Test item", 20);

		assertEquals(5, state.getRemainingTicks(20));
		assertEquals(4, state.getRemainingTicks(21));
		assertEquals(0, state.getRemainingTicks(25));
		assertTrue(state.isActive(24));
		assertFalse(state.isActive(25));
	}

	@Test
	public void expiresAfterReadyWindow()
	{
		CooldownDefinition definition = CooldownDefinition.parseAll("100: 3").get(0);
		CooldownState state = new CooldownState(definition, 100, "Test item", 10);

		assertFalse(state.isExpired(13, 5));
		assertFalse(state.isExpired(17, 5));
		assertTrue(state.isExpired(18, 5));
	}
}
