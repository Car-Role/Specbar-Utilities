package com.specbar;

import org.junit.Test;
import java.awt.Color;

import static org.junit.Assert.*;

/**
 * Test class for SpecBarPlugin - compatible with IntelliJ RuneLite project
 * These tests verify basic functionality without requiring RuneLite runtime dependencies
 */
public class SpecBarPluginTest
{
    @Test
    public void testPluginCreation()
    {
        // Simple test to verify the plugin can be instantiated
        SpecBarPlugin plugin = new SpecBarPlugin();
        assertNotNull("Plugin should be created successfully", plugin);
    }

    @Test
    public void testConfigDefaults()
    {
        // Test that config interface has proper default values
        SpecBarConfig config = new SpecBarConfig() {};
        
        assertTrue("Hover effect should be enabled by default", config.enableHoverEffect());
        assertTrue("Click effect should be enabled by default", config.enableClickEffect());
        
        // Test hover settings (integrated color+opacity)
        assertEquals(new Color(135, 206, 250, 60), config.hoverAreaColor());
        assertEquals(new Color(30, 144, 255, 180), config.hoverBorderColor());
        
        // Test click settings (integrated color+opacity)
        assertEquals(new Color(255, 165, 0, 100), config.clickAreaColor());
        assertEquals(new Color(255, 69, 0, 220), config.clickBorderColor());
        
        // Test effect settings
        assertTrue(config.enableHoverEffect());
        assertTrue(config.enableClickEffect());
        assertFalse(config.hideSpecBarText()); // Should be disabled by default
        assertFalse(config.enableIdleBorder()); // Should be disabled by default
        
        // Test idle state settings
        assertEquals(new Color(128, 128, 128, 30), config.idleAreaColor());
        assertEquals(new Color(128, 128, 128, 100), config.idleBorderColor());
        
        // Test color preset setting
        assertEquals(SpecBarConfig.ColorPreset.CUSTOM, config.colorPreset());
    }

    @Test
    public void testOverlayClassExists()
    {
        // Simple test to verify overlay class structure exists
        try {
            Class<?> overlayClass = Class.forName("com.specbar.SpecBarOverlay");
            assertNotNull("SpecBarOverlay class should exist", overlayClass);
        } catch (ClassNotFoundException e) {
            fail("SpecBarOverlay class should be found and loadable");
        }
    }

    @Test
    public void testPluginDescriptor()
    {
        // Verify the plugin has the required RuneLite plugin descriptor
        Class<SpecBarPlugin> pluginClass = SpecBarPlugin.class;
        assertTrue("Plugin should have PluginDescriptor annotation",
                pluginClass.isAnnotationPresent(net.runelite.client.plugins.PluginDescriptor.class));
        
        net.runelite.client.plugins.PluginDescriptor descriptor = 
                pluginClass.getAnnotation(net.runelite.client.plugins.PluginDescriptor.class);
        
        assertEquals("Plugin name should match", "Spec Bar Utilities", descriptor.name());
        assertFalse("Plugin should have a description", descriptor.description().isEmpty());
        assertTrue("Plugin should have tags", descriptor.tags().length > 0);
    }
}
