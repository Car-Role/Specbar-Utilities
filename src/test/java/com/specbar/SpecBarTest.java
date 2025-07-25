package com.specbar;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import com.specbar.SpecBarPlugin;

public class SpecBarTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(SpecBarPlugin.class);
        RuneLite.main(args);
    }
}
