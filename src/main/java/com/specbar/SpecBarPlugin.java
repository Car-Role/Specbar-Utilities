package com.specbar;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@Slf4j
@PluginDescriptor(
    name = "Spec Bar Utilities",
    description = "Enhances the special attack bar with visual feedback for hover and click states",
    tags = {"combat", "ui", "special attack"}
)
public class SpecBarPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private SpecBarConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private SpecBarOverlay specBarOverlay;

    private boolean isHovering = false;
    private boolean mouseOverSpecBar = false;
    private int tickCounter = 0;
    private long lastClickClearTime = 0; // Track when click state was cleared
    private static final long HOVER_SUPPRESS_DURATION_MS = 300; // Suppress hover for 300ms after click clear

    // Spec armed state comes from the game (varp SA_ATTACK). A click shows the
    // expected state immediately until the server confirms it or the window expires.
    private boolean specArmed = false;
    private boolean predictedArmed = false;
    private long predictionExpiresAt = 0;
    private static final long PREDICTION_WINDOW_MS = 1200; // ~2 game ticks
    
    // Widget caching for performance
    private Widget cachedSpecBarWidget = null;
    private long lastWidgetSearchTime = 0;
    private static final long WIDGET_CACHE_DURATION_MS = 10000; // Cache for 10 seconds

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(specBarOverlay);
        log.info("Spec Bar Utilities started!");
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(specBarOverlay);
        isHovering = false;
        mouseOverSpecBar = false;
        specArmed = false;
        predictionExpiresAt = 0;
        log.info("Spec Bar Utilities stopped!");
    }

    @Subscribe
    public void onClientTick(ClientTick event)
    {
        boolean wasShowingClick = isClickStateActive();
        boolean armed = client.getVarpValue(VarPlayerID.SA_ATTACK) == 1;
        if (armed != specArmed)
        {
            specArmed = armed;
            predictionExpiresAt = 0; // server state changed, it wins over the prediction
        }
        if (wasShowingClick && !isClickStateActive())
        {
            lastClickClearTime = System.currentTimeMillis();
        }
        
        // Throttle hover detection to every 3rd tick for performance (still responsive at ~50fps)
        tickCounter++;
        if (tickCounter % 3 != 0)
        {
            return;
        }
        
        // Check if mouse is hovering over the special attack bar
        Widget specBarWidget = getCachedSpecialAttackWidget();
        if (specBarWidget != null && !specBarWidget.isHidden())
        {
            // Hide spec bar text if option is enabled
            if (config.hideSpecBarText())
            {
                String currentText = specBarWidget.getText();
                if (currentText != null && !currentText.isEmpty())
                {
                    specBarWidget.setText("");
                }
            }
            
            Rectangle bounds = specBarWidget.getBounds();
            if (bounds != null)
            {
                net.runelite.api.Point mousePos = client.getMouseCanvasPosition();
                mouseOverSpecBar = bounds.contains(mousePos.getX(), mousePos.getY());
                
                // Suppress hover effect for a short time after click state is cleared
                long currentTime = System.currentTimeMillis();
                boolean hoverSuppressed = (currentTime - lastClickClearTime) < HOVER_SUPPRESS_DURATION_MS;
                
                isHovering = mouseOverSpecBar && !hoverSuppressed;
            }
            else
            {
                mouseOverSpecBar = false;
                isHovering = false;
            }
        }
        else
        {
            mouseOverSpecBar = false;
            isHovering = false;
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        // Optimized click detection with minimal string operations
        String menuTarget = event.getMenuTarget();
        
        // Fast path: mouse is over the bar and a relevant click happens.
        // Uses the raw mouse position so clicks during hover suppression still count.
        if (mouseOverSpecBar)
        {
            String menuOption = event.getMenuOption();
            if (menuOption != null && !menuOption.equals("Walk here") && !menuOption.equals("Examine"))
            {
                predictToggle();
                return;
            }
        }
        
        // Check for specific special attack clicks (reduced string comparisons)
        if (menuTarget != null && menuTarget.length() < 50) // Performance: skip very long targets
        {
            if (menuTarget.contains("Special Attack") || 
                (menuTarget.contains("%") && menuTarget.length() < 30))
            {
                String menuOption = event.getMenuOption();
                if (menuOption != null && (menuOption.equals("Use") || menuOption.equals("Activate")))
                {
                    predictToggle();
                }
            }
        }
    }
    
    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        // Only process game messages
        if (event.getType() != ChatMessageType.GAMEMESSAGE)
        {
            return;
        }
        
        // Check for the "not enough power" message when in clicked state
        if (event.getMessage().equals("You don't have enough power left."))
        {
            // Failed special attack attempt - drop the prediction and show the real state
            predictionExpiresAt = 0;
            log.debug("Special attack failed - not enough power");
        }
    }
    
    private Widget getCachedSpecialAttackWidget()
    {
        long currentTime = System.currentTimeMillis();
        
        // Check if we have a valid cached widget
        if (cachedSpecBarWidget != null && 
            currentTime - lastWidgetSearchTime < WIDGET_CACHE_DURATION_MS)
        {
            // Verify cached widget is still valid
            if (!cachedSpecBarWidget.isHidden())
            {
                return cachedSpecBarWidget;
            }
        }
        
        // Cache expired or invalid
        Widget specBarWidget = client.getWidget(InterfaceID.CombatInterface.SP_ATTACKBAR); // Special attack bar component
        if (specBarWidget != null && !specBarWidget.isHidden())
        {
            cachedSpecBarWidget = specBarWidget;
            lastWidgetSearchTime = currentTime;
            return specBarWidget;
        }
        
        return null;
    }
    
    private void predictToggle()
    {
        long now = System.currentTimeMillis();
        boolean next = !resolveClickState(specArmed, predictedArmed, predictionExpiresAt, now);
        if (!next)
        {
            lastClickClearTime = now; // Hover effect will be suppressed for HOVER_SUPPRESS_DURATION_MS
        }
        predictedArmed = next;
        predictionExpiresAt = now + PREDICTION_WINDOW_MS;
    }

    private boolean isClickStateActive()
    {
        return resolveClickState(specArmed, predictedArmed, predictionExpiresAt, System.currentTimeMillis());
    }

    static boolean resolveClickState(boolean serverArmed, boolean predicted, long predictionExpiresAt, long now)
    {
        return now < predictionExpiresAt ? predicted : serverArmed;
    }

    public boolean isHovering()
    {
        return isHovering && config.enableHoverEffect();
    }

    public boolean isClicked()
    {
        return isClickStateActive() && config.enableClickEffect();
    }

    // Hover state getters (integrated color+opacity)
    public Color getHoverAreaColor()
    {
        return config.hoverAreaColor();
    }

    public Color getHoverBorderColor()
    {
        return config.hoverBorderColor();
    }

    // Click state getters (integrated color+opacity)
    public Color getClickAreaColor()
    {
        return config.clickAreaColor();
    }

    public Color getClickBorderColor()
    {
        return config.clickBorderColor();
    }

    // Idle state getters
    public boolean isIdleBorderEnabled()
    {
        return config.enableIdleBorder();
    }

    public Color getIdleAreaColor()
    {
        return config.idleAreaColor();
    }

    public Color getIdleBorderColor()
    {
        return config.idleBorderColor();
    }

    // Color preset getter
    public SpecBarConfig.ColorPreset getColorPreset()
    {
        return config.colorPreset();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!event.getGroup().equals("specbar"))
        {
            return;
        }

        // Check if the color preset was changed
        if (event.getKey().equals("colorPreset"))
        {
            SpecBarConfig.ColorPreset newPreset = SpecBarConfig.ColorPreset.valueOf(event.getNewValue());
            loadPresetToUI(newPreset);
        }
    }

    private enum ColorType
    {
        HOVER_AREA, HOVER_BORDER,
        CLICK_AREA, CLICK_BORDER,
        IDLE_AREA, IDLE_BORDER
    }

    private Color getPresetColor(SpecBarConfig.ColorPreset preset, ColorType colorType)
    {
        if (preset == SpecBarConfig.ColorPreset.CUSTOM)
        {
            // Return default colors for custom preset
            switch (colorType)
            {
                case HOVER_AREA: return new Color(135, 206, 250, 60);
                case HOVER_BORDER: return new Color(30, 144, 255, 150);
                case CLICK_AREA: return new Color(255, 69, 0, 80);
                case CLICK_BORDER: return new Color(220, 20, 60, 180);
                case IDLE_AREA: return new Color(128, 128, 128, 30);
                case IDLE_BORDER: return new Color(128, 128, 128, 100);
                default: return new Color(128, 128, 128, 100);
            }
        }

        // Return preset-specific colors
        switch (preset)
        {
            case CUSTOM:
                // Already handled above, but include for completeness
                return getPresetColor(SpecBarConfig.ColorPreset.CUSTOM, colorType);
            case CLASSIC_BLUE:
                switch (colorType)
                {
                    case HOVER_AREA: return new Color(100, 149, 237, 120);
                    case HOVER_BORDER: return new Color(65, 105, 225, 220);
                    case CLICK_AREA: return new Color(0, 100, 200, 160);
                    case CLICK_BORDER: return new Color(0, 50, 150, 255);
                    case IDLE_AREA: return new Color(173, 216, 230, 25);
                    case IDLE_BORDER: return new Color(135, 206, 250, 80);
                }
                break;
            case WARM_ORANGE:
                switch (colorType)
                {
                    case HOVER_AREA: return new Color(255, 165, 0, 120);
                    case HOVER_BORDER: return new Color(255, 140, 0, 220);
                    case CLICK_AREA: return new Color(220, 20, 60, 160);
                    case CLICK_BORDER: return new Color(180, 0, 0, 255);
                    case IDLE_AREA: return new Color(255, 218, 185, 25);
                    case IDLE_BORDER: return new Color(255, 160, 122, 80);
                }
                break;
            case NATURE_GREEN:
                switch (colorType)
                {
                    case HOVER_AREA: return new Color(144, 238, 144, 120);
                    case HOVER_BORDER: return new Color(34, 139, 34, 220);
                    case CLICK_AREA: return new Color(0, 128, 0, 160);
                    case CLICK_BORDER: return new Color(0, 100, 0, 255);
                    case IDLE_AREA: return new Color(240, 255, 240, 25);
                    case IDLE_BORDER: return new Color(152, 251, 152, 80);
                }
                break;
            case ROYAL_PURPLE:
                switch (colorType)
                {
                    case HOVER_AREA: return new Color(147, 112, 219, 120);
                    case HOVER_BORDER: return new Color(138, 43, 226, 220);
                    case CLICK_AREA: return new Color(128, 0, 128, 160);
                    case CLICK_BORDER: return new Color(75, 0, 130, 255);
                    case IDLE_AREA: return new Color(221, 160, 221, 25);
                    case IDLE_BORDER: return new Color(186, 85, 211, 80);
                }
                break;
            case DARK_THEME:
                switch (colorType)
                {
                    case HOVER_AREA: return new Color(96, 96, 96, 120);
                    case HOVER_BORDER: return new Color(160, 160, 160, 220);
                    case CLICK_AREA: return new Color(32, 32, 32, 160);
                    case CLICK_BORDER: return new Color(220, 220, 220, 255);
                    case IDLE_AREA: return new Color(48, 48, 48, 25);
                    case IDLE_BORDER: return new Color(80, 80, 80, 80);
                }
                break;
        }
        
        // Fallback to custom colors
        return getPresetColor(SpecBarConfig.ColorPreset.CUSTOM, colorType);
    }

    // Backup storage for custom settings
    private Color customHoverArea = null;
    private Color customHoverBorder = null;
    private Color customClickArea = null;
    private Color customClickBorder = null;
    private Color customIdleArea = null;
    private Color customIdleBorder = null;
    private SpecBarConfig.ColorPreset lastPreset = SpecBarConfig.ColorPreset.CUSTOM;

    /**
     * Loads preset values into the UI fields while preserving custom settings.
     * This allows users to switch between presets and custom without losing their custom values.
     */
    private void loadPresetToUI(SpecBarConfig.ColorPreset preset)
    {
        // If switching from CUSTOM to a preset, backup the current custom values
        if (lastPreset == SpecBarConfig.ColorPreset.CUSTOM && preset != SpecBarConfig.ColorPreset.CUSTOM)
        {
            backupCustomSettings();
        }
        
        // If switching to CUSTOM from a preset, restore the backed up custom values
        if (preset == SpecBarConfig.ColorPreset.CUSTOM && lastPreset != SpecBarConfig.ColorPreset.CUSTOM)
        {
            restoreCustomSettings();
            lastPreset = preset;
            return;
        }
        
        // If switching between presets (not involving CUSTOM), just load the new preset
        if (preset != SpecBarConfig.ColorPreset.CUSTOM)
        {
            loadPresetValues(preset);
        }
        
        lastPreset = preset;
    }
    
    /**
     * Backs up the current custom color settings before switching to a preset
     */
    private void backupCustomSettings()
    {
        customHoverArea = config.hoverAreaColor();
        customHoverBorder = config.hoverBorderColor();
        customClickArea = config.clickAreaColor();
        customClickBorder = config.clickBorderColor();
        customIdleArea = config.idleAreaColor();
        customIdleBorder = config.idleBorderColor();
        
        log.debug("Backed up custom settings before switching to preset");
    }
    
    /**
     * Restores the previously backed up custom settings
     */
    private void restoreCustomSettings()
    {
        if (customHoverArea != null)
        {
            configManager.setConfiguration("specbar", "hoverAreaColor", customHoverArea);
            configManager.setConfiguration("specbar", "hoverBorderColor", customHoverBorder);
            configManager.setConfiguration("specbar", "clickAreaColor", customClickArea);
            configManager.setConfiguration("specbar", "clickBorderColor", customClickBorder);
            configManager.setConfiguration("specbar", "idleAreaColor", customIdleArea);
            configManager.setConfiguration("specbar", "idleBorderColor", customIdleBorder);
            
            log.debug("Restored custom settings");
        }
    }
    
    /**
     * Loads preset values into the UI configuration
     */
    private void loadPresetValues(SpecBarConfig.ColorPreset preset)
    {
        Color hoverArea = getPresetColor(preset, ColorType.HOVER_AREA);
        Color hoverBorder = getPresetColor(preset, ColorType.HOVER_BORDER);
        Color clickArea = getPresetColor(preset, ColorType.CLICK_AREA);
        Color clickBorder = getPresetColor(preset, ColorType.CLICK_BORDER);
        Color idleArea = getPresetColor(preset, ColorType.IDLE_AREA);
        Color idleBorder = getPresetColor(preset, ColorType.IDLE_BORDER);

        configManager.setConfiguration("specbar", "hoverAreaColor", hoverArea);
        configManager.setConfiguration("specbar", "hoverBorderColor", hoverBorder);
        configManager.setConfiguration("specbar", "clickAreaColor", clickArea);
        configManager.setConfiguration("specbar", "clickBorderColor", clickBorder);
        configManager.setConfiguration("specbar", "idleAreaColor", idleArea);
        configManager.setConfiguration("specbar", "idleBorderColor", idleBorder);
        
        log.debug("Loaded preset values for: {}", preset);
    }

    @Provides
    SpecBarConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(SpecBarConfig.class);
    }
}
