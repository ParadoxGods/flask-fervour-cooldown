package com.tickcooldowntracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TickCooldownTrackerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TickCooldownTrackerPlugin.class);
		RuneLite.main(args);
	}
}
