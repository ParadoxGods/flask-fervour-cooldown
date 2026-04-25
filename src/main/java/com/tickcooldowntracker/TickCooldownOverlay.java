package com.tickcooldowntracker;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

class TickCooldownOverlay extends OverlayPanel
{
	private final TickCooldownTrackerPlugin plugin;
	private final TickCooldownTrackerConfig config;

	@Inject
	TickCooldownOverlay(TickCooldownTrackerPlugin plugin, TickCooldownTrackerConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(PRIORITY_MED);
		getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Tick cooldown overlay"));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showPanel())
		{
			return null;
		}

		List<CooldownState> visibleStates = plugin.getVisibleStates();
		if (visibleStates.isEmpty())
		{
			return null;
		}

		setPreferredSize(new Dimension(config.panelWidth(), 0));
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Cooldowns")
			.color(Color.WHITE)
			.build());

		int currentTick = plugin.getCurrentTick();
		for (CooldownState state : visibleStates)
		{
			int remainingTicks = state.getRemainingTicks(currentTick);
			boolean active = remainingTicks > 0;
			if (!active && !config.showReadyInPanel())
			{
				continue;
			}

			panelComponent.getChildren().add(LineComponent.builder()
				.left(state.getDisplayName())
				.right(active ? remainingTicks + "t" : "READY")
				.rightColor(active ? Color.WHITE : config.readyColor().brighter())
				.build());
		}

		return super.render(graphics);
	}
}
