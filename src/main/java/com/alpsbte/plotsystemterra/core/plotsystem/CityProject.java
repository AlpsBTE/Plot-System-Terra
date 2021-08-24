package com.alpsbte.plotsystemterra.core.plotsystem;

import com.alpsbte.plotsystemterra.core.DatabaseConnection;
import com.alpsbte.plotsystemterra.utils.ItemBuilder;
import com.alpsbte.plotsystemterra.utils.LoreBuilder;
import com.alpsbte.plotsystemterra.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.util.logging.Level;

public class CityProject {

    private final int ID;
    private final String name;
    private int headID;

    public CityProject(int ID, int countryID, String name) {
        this.ID = ID;
        this.name = name;

        try {
            ResultSet rs = DatabaseConnection.createStatement("SELECT head_id FROM plotsystem_countries WHERE id = ?")
                    .setValue(countryID).executeQuery();

            if (rs.next()) {
                this.headID = Integer.parseInt(rs.getString(1));
            }
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while getting country head!", ex);
        }
    }

    public int getID() {
        return ID;
    }

    public String getName() {
        return name;
    }

    public ItemStack getItem() {
        return new ItemBuilder(Utils.getItemHead(String.valueOf(headID)))
                .setName("§b§l" + name)
                .setLore(new LoreBuilder()
                        .addLine("§bID: §7" + getID())
                        .build())
                .build();
    }
}
