package com.alpsbte.plotsystemterra.utils;

import com.alpsbte.alpslib.utils.AlpsUtils;
import com.alpsbte.alpslib.utils.head.AlpsHeadUtils;
import com.alpsbte.alpslib.utils.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import static net.kyori.adventure.text.format.NamedTextColor.*;

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

    public static ItemStack getConfiguredItem(String material, String customModelData) {
        ItemStack base;
        if (material.startsWith("head(") && material.endsWith(")")) {
            String headId = material.substring(material.indexOf("(") + 1, material.lastIndexOf(")"));
            base = AlpsHeadUtils.getCustomHead(headId);
        } else {
            Material mat = Material.getMaterial(material);
            base = new ItemStack(mat == null ? Material.BARRIER : mat);
        }
        ItemBuilder builder = new ItemBuilder(base);
        if (customModelData != null) builder.setItemModel(customModelData);

        return builder.build();
    }
}
