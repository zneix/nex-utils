package nexutils;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;

public class AltarOverlay extends Overlay {
	private final NexUtilsPlugin plugin;
	private final NexUtilsConfig config;
	private final Client client;
	public static final Color CLICKBOX_FILL_COLOR = new Color(205, 50, 50, 50);

	@Inject
	AltarOverlay(NexUtilsPlugin plugin, NexUtilsConfig config, Client client) {
		super(plugin);

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);

		this.plugin = plugin;
		this.config = config;
		this.client = client;
	}

	@Override
	public Dimension render(Graphics2D graphics) {
		if (!config.altarPreventNoZarosItem() || plugin.altarObject == null) {
			return null;
		}

		if (plugin.isZarosItemEquipped()) {
			return null;
		}

		Point mousePosition = client.getMouseCanvasPosition();
		if (client.getTopLevelWorldView().getPlane() == plugin.altarObject.getPlane()) {
			// Draw image on the object
			Point baseImageLocation = plugin.altarObject.getCanvasTextLocation(graphics, "", 125);

			if (baseImageLocation == null) {
				return null;
			}
			BufferedImage generatedEntranceOverlayImage = plugin.getAltarOverlayImage();
			int xOffset = generatedEntranceOverlayImage.getWidth() / 2;
			int yOffset = generatedEntranceOverlayImage.getHeight() / 2;

			Point imageLocation = new Point(baseImageLocation.getX() - xOffset, baseImageLocation.getY() - yOffset);
			OverlayUtil.renderImageLocation(graphics, imageLocation, generatedEntranceOverlayImage);

			// Fill the object with colour
			OverlayUtil.renderHoverableArea(graphics, plugin.altarObject.getClickbox(), mousePosition,
				CLICKBOX_FILL_COLOR, // fill colour
				Color.BLACK, // border
				Color.BLACK // hover border
			);
		}

		return null;
	}
}
