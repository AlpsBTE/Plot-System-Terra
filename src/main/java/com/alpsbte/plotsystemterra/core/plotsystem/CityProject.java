package com.alpsbte.plotsystemterra.core.plotsystem;

import com.alpsbte.plotsystemterra.core.DatabaseConnection;
import com.alpsbte.plotsystemterra.utils.ItemBuilder;
import com.alpsbte.plotsystemterra.utils.LoreBuilder;
import com.alpsbte.plotsystemterra.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

public class CityProject {

    private final int ID;
    private String name;
    private int countryID;
    private int headID;

    public CityProject(int ID) {
        this.ID = ID;

        try {
            ResultSet rs;
            rs = DatabaseConnection.createStatement("SELECT country_id, name FROM plotsystem_city_projects WHERE id = ?")
                    .setValue(ID).executeQuery();

            if (rs.next()) {
                this.countryID = rs.getInt(1);
                this.name = rs.getString(2);
            }

            rs = DatabaseConnection.createStatement("SELECT head_id FROM plotsystem_countries WHERE id = ?")
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

    public FTPConfiguration getFTPConfiguration() {
        try {
            ResultSet rsServer = DatabaseConnection.createStatement("SELECT server_id FROM plotsystem_countries WHERE id = ?")
                    .setValue(countryID).executeQuery();

            if (rsServer.next()) {
                ResultSet rsFTP = DatabaseConnection.createStatement("SELECT ftp_configuration_id FROM plotsystem_servers WHERE id = ?")
                        .setValue(rsServer.getInt(1)).executeQuery();

                if (rsFTP.next()) {
                    int ftpID = rsFTP.getInt(1);
                    if (!rsFTP.wasNull()) return new FTPConfiguration(ftpID);
                }
            }
        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not check for FTP-Configuration for city project (" + ID + ")!", ex);
        }
        return null;
    }

    public int getServerID() throws SQLException {
        ResultSet rs = DatabaseConnection.createStatement("SELECT server_id FROM plotsystem_countries WHERE id = ?")
                .setValue(this.countryID).executeQuery();

        if (rs.next()) {
            return rs.getInt(1);
        }
        return 1;
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
