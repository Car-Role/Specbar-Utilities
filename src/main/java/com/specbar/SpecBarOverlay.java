package com.specbar;

import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

public class SpecBarOverlay extends Overlay
{
    private final Client client;
    private final SpecBarPlugin plugin;

    @Inject
    private SpecBarOverlay(Client client, SpecBarPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // Use direct widget access for performance (593:42 is the known working widget)
        Widget specBarWidget = client.getWidget(593, 42);
        if (specBarWidget == null || specBarWidget.isHidden())
        {
            return null;
        }

        Rectangle bounds = specBarWidget.getBounds();
        if (bounds == null)
        {
            return null;
        }

        // Check if any effect should be rendered
        boolean hasClickEffect = plugin.isClicked();
        boolean hasHoverEffect = plugin.isHovering();
        boolean hasIdleEffect = plugin.isIdleBorderEnabled() && !hasClickEffect && !hasHoverEffect;
        
        // Early exit if no effects are active
        if (!hasClickEffect && !hasHoverEffect && !hasIdleEffect)
        {
            return null;
        }

        // Create a graphics context only when needed
        Graphics2D g2d = (Graphics2D) graphics.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Render effects in priority order: Click > Hover > Idle
        if (hasClickEffect)
        {
            renderEffect(g2d, bounds, plugin.getClickAreaColor(), plugin.getClickBorderColor());
        }
        else if (hasHoverEffect)
        {
            renderEffect(g2d, bounds, plugin.getHoverAreaColor(), plugin.getHoverBorderColor());
        }
        else if (hasIdleEffect)
        {
            renderEffect(g2d, bounds, plugin.getIdleAreaColor(), plugin.getIdleBorderColor());
        }

        g2d.dispose();
        return null;
    }

    private void renderEffect(Graphics2D g2d, Rectangle bounds, 
                             Color areaColor, Color borderColor)
    {
        // Draw area fill (color already includes opacity from @Alpha annotation)
        g2d.setColor(areaColor);
        g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

        // Draw border (color already includes opacity from @Alpha annotation)
        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
    }
    
    private void renderIdleBorder(Graphics2D g2d, Rectangle bounds, Color borderColor)
    {
        // Draw only border for idle state (no area fill)
        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(1)); // Thinner stroke for subtle idle indication
        g2d.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
    }
}
