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

		state.sync(300, 10, TickCooldownTrackerConfig.CooldownValueMode.TICKS);
		assertTrue(state.isActive());
		assertEquals(300, state.getCooldownTicks());

		state.sync(127, 42, TickCooldownTrackerConfig.CooldownValueMode.TICKS);
		assertEquals(127, state.getCooldownTicks());
	}

	@Test
	public void advancesLocalCountdownBetweenRawUpdates()
	{
		FlaskCooldownState state = new FlaskCooldownState();

		state.sync(180, 10, TickCooldownTrackerConfig.CooldownValueMode.SECONDS);
		state.sync(180, 11, TickCooldownTrackerConfig.CooldownValueMode.SECONDS);

		assertEquals(299, state.getCooldownTicks());
	}

	@Test
	public void marksRecentlyReadyWhenCooldownDropsToZero()
	{
		FlaskCooldownState state = new FlaskCooldownState();

		state.sync(2, 10, TickCooldownTrackerConfig.CooldownValueMode.TICKS);
		state.sync(0, 12, TickCooldownTrackerConfig.CooldownValueMode.TICKS);

		assertFalse(state.isActive());
		assertTrue(state.isReady());
		assertTrue(state.isRecentlyReady(16, 5));
		assertFalse(state.isRecentlyReady(17, 5));
	}

	@Test
	public void reducesOneTickPerTenDamage()
	{
		FlaskCooldownState state = new FlaskCooldownState();

		state.sync(300, 10, TickCooldownTrackerConfig.CooldownValueMode.TICKS);

		assertEquals(0, state.reduceFromDamage(9, 10));
		assertEquals(300, state.getCooldownTicks());
		assertEquals(1, state.reduceFromDamage(10, 10));
		assertEquals(299, state.getCooldownTicks());
		assertEquals(4, state.reduceFromDamage(40, 10));
		assertEquals(295, state.getCooldownTicks());
	}

	@Test
	public void decodesPackedHighBitFlagAsReadyMetadata()
	{
		FlaskCooldownState state = new FlaskCooldownState();

		state.sync(32768, 10, TickCooldownTrackerConfig.CooldownValueMode.TICKS);
		assertFalse(state.isActive());
		assertTrue(state.isReady());
		assertEquals(0, state.getCooldownTicks());
	}

	@Test
	public void decodesLargePackedReusableItemValueAsTicks()
	{
		FlaskCooldownState state = new FlaskCooldownState();

		state.sync(253600, 10, TickCooldownTrackerConfig.CooldownValueMode.AUTO);

		assertTrue(state.isActive());
		assertEquals(247, state.getCooldownTicks());
		assertEquals("ticks+packed", state.getModeLabel());
	}

	@Test
	public void ignoresLowByteMetadataWhenPackedValueContainsRemainingTicks()
	{
		FlaskCooldownState state = new FlaskCooldownState();

		state.sync((45 << 10) | 149, 10, TickCooldownTrackerConfig.CooldownValueMode.AUTO);

		assertTrue(state.isActive());
		assertEquals(45, state.getCooldownTicks());
	}

	@Test
	public void treatsPackedMetadataOnlyValueAsReady()
	{
		FlaskCooldownState state = new FlaskCooldownState();

		state.sync(32768, 10, TickCooldownTrackerConfig.CooldownValueMode.AUTO);

		assertFalse(state.isActive());
		assertTrue(state.isReady());
		assertEquals(0, state.getCooldownTicks());
	}

	@Test
	public void keepsProgressRatioBounded()
	{
		FlaskCooldownState state = new FlaskCooldownState();

		state.sync(600, 1, TickCooldownTrackerConfig.CooldownValueMode.TICKS);
		assertEquals(1.0, state.getCooldownRatio(), 0.0001);

		state.sync(300, 2, TickCooldownTrackerConfig.CooldownValueMode.TICKS);
		assertEquals(0.5, state.getCooldownRatio(), 0.0001);
	}
}
