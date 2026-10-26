package com.specbar;

import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
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
    private final SpecBarConfig config;

    // Spec bar bounds relative to the combat interface, remembered while the bar is
    // visible so the "no spec weapon" border lands exactly where the bar would be.
    private Rectangle lastBarOffset;

    @Inject
    private SpecBarOverlay(Client client, SpecBarPlugin plugin, SpecBarConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        Widget energyWidget = client.getWidget(InterfaceID.CombatInterface.CURRENTENERGY_COLOUR);
        if (energyWidget == null)
        {
            return null;
        }

        Widget combatTab = client.getWidget(InterfaceID.CombatInterface.UNIVERSE);
        if (energyWidget.isHidden())
        {
            renderBorderWithoutSpecWeapon(graphics, energyWidget, combatTab);
            return null;
        }

        Rectangle bounds = energyWidget.getBounds();
        if (bounds == null)
        {
            return null;
        }
        rememberBarOffset(bounds, combatTab);

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

    private void rememberBarOffset(Rectangle bounds, Widget combatTab)
    {
        if (combatTab == null || combatTab.isHidden())
        {
            return;
        }
        Rectangle tab = combatTab.getBounds();
        lastBarOffset = new Rectangle(bounds.x - tab.x, bounds.y - tab.y, bounds.width, bounds.height);
    }

    private void renderBorderWithoutSpecWeapon(Graphics2D graphics, Widget energyWidget, Widget combatTab)
    {
        if (!config.showBorderWithoutSpecWeapon() || combatTab == null || combatTab.isHidden())
        {
            return;
        }

        Rectangle bounds = barBoundsWhileHidden(energyWidget, combatTab);
        if (bounds == null || bounds.isEmpty())
        {
            return;
        }

        Graphics2D g2d = (Graphics2D) graphics.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        renderEffect(g2d, bounds, plugin.getIdleAreaColor(), plugin.getIdleBorderColor());
        g2d.dispose();
    }

    private Rectangle barBoundsWhileHidden(Widget energyWidget, Widget combatTab)
    {
        Rectangle tab = combatTab.getBounds();
        if (lastBarOffset != null)
        {
            return new Rectangle(tab.x + lastBarOffset.x, tab.y + lastBarOffset.y,
                    lastBarOffset.width, lastBarOffset.height);
        }

        // No spec weapon equipped yet this session: resolve the bar's position from its
        // layout offsets up to the first visible ancestor.
        int x = 0;
        int y = 0;
        Widget w = energyWidget;
        while (w != null && w.isHidden())
        {
            x += w.getRelativeX();
            y += w.getRelativeY();
            w = w.getParent();
        }
        if (w == null)
        {
            return null;
        }
        net.runelite.api.Point origin = w.getCanvasLocation();
        if (origin == null)
        {
            return null;
        }
        return new Rectangle(origin.getX() + x, origin.getY() + y, energyWidget.getWidth(), energyWidget.getHeight());
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
