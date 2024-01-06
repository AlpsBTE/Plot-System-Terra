package com.alpsbte.plotsystemterra.utils;

import com.alpsbte.alpslib.utils.AlpsUtils;
import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.config.ConfigPaths;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDateTime;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class Utils {
    /**
     * Prefix used for all command permissions.
     */
    public static final String permissionPrefix = "plotsystem";

    public static class ChatUtils {
        public static void setChatFormat(String infoPrefix, String alertPrefix) {
            ChatUtils.infoPrefix = AlpsUtils.deserialize(infoPrefix);
            ChatUtils.alertPrefix = AlpsUtils.deserialize(alertPrefix);
        }

        private static Component infoPrefix;
        private static Component alertPrefix;

        public static Component getInfoFormat(Component infoComponent) {
            return infoPrefix.append(infoComponent).color(GREEN);
        }

        public static Component getAlertFormat(Component alertComponent) {
            return alertPrefix.append(alertComponent).color(RED);
        }
    }

    public static boolean hasPermission(CommandSender sender, String permissionNode) {
        return sender.hasPermission(permissionPrefix + "." + permissionNode);
    }
}
