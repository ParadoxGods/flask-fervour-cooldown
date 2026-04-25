package com.tickcooldowntracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class CooldownDefinition
{
	private final String label;
	private final Integer itemId;
	private final String itemName;
	private final int ticks;

	private CooldownDefinition(String label, Integer itemId, String itemName, int ticks)
	{
		this.label = label;
		this.itemId = itemId;
		this.itemName = itemName;
		this.ticks = ticks;
	}

	static List<CooldownDefinition> parseAll(String rawDefinitions)
	{
		if (rawDefinitions == null || rawDefinitions.trim().isEmpty())
		{
			return Collections.emptyList();
		}

		List<CooldownDefinition> definitions = new ArrayList<>();
		String[] entries = rawDefinitions.split("[\\r\\n;,]+");
		for (String entry : entries)
		{
			CooldownDefinition definition = parse(entry);
			if (definition != null)
			{
				definitions.add(definition);
			}
		}
		return definitions;
	}

	private static CooldownDefinition parse(String rawEntry)
	{
		String entry = stripComment(rawEntry).trim();
		if (entry.isEmpty())
		{
			return null;
		}

		int separator = Math.max(entry.lastIndexOf(':'), entry.lastIndexOf('='));
		if (separator <= 0 || separator == entry.length() - 1)
		{
			return null;
		}

		String rawItem = entry.substring(0, separator).trim();
		String rawTicks = entry.substring(separator + 1).trim();
		if (rawItem.isEmpty() || rawTicks.isEmpty())
		{
			return null;
		}

		int ticks;
		try
		{
			ticks = Integer.parseInt(rawTicks);
		}
		catch (NumberFormatException ex)
		{
			return null;
		}

		if (ticks <= 0)
		{
			return null;
		}

		try
		{
			int itemId = Integer.parseInt(rawItem);
			if (itemId > 0)
			{
				return new CooldownDefinition(rawItem, itemId, null, ticks);
			}

			return null;
		}
		catch (NumberFormatException ignored)
		{
			// Non-numeric keys are treated as exact item names.
		}

		return new CooldownDefinition(rawItem, null, normalize(rawItem), ticks);
	}

	private static String stripComment(String rawEntry)
	{
		int commentIndex = rawEntry.indexOf('#');
		return commentIndex >= 0 ? rawEntry.substring(0, commentIndex) : rawEntry;
	}

	static String normalize(String value)
	{
		return value.trim().toLowerCase(Locale.ROOT);
	}

	boolean matches(int rawItemId, int canonicalItemId, String normalizedItemName, int canonicalDefinitionId)
	{
		if (itemId != null)
		{
			return itemId == rawItemId || itemId == canonicalItemId || canonicalDefinitionId == canonicalItemId;
		}

		return itemName.equals(normalizedItemName);
	}

	String stateKey(int canonicalDefinitionId)
	{
		if (itemId != null)
		{
			int keyItemId = canonicalDefinitionId > 0 ? canonicalDefinitionId : itemId;
			return "id:" + keyItemId;
		}

		return "name:" + itemName;
	}

	String getLabel()
	{
		return label;
	}

	Integer getItemId()
	{
		return itemId;
	}

	int getTicks()
	{
		return ticks;
	}
}
