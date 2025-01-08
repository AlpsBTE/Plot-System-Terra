package com.alpsbte.plotsystemterra.core.model;

import com.alpsbte.alpslib.utils.item.ItemBuilder;
import com.alpsbte.alpslib.utils.item.LoreBuilder;
import com.alpsbte.plotsystemterra.utils.Utils;
import org.bukkit.inventory.ItemStack;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.AQUA;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class CityProject {
    private final String id;
    private final String countryCode;
    private final boolean isVisible;
    private final String material;
    private final String customModelData;

    public CityProject(String id, String countryCode, boolean isVisible, String material, String customModelData) {
        this.id = id;
        this.countryCode = countryCode;
        this.isVisible = isVisible;
        this.material = material;
        this.customModelData = customModelData;
    }

    public String getId() {
        return id;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public String getMaterial() {
        return material;
    }

    public String getCustomModelData() {
        return customModelData;
    }

    public ItemStack getItem() {
        return new ItemBuilder(Utils.getConfiguredItem(material, customModelData))
                .setName(text(id, AQUA, BOLD)) //TODO: get name from language system
                .setLore(new LoreBuilder()
                        .addLine(text("ID: ", AQUA).append(text(getId(), GRAY)))
                        .build())
                .build();
    }
}
