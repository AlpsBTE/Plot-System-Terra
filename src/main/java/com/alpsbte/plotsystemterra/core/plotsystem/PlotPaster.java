package com.alpsbte.plotsystemterra.core.plotsystem;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.DatabaseConnection;
import com.alpsbte.plotsystemterra.core.config.ConfigPaths;
import com.alpsbte.plotsystemterra.utils.FTPManager;
import com.alpsbte.plotsystemterra.utils.Utils;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.DataException;
import org.apache.commons.vfs2.FileSystemException;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
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
        this.world = Bukkit.getWorld(Objects.requireNonNull(config.getString(ConfigPaths.WORLD_NAME)));
        this.pasteInterval = config.getInt(ConfigPaths.PASTING_INTERVAL);
        this.broadcastMessages = config.getBoolean(ConfigPaths.BROADCAST_INFO);
    }

    @Override
    public void run() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(PlotSystemTerra.getPlugin(), () -> {
            try (ResultSet rs = DatabaseConnection.createStatement("SELECT id, city_project_id, mc_coordinates FROM plotsystem_plots WHERE status = 'completed' AND pasted = '0' LIMIT 20")
                    .executeQuery()) {
                int pastedPlots = 0;

                if (rs.isBeforeFirst()) {
                    while (rs.next()) {
                        int plotID = -1;
                        try {
                            plotID = rs.getInt(1);
                            CityProject city = new CityProject(rs.getInt(2));

                            try (ResultSet rsServer = DatabaseConnection.createStatement("SELECT name FROM plotsystem_servers WHERE id = ?")
                                    .setValue(city.getServerID()).executeQuery()) {

                                if (rsServer.next()) {
                                    String name = rsServer.getString(1);
                                    if (name.equals(serverName)) {
                                        String[] splitCoordinates = rs.getString(3).split(",");

                                        BlockVector3 mcCoordinates = BlockVector3.at(
                                                Float.parseFloat(splitCoordinates[0]),
                                                Float.parseFloat(splitCoordinates[1]),
                                                Float.parseFloat(splitCoordinates[2])
                                        );

                                        pastePlotSchematic(plotID, city, world, mcCoordinates);
                                        pastedPlots++;
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while pasting plot #" + plotID + "!", ex);
                        }
                    }

                    if (broadcastMessages && pastedPlots != 0) {
                        Bukkit.broadcastMessage(Utils.getInfoMessageFormat("Pasted §6" + pastedPlots +
                            PlotSystemTerra.getPlugin().getConfig().getString(ConfigPaths.MESSAGE_INFO_COLOUR) +
                            " plot" + (pastedPlots > 1 ? "s" : "") + "!"));
                    }
                }
            } catch (SQLException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "A SQL error occurred!", ex);
            }
        }, 0L, 20L * pasteInterval);
    }

    public static void pastePlotSchematic(int plotID, CityProject city, World world, BlockVector3 mcCoordinates) throws IOException, DataException, MaxChangedBlocksException, SQLException {
        File file = Paths.get(PlotCreator.schematicsPath, String.valueOf(city.getServerID()), "finishedSchematics", String.valueOf(city.getID()), plotID + ".schematic").toFile();

        // Download from SFTP or FTP server if enabled
        FTPConfiguration ftpConfiguration = city.getFTPConfiguration();
        if (ftpConfiguration != null) {
            Files.deleteIfExists(file.toPath());
            if (CompletableFuture.supplyAsync(() -> {
                try {
                    return FTPManager.downloadSchematic(FTPManager.getFTPUrl(ftpConfiguration, city.getID()), file);
                } catch (FileSystemException | URISyntaxException ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "An error occurred while downloading schematic file from SFTP/FTP server!", ex);
                    return null;
                }
            }).join() == null) throw new IOException();
        }

        if (file.exists()) {
            try(EditSession editSession = WorldEdit.getInstance().newEditSession(new BukkitWorld(world))) {
                Clipboard clipboard;
                ClipboardFormat schematicFormat = ClipboardFormats.findByFile(file);
                try (ClipboardReader reader = schematicFormat.getReader(new FileInputStream(file)))
                {
                    clipboard = reader.read();
                }

                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(mcCoordinates)
                        .copyEntities(true)
                        .copyBiomes(true)
                        .build();
                Operations.complete(operation);

            } catch (WorldEditException e) {
                e.printStackTrace();
            }

            DatabaseConnection.createStatement("UPDATE plotsystem_plots SET pasted = '1' WHERE id = ?")
                    .setValue(plotID).executeUpdate();
        } else {
            Bukkit.getLogger().log(Level.WARNING, "Could not find finished schematic file of plot #" + plotID + "!");
        }
    }
}
