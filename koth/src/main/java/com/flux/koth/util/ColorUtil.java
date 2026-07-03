package com.flux.koth.util;

import org.bukkit.ChatColor;

public class ColorUtil {

    public static String color(String input) {
        if (input == null) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
