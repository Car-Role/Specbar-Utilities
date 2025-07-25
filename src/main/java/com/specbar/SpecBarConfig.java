package com.specbar;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.awt.Color;

@ConfigGroup("specbar")
public interface SpecBarConfig extends Config
{
    @ConfigSection(
        name = "Color Presets",
        description = "Pre-configured color themes for all states",
        position = 0
    )
    String colorPresetsSection = "colorPresets";

    @ConfigSection(
        name = "Visual Effects",
        description = "Configure the visual feedback effects",
        position = 1
    )
    String visualEffectsSection = "visualEffects";

    @ConfigSection(
        name = "Hover Colors",
        description = "Customize the colors for hover state",
        position = 2
    )
    String hoverColorsSection = "hoverColors";

    @ConfigSection(
        name = "Click Colors",
        description = "Customize colors for click state",
        position = 2
    )
    String clickColorsSection = "clickColors";

    @ConfigSection(
        name = "Idle State",
        description = "Persistent border when not hovering or clicking",
        position = 4
    )
    String idleStateSection = "idleState";

    @ConfigItem(
        keyName = "colorPreset",
        name = "Color Preset",
        description = "Choose from pre-configured color themes for all states",
        section = colorPresetsSection,
        position = 0
    )
    default ColorPreset colorPreset()
    {
        return ColorPreset.CUSTOM;
    }

    @ConfigItem(
        keyName = "enableHoverEffect",
        name = "Enable Hover Effect",
        description = "Show visual feedback when hovering over the special attack bar",
        section = visualEffectsSection,
        position = 0
    )
    default boolean enableHoverEffect()
    {
        return true;
    }

    @ConfigItem(
        keyName = "enableClickEffect",
        name = "Enable Click Effect",
        description = "Show visual feedback when clicking the special attack bar",
        section = visualEffectsSection,
        position = 1
    )
    default boolean enableClickEffect()
    {
        return true;
    }

    @ConfigItem(
        keyName = "enableIdleBorder",
        name = "Enable Idle Border",
        description = "Show a persistent border around the special attack bar when not interacting",
        section = visualEffectsSection,
        position = 2
    )
    default boolean enableIdleBorder()
    {
        return false;
    }

    @ConfigItem(
        keyName = "hideSpecBarText",
        name = "Hide Spec Bar Text",
        description = "Hide the percentage text on the special attack bar for a cleaner look",
        section = visualEffectsSection,
        position = 3
    )
    default boolean hideSpecBarText()
    {
        return false;
    }

    // Hover State Settings
    @Alpha
    @ConfigItem(
        keyName = "hoverAreaColor",
        name = "Hover Area Color",
        description = "Color and opacity for the hover area fill of the special attack bar",
        section = hoverColorsSection,
        position = 0
    )
    default Color hoverAreaColor()
    {
        return new Color(135, 206, 250, 60); // Light sky blue with transparency
    }

    @Alpha
    @ConfigItem(
        keyName = "hoverBorderColor",
        name = "Hover Border Color",
        description = "Color and opacity for the border when hovering over the special attack bar",
        section = hoverColorsSection,
        position = 1
    )
    default Color hoverBorderColor()
    {
        return new Color(30, 144, 255, 180); // Dodger blue with higher opacity
    }

    // Click State Settings
    @Alpha
    @ConfigItem(
        keyName = "clickAreaColor",
        name = "Click Area Color",
        description = "Color and opacity for the area fill when clicking the special attack bar",
        section = clickColorsSection,
        position = 0
    )
    default Color clickAreaColor()
    {
        return new Color(255, 165, 0, 100); // Orange with transparency for high contrast
    }

    @Alpha
    @ConfigItem(
        keyName = "clickBorderColor",
        name = "Click Border Color",
        description = "Color and opacity for the border when clicking the special attack bar",
        section = clickColorsSection,
        position = 1
    )
    default Color clickBorderColor()
    {
        return new Color(255, 69, 0, 220); // Red-orange with high opacity for visibility
    }

    // Idle State Settings
    @Alpha
    @ConfigItem(
        keyName = "idleAreaColor",
        name = "Idle Area Color",
        description = "Color and opacity for the idle area fill of the special attack bar",
        section = idleStateSection,
        position = 0
    )
    default Color idleAreaColor()
    {
        return new Color(128, 128, 128, 30); // Very subtle gray fill
    }

    @Alpha
    @ConfigItem(
        keyName = "idleBorderColor",
        name = "Idle Border Color",
        description = "Color and opacity for the idle border around the special attack bar",
        section = idleStateSection,
        position = 1
    )
    default Color idleBorderColor()
    {
        return new Color(128, 128, 128, 100); // Light gray with low opacity for subtle indication
    }



    enum ColorPreset
    {
        CUSTOM("Custom"),
        CLASSIC_BLUE("Classic Blue"),
        WARM_ORANGE("Warm Orange"),
        NATURE_GREEN("Nature Green"),
        ROYAL_PURPLE("Royal Purple"),
        DARK_THEME("Dark Theme");

        private final String displayName;

        ColorPreset(String displayName)
        {
            this.displayName = displayName;
        }

        @Override
        public String toString()
        {
            return displayName;
        }
    }
}
