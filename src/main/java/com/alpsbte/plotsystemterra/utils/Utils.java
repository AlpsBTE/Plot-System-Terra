package com.alpsbte.plotsystemterra.utils;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

public class Utils {

    // Head Database API
    public static HeadDatabaseAPI headDatabaseAPI;

    /**
     * Prefix used for all command permissions.
     */
    public static final String permissionPrefix = "plotsystem";

    public static ItemStack getItemHead(String headID) {
        return headDatabaseAPI != null ? headDatabaseAPI.getItemHead(headID) : new ItemBuilder(Material.SKULL_ITEM, 1, (byte) 3).build();
    }

    // Player Messages
    private static final String messagePrefix =  PlotSystemTerra.getPlugin().getConfig().getString("message-prefix") + " ";

    public static String getInfoMessageFormat(String info) {
        return messagePrefix + PlotSystemTerra.getPlugin().getConfig().getString("info-prefix") + info;
    }

    public static String getErrorMessageFormat(String error) {
        return messagePrefix + PlotSystemTerra.getPlugin().getConfig().getString("error-prefix") + error;
    }

    public static boolean hasPermission(CommandSender sender, String permissionNode) {
        return sender.hasPermission(permissionPrefix + permissionNode);
    }
}
