package com.alpsbte.plotsystemterra.core.plotsystem;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.DatabaseConnection;
import com.alpsbte.plotsystemterra.core.config.ConfigPaths;
import com.alpsbte.plotsystemterra.utils.FTPManager;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.schematic.SchematicFormat;
import com.sk89q.worldedit.world.DataException;
import org.apache.commons.vfs2.FileSystemException;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PlotPaster extends Thread {

    private final String serverName;

    private final int pasteInterval;
    private final World world;
    private final boolean broadcastMessages;

    public PlotPaster() {
        FileConfiguration config = PlotSystemTerra.getPlugin().getConfig();
        this.serverName = config.getString(ConfigPaths.SERVER_NAME);
        this.world = Bukkit.getWorld(config.getString(ConfigPaths.WORLD_NAME));
        this.pasteInterval = config.getInt(ConfigPaths.PASTING_INTERVAL);
        this.broadcastMessages = config.getBoolean(ConfigPaths.BROADCAST_INFO);
    }

    @Override
    public void run() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(PlotSystemTerra.getPlugin(), () -> {
            try {
                int pastedPlots = 0;

                ResultSet rs = DatabaseConnection.createStatement("SELECT id, city_project_id, mc_coordinates FROM plotsystem_plots WHERE status = 'completed' AND pasted = '0' LIMIT 20")
                        .executeQuery();

                if (rs.isBeforeFirst()) {
                    while (rs.next()) {
                        int plotID = -1;
                        try {
                            plotID = rs.getInt(1);
                            CityProject city = new CityProject(rs.getInt(2));

                            ResultSet rsServer = DatabaseConnection.createStatement("SELECT name FROM plotsystem_servers WHERE id = ?")
                                    .setValue(city.getServerID()).executeQuery();
                            if (rsServer.next()) {
                                String name = rsServer.getString(1);
                                if (name.equals(serverName)) {
                                    String[] splitCoordinates = rs.getString(3).split(",");

                                    Vector mcCoordinates = Vector.toBlockPoint(
                                            Float.parseFloat(splitCoordinates[0]),
                                            Float.parseFloat(splitCoordinates[1]),
                                            Float.parseFloat(splitCoordinates[2])
                                    );

                                    pastePlotSchematic(plotID, city, world, mcCoordinates);
                                    pastedPlots++;
                                }
                            }
                        } catch (Exception ex) {
                            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while pasting plot #" + plotID + "!", ex);
                        }
                    }

                    if (broadcastMessages && pastedPlots != 0) {
                        Bukkit.broadcastMessage("§7§l>§a Pasted §6" + pastedPlots + " §aplot" + (pastedPlots > 1 ? "s" : "") + "!");
                    }
                }
            } catch (SQLException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "A SQL error occurred!", ex);
            }
        }, 0L, 20L * pasteInterval);
    }

    public static void pastePlotSchematic(int plotID, CityProject city, World world, Vector mcCoordinates) throws IOException, DataException, MaxChangedBlocksException, SQLException {
        File file = Paths.get(PlotCreator.schematicsPath, "finishedSchematics", String.valueOf(city.getID()), plotID + ".schematic").toFile();

        // Download from SFTP or FTP server if enabled
        FTPConfiguration ftpConfiguration = city.getFTPConfiguration();
        if (ftpConfiguration != null) {
            Files.deleteIfExists(file.toPath());
            if (CompletableFuture.supplyAsync(() -> {
                try {
                    return FTPManager.downloadSchematic(FTPManager.getFTPUrl(ftpConfiguration, city.getID()), file);
                } catch (FileSystemException ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "An error occurred while downloading schematic file from SFTP/FTP server!", ex);
                    return null;
                }
            }).join() == null) throw new FileSystemException("Schematic file download failed!");
        }

        if (file.exists()) {
            EditSession editSession = new EditSession(new BukkitWorld(world), -1);
            editSession.enableQueue();

            SchematicFormat schematicFormat = SchematicFormat.getFormat(file);
            CuboidClipboard clipboard = schematicFormat.load(file);

            clipboard.paste(editSession, mcCoordinates, true);
            editSession.flushQueue();

            DatabaseConnection.createStatement("UPDATE plotsystem_plots SET pasted = '1' WHERE id = ?")
                    .setValue(plotID).executeUpdate();
        } else {
            Bukkit.getLogger().log(Level.WARNING, "Could not find finished schematic file of plot #" + plotID + "!");
        }
    }
}
