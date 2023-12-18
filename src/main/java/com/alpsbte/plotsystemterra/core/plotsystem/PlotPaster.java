package com.alpsbte.plotsystemterra.core.plotsystem;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.Connection;
import com.alpsbte.plotsystemterra.core.config.ConfigPaths;
import com.alpsbte.plotsystemterra.utils.FTPManager;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.List;
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
            Connection connection = PlotSystemTerra.getPlugin().getConnection();
            try {

                List<Plot> pastePlots = connection.getCompletedAndUnpastedPlots();
                int pastedPlots = 0;

                for (Plot plot : pastePlots){
                        
                    int plotID = plot.id;
                    try{
                        CityProject city = connection.getCityProject(plot.city_project_id);
                        int serverID = connection.getServerID(city);
                        String cityServerName = connection.getServer(serverID).name;

                            
                        if (cityServerName.equals(serverName)) {

                            pastePlotSchematic(plotID, city, world, plot.mc_coordinates, plot.version, fastMode);
                            pastedPlots++;
                        }//endif city is on this server
                        
                    } catch (Exception ex) {
                        Bukkit.getLogger().log(Level.SEVERE, "An error occurred while pasting plot #" + plotID + "!", ex);
                    }

                }//endfor plots

                if (broadcastMessages && pastedPlots != 0) {
                    Bukkit.broadcastMessage("§7§l>§a Pasted §6" + pastedPlots + " §aplot" + (pastedPlots > 1 ? "s" : "") + "!");
                }

        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while getting unpasted plots!", ex);
        }
        }, 0L, 20L * pasteInterval);
    }

    public static void pastePlotSchematic(int plotID, CityProject city, World world, Vector mcCoordinates, double plotVersion, boolean fastMode) throws IOException, WorldEditException, URISyntaxException {
        Connection connection = PlotSystemTerra.getPlugin().getConnection();
        try {

            int serverID = connection.getServerID(city);
            File outlineSchematic = Paths.get(PlotCreator.schematicsPath, String.valueOf(serverID), String.valueOf(city.id), plotID + ".schematic").toFile();
            File completedSchematic = Paths.get(PlotCreator.schematicsPath, String.valueOf(serverID), "finishedSchematics", String.valueOf(city.id), plotID + ".schematic").toFile();

            // Download from SFTP or FTP server if enabled
            FTPConfiguration ftpConfiguration = connection.getFTPConfiguration(city);
            if (ftpConfiguration != null) {
                Files.deleteIfExists(completedSchematic.toPath());
                FTPManager.downloadSchematic(FTPManager.getFTPUrl(ftpConfiguration, city.id), completedSchematic);
            }

            if (outlineSchematic.exists() && completedSchematic.exists()) {
                com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);
                EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(weWorld, -1);
                if (fastMode) editSession.setFastMode(true);
                editSession.enableQueue();

                Clipboard outlineClipboard = ClipboardFormat.SCHEMATIC.getReader(Files.newInputStream(outlineSchematic.toPath())).read(weWorld.getWorldData());
                Clipboard completedClipboard = ClipboardFormat.SCHEMATIC.getReader(Files.newInputStream(completedSchematic.toPath())).read(weWorld.getWorldData());

                Vector toPaste;
                if (plotVersion >= 3) {
                    Vector plotOriginOutline = outlineClipboard.getOrigin();
                    toPaste = new Vector(plotOriginOutline.getX(), plotOriginOutline.getY(), plotOriginOutline.getZ());
                } else toPaste = mcCoordinates;

                Operation operation = new ClipboardHolder(completedClipboard, weWorld.getWorldData()).createPaste(editSession, weWorld.getWorldData())
                        .to(toPaste).ignoreAirBlocks(true).build();
                Operations.complete(operation);
                editSession.flushQueue();


                connection.setPlotPasted(plotID);
            } else {
                Bukkit.getLogger().log(Level.WARNING, "Could not find schematic file(s) of plot #" + plotID + "!");
            }
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.WARNING, "Connection exception during pasting of plot #" + plotID + "!");
        }
    }
}
