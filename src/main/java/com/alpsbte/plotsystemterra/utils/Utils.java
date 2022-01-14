package com.alpsbte.plotsystemterra.utils;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.config.ConfigPaths;
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
        return headDatabaseAPI != null && headID != null ? headDatabaseAPI.getItemHead(headID) : new ItemBuilder(Material.PLAYER_HEAD, 1).build();
    }

    // Player Messages
    private static final String messagePrefix =  PlotSystemTerra.getPlugin().getConfig().getString(ConfigPaths.MESSAGE_PREFIX) + " ";

    public static String getInfoMessageFormat(String info) {
        return messagePrefix + PlotSystemTerra.getPlugin().getConfig().getString(ConfigPaths.MESSAGE_INFO_COLOUR) + info;
    }

    public static String getErrorMessageFormat(String error) {
        return messagePrefix + PlotSystemTerra.getPlugin().getConfig().getString(ConfigPaths.MESSAGE_ERROR_COLOUR) + error;
    }

    public static boolean hasPermission(CommandSender sender, String permissionNode) {
        return sender.hasPermission(permissionPrefix + "." + permissionNode);
    }
}
