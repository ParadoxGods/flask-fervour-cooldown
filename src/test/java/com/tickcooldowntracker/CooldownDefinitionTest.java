package com.tickcooldowntracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class CooldownDefinitionTest
{
	@Test
	public void parsesIdAndNameDefinitions()
	{
		List<CooldownDefinition> definitions = CooldownDefinition.parseAll("8013: 100; Dragon dagger = 4\n# ignored\nbad");

		assertEquals(2, definitions.size());
		assertEquals(Integer.valueOf(8013), definitions.get(0).getItemId());
		assertEquals(100, definitions.get(0).getTicks());
		assertEquals("Dragon dagger", definitions.get(1).getLabel());
		assertEquals(4, definitions.get(1).getTicks());
	}

	@Test
	public void ignoresInvalidDefinitions()
	{
		List<CooldownDefinition> definitions = CooldownDefinition.parseAll("0: 10, 123: 0, abc: nope, missing");

		assertTrue(definitions.isEmpty());
	}
}
