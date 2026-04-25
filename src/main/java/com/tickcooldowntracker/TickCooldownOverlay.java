package com.tickcooldowntracker;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
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
		if (!config.showPanel() || (!plugin.isCooldownActive() && !(config.showReadyInPanel() && plugin.shouldShowReadyPanel())))
		{
			return null;
		}

		setPreferredSize(new Dimension(config.panelWidth(), 0));
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Flask of Fervour")
			.color(Color.WHITE)
			.build());

		boolean active = plugin.isCooldownActive();
		panelComponent.getChildren().add(LineComponent.builder()
			.left(active ? "Cooldown" : "Status")
			.right(active ? plugin.getCooldownTicks() + "t" : "READY")
			.rightColor(active ? Color.WHITE : config.readyColor().brighter())
			.build());

		if (config.showDebugRawValue())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Raw")
				.right(plugin.getRawCooldownValue() + " / " + plugin.getCooldownModeLabel())
				.rightColor(Color.LIGHT_GRAY)
				.build());

			panelComponent.getChildren().add(LineComponent.builder()
				.left("Last hit")
				.right(plugin.getLastDamage() + " -> -" + plugin.getLastDamageReductionTicks() + "t")
				.rightColor(Color.LIGHT_GRAY)
				.build());
		}

		return super.render(graphics);
	}
}
