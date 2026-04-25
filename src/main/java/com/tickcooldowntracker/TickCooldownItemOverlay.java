package com.tickcooldowntracker;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.components.TextComponent;

class TickCooldownItemOverlay extends WidgetItemOverlay
{
	private final TickCooldownTrackerPlugin plugin;
	private final TickCooldownTrackerConfig config;

	@Inject
	TickCooldownItemOverlay(TickCooldownTrackerPlugin plugin, TickCooldownTrackerConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		showOnInventory();
		showOnEquipment();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!plugin.isFlaskItem(itemId))
		{
			return;
		}

		Rectangle bounds = widgetItem.getCanvasBounds();

		if (plugin.isCooldownActive())
		{
			renderActive(graphics, bounds, plugin.getCooldownTicks(), plugin.getCooldownRatio());
			return;
		}

		if (config.showReadyHighlight() && plugin.shouldShowReadyItem())
		{
			renderReady(graphics, bounds);
		}
	}

	private void renderActive(Graphics2D graphics, Rectangle bounds, int remainingTicks, double remainingRatio)
	{
		if (config.showCooldownFill())
		{
			graphics.setColor(config.cooldownFillColor());
			graphics.fill(bounds);
		}

		graphics.setColor(config.cooldownFillColor().brighter());
		graphics.draw(bounds);

		if (config.showProgressBar())
		{
			int progressWidth = (int) Math.ceil(bounds.width * remainingRatio);
			graphics.fillRect(bounds.x, bounds.y + bounds.height - 4, progressWidth, 4);
		}

		if (config.showItemTicks())
		{
			graphics.setFont(FontManager.getRunescapeSmallFont());
			TextComponent textComponent = new TextComponent();
			textComponent.setText(remainingTicks + "t");
			textComponent.setColor(config.activeTextColor());
			textComponent.setPosition(new Point(bounds.x + 1, bounds.y + 15));
			textComponent.render(graphics);
		}
	}

	private void renderReady(Graphics2D graphics, Rectangle bounds)
	{
		graphics.setColor(config.readyColor());
		graphics.fill(bounds);

		graphics.setStroke(new BasicStroke(2));
		graphics.draw(bounds);
		graphics.setStroke(new BasicStroke(1));

		if (config.showItemTicks())
		{
			graphics.setFont(FontManager.getRunescapeSmallFont());
			TextComponent textComponent = new TextComponent();
			textComponent.setText("OK");
			textComponent.setColor(config.readyColor().brighter());
			textComponent.setPosition(new Point(bounds.x + 1, bounds.y + 15));
			textComponent.render(graphics);
		}
	}
}
