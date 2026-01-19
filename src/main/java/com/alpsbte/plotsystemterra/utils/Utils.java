package com.alpsbte.plotsystemterra.utils;

import com.alpsbte.alpslib.utils.AlpsUtils;
import com.alpsbte.alpslib.utils.head.AlpsHeadUtils;
import com.alpsbte.alpslib.utils.item.ItemBuilder;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import static net.kyori.adventure.text.format.NamedTextColor.*;

@UtilityClass
public class Utils {
    @UtilityClass
    public static class ChatUtils {
        private static Component infoPrefix;
        private static Component alertPrefix;

        public static void setChatFormat(String infoPrefix, String alertPrefix) {
            ChatUtils.infoPrefix = AlpsUtils.deserialize(infoPrefix);
            ChatUtils.alertPrefix = AlpsUtils.deserialize(alertPrefix);
        }

        public static @NonNull Component getInfoFormat(@NonNull Component infoComponent) {
            return infoPrefix.append(infoComponent.color(GREEN));
        }

        public static @NonNull Component getAlertFormat(@NonNull Component alertComponent) {
            return alertPrefix.append(alertComponent.color(RED));
        }
    }

    public static ItemStack getConfiguredItem(@NonNull String material, String customModelData) {
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
