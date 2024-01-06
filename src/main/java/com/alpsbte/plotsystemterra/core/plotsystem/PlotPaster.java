package com.alpsbte.plotsystemterra.core.plotsystem;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.DatabaseConnection;
import com.alpsbte.plotsystemterra.core.config.ConfigPaths;
import com.alpsbte.plotsystemterra.utils.FTPManager;
import com.fastasyncworldedit.core.FaweAPI;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

public class PlotPaster extends Thread {

    private final String serverName;

    public final boolean fastMode;
    private final int pasteInterval;
    public final World world;
    private final boolean broadcastMessages;

    public PlotPaster() {
        FileConfiguration config = PlotSystemTerra.getPlugin().getConfig();

        this.serverName = config.getString(ConfigPaths.SERVER_NAME);
        this.fastMode = config.getBoolean(ConfigPaths.FAST_MODE);
        this.world = Bukkit.getWorld(config.getString(ConfigPaths.WORLD_NAME));
        this.pasteInterval = config.getInt(ConfigPaths.PASTING_INTERVAL);
        this.broadcastMessages = config.getBoolean(ConfigPaths.BROADCAST_INFO);
    }

    @Override
    public void run() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(PlotSystemTerra.getPlugin(), () -> {
            try (ResultSet rs = DatabaseConnection.createStatement("SELECT id, city_project_id, mc_coordinates, version FROM plotsystem_plots WHERE status = 'completed' AND pasted = '0' LIMIT 20")
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

                                        BlockVector3 mcCoordinates = Vector3.toBlockPoint(
                                                Double.parseDouble(splitCoordinates[0]),
                                                Double.parseDouble(splitCoordinates[1]),
                                                Double.parseDouble(splitCoordinates[2])
                                        );

                                        double version = rs.getDouble(4);
                                        if (rs.wasNull()) { version = 2; }

                                        pastePlotSchematic(plotID, city, world, mcCoordinates, version , fastMode);
                                        pastedPlots++;
                                    }
                                }

                                DatabaseConnection.closeResultSet(rsServer);

                            }
                        } catch (Exception ex) {
                            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while pasting plot #" + plotID + "!", ex);
                        }
                    }

                    if (broadcastMessages && pastedPlots != 0) {
                        Bukkit.broadcastMessage("§7§l>§a Pasted §6" + pastedPlots + " §aplot" + (pastedPlots > 1 ? "s" : "") + "!");
                    }
                }

                DatabaseConnection.closeResultSet(rs);

            } catch (SQLException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "A SQL error occurred!", ex);
            }
        }, 0L, 20L * pasteInterval);
    }

    public static void pastePlotSchematic(int plotID, CityProject city, World world, BlockVector3 mcCoordinates, double plotVersion, boolean fastMode) throws IOException, WorldEditException, SQLException, URISyntaxException {
        File outlineSchematic = Paths.get(PlotCreator.schematicsPath, String.valueOf(city.getServerID()), String.valueOf(city.getID()), plotID + ".schem").toFile();
        if (!outlineSchematic.exists()) outlineSchematic = Paths.get(PlotCreator.schematicsPath, String.valueOf(city.getServerID()), String.valueOf(city.getID()), plotID + ".schematic").toFile();
        File completedSchematic = Paths.get(PlotCreator.schematicsPath, String.valueOf(city.getServerID()), "finishedSchematics", String.valueOf(city.getID()), plotID + ".schem").toFile();
        if (!completedSchematic.exists()) completedSchematic = Paths.get(PlotCreator.schematicsPath, String.valueOf(city.getServerID()), "finishedSchematics", String.valueOf(city.getID()), plotID + ".schematic").toFile();

        // Download from SFTP or FTP server if enabled
        FTPConfiguration ftpConfiguration = city.getFTPConfiguration();
        if (ftpConfiguration != null) {
            Files.deleteIfExists(completedSchematic.toPath());
            FTPManager.downloadSchematic(FTPManager.getFTPUrl(ftpConfiguration, city.getID()), completedSchematic);
        }

        if (outlineSchematic.exists() && completedSchematic.exists()) {
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
                BlockVector3 toPaste;
                if (plotVersion >= 3) {
                    BlockVector3 plotOriginOutline = FaweAPI.load(outlineSchematic).getOrigin();
                    toPaste = BlockVector3.at(plotOriginOutline.getX(), plotOriginOutline.getY(), plotOriginOutline.getZ());
                } else toPaste = mcCoordinates;

                Mask airMask = new BlockTypeMask(BukkitAdapter.adapt(world), BlockTypes.AIR);
                editSession.setMask(airMask);
                if (fastMode) editSession.setFastMode(true);
                Clipboard completedClipboard = FaweAPI.load(completedSchematic);

                Operation clipboardHolder = new ClipboardHolder(completedClipboard)
                        .createPaste(editSession)
                        .to(toPaste)
                        .build();
                Operations.complete(clipboardHolder);

                DatabaseConnection.createStatement("UPDATE plotsystem_plots SET pasted = '1' WHERE id = ?")
                        .setValue(plotID).executeUpdate();
            }
        } else {
            Bukkit.getLogger().log(Level.WARNING, "Could not find schematic file(s) of plot #" + plotID + "!");
        }
    }
}
