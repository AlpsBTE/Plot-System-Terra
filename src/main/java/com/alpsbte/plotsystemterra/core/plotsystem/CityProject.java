package com.alpsbte.plotsystemterra.core.plotsystem;

import com.alpsbte.alpslib.utils.head.AlpsHeadUtils;
import com.alpsbte.alpslib.utils.item.ItemBuilder;
import com.alpsbte.alpslib.utils.item.LoreBuilder;
import com.alpsbte.plotsystemterra.core.DatabaseConnection;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.AQUA;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class CityProject {

    private final int ID;
    private String name;
    private int countryID;
    private String headID;

    public CityProject(int ID) {
        this.ID = ID;

        try (ResultSet rsCity = DatabaseConnection.createStatement("SELECT country_id, name FROM plotsystem_city_projects WHERE id = ?")
                .setValue(ID).executeQuery()) {

            if (rsCity.next()) {
                this.countryID = rsCity.getInt(1);
                this.name = rsCity.getString(2);
            }

            DatabaseConnection.closeResultSet(rsCity);

            try (ResultSet rsCountry = DatabaseConnection.createStatement("SELECT head_id FROM plotsystem_countries WHERE id = ?")
                    .setValue(countryID).executeQuery();) {

                if (rsCountry.next()) {
                    this.headID = rsCountry.getString(1);
                }

                DatabaseConnection.closeResultSet(rsCountry);

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
        try (ResultSet rsServer = DatabaseConnection.createStatement("SELECT server_id FROM plotsystem_countries WHERE id = ?")
                .setValue(countryID).executeQuery()) {

            if (rsServer.next()) {
                try (ResultSet rsFTP = DatabaseConnection.createStatement("SELECT ftp_configuration_id FROM plotsystem_servers WHERE id = ?")
                        .setValue(rsServer.getInt(1)).executeQuery()) {

                    if (rsFTP.next()) {
                        int ftpID = rsFTP.getInt(1);
                        if (!rsFTP.wasNull()){
                            DatabaseConnection.closeResultSet(rsServer);
                            DatabaseConnection.closeResultSet(rsFTP);
                            return new FTPConfiguration(ftpID);
                        }
                    }

                    DatabaseConnection.closeResultSet(rsFTP);
                }
            }

            DatabaseConnection.closeResultSet(rsServer);

        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not check for FTP-Configuration for city project (" + ID + ")!", ex);
        }
        return null;
    }

    public int getServerID() throws SQLException {
        try (ResultSet rs = DatabaseConnection.createStatement("SELECT server_id FROM plotsystem_countries WHERE id = ?")
                .setValue(this.countryID).executeQuery()) {

            if (rs.next()) {
                int i = rs.getInt(1);
                DatabaseConnection.closeResultSet(rs);
                return i;
            }

            DatabaseConnection.closeResultSet(rs);

            return 1;
        }
    }

    public ItemStack getItem() {
        return new ItemBuilder(AlpsHeadUtils.getCustomHead(headID))
                .setName(text(name, AQUA, BOLD))
                .setLore(new LoreBuilder()
                        .addLine(text("ID: ", AQUA).append(text(getID(), GRAY)))
                        .build())
                .build();
    }
}
