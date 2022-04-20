package com.alpsbte.plotsystemterra.core.plotsystem;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.DatabaseConnection;
import com.alpsbte.plotsystemterra.core.config.ConfigPaths;
import com.alpsbte.plotsystemterra.utils.FTPManager;
import com.alpsbte.plotsystemterra.utils.Utils;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.AbstractRegion;
import com.sk89q.worldedit.regions.CylinderRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PlotCreator {

    public final static String schematicsPath = Paths.get(PlotSystemTerra.getPlugin().getDataFolder().getAbsolutePath(), "schematics") + File.separator;
    private final static String errorCode = "error";

    public static CompletableFuture<Void> Create(Player player, CityProject cityProject, int difficultyID) {
        int plotID;
        Region plotRegion;

        // Read the config
        FileConfiguration config = PlotSystemTerra.getPlugin().getConfig();
        boolean environmentEnabled = config.getBoolean(ConfigPaths.ENVIRONMENT_ENABLED);
        int environmentRadius = config.getInt(ConfigPaths.ENVIRONMENT_RADIUS);


        // Get WorldEdit selection of player
        try {
            plotRegion = Objects.requireNonNull(WorldEdit.getInstance().getSessionManager().findByName(player.getName())).getSelection(
                    Objects.requireNonNull(WorldEdit.getInstance().getSessionManager().findByName(player.getName())).getSelectionWorld());
        } catch (NullPointerException | IncompleteRegionException ex) {
            player.sendMessage(Utils.getErrorMessageFormat("Please select a plot using WorldEdit!"));
            return CompletableFuture.completedFuture(null);
        }


        // Conversion
        Polygonal2DRegion plotPolyRegion = null;
        CylinderRegion environmentPolyRegion = null;
        try {
            // Check if WorldEdit selection is polygonal
            if (plotRegion instanceof Polygonal2DRegion) {
                // Cast WorldEdit region to polygonal region
                plotPolyRegion = (Polygonal2DRegion) plotRegion;

                if (plotPolyRegion.getLength() > 100 || plotPolyRegion.getWidth() > 100 || plotPolyRegion.getHeight() > 250) {
                    player.sendMessage(Utils.getErrorMessageFormat("Please adjust your selection size!"));
                    return CompletableFuture.completedFuture(null);
                }

                // Set minimum selection height under player location
                if (plotPolyRegion.getMinimumY() > player.getLocation().getY() - 5) {
                    plotPolyRegion.setMinimumY((int) player.getLocation().getY() - 5);
                }

                if (plotPolyRegion.getMaximumY() <= player.getLocation().getY() + 1) {
                    plotPolyRegion.setMaximumY((int) player.getLocation().getY() + 1);
                }


                // Create the environment selection
                if(environmentEnabled) {

                    //Create a new cylinder region with the size of the plot + the configured radius around it.
                    int width = plotPolyRegion.getWidth();
                    int length = plotPolyRegion.getLength();
                    Bukkit.getLogger().log(Level.INFO, "Plot width: " + width + " length: " + length + " radius: " + environmentRadius + " minY: " + plotPolyRegion.getMinimumY() + " maxY: " + plotPolyRegion.getMaximumY());
                    Vector2D radius = new Vector2D(width/2 + environmentRadius, length/2 + environmentRadius);

                    environmentPolyRegion = new CylinderRegion(
                            plotPolyRegion.getWorld(),
                            plotPolyRegion.getCenter(),
                            radius, plotPolyRegion.getMinimumY(),
                            plotPolyRegion.getMaximumY()
                    );
                }


                // Check if selection contains sign
                try {
                    if(!containsSign(plotPolyRegion, player.getWorld())) {
                        player.sendMessage(Utils.getErrorMessageFormat("Please place a minimum of one sign for the street side."));
                        return CompletableFuture.completedFuture(null);
                    }
                } catch (Exception ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "An error occurred while checking for sign(s) in selection!", ex);
                }
            } else {
                player.sendMessage(Utils.getErrorMessageFormat("Please use poly selection to create a new plot!"));
                return CompletableFuture.completedFuture(null);
            }
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while creating a new plot!", ex);
            player.sendMessage(Utils.getErrorMessageFormat("An error occurred while creating plot!"));
            return CompletableFuture.completedFuture(null);
        }

        // Convert polygon outline data to string
        String polyOutline;
        List<String> points = new ArrayList<>();

        for(BlockVector2D point : plotPolyRegion.getPoints())
            points.add(point.getBlockX() + "," + point.getBlockZ());
        polyOutline = StringUtils.join(points, "|");


        player.sendMessage(Utils.getInfoMessageFormat("Creating plot..."));

        // Saving schematic
        String plotFilePath, environmentFilePath = null;
        try {
            plotID = DatabaseConnection.getTableID("plotsystem_plots");


            // Save plot schematic
            plotFilePath = createPlotSchematic(plotPolyRegion, plotID + "", player, cityProject);

            if (plotFilePath.equals(errorCode)) {
                Bukkit.getLogger().log(Level.SEVERE, "Could not create schematic file! (" + plotFilePath + ")");
                player.sendMessage(Utils.getErrorMessageFormat("An error occurred while creating plot!"));
                return CompletableFuture.completedFuture(null);
            }

            // Save environment schematic
            if(environmentEnabled){
                environmentFilePath = createPlotSchematic(environmentPolyRegion, plotID + "-env", player, cityProject);

                if(environmentFilePath.equals(errorCode)) {
                    Bukkit.getLogger().log(Level.SEVERE, "Could not create schematic file! (" + environmentFilePath + ")");
                    player.sendMessage(Utils.getErrorMessageFormat("An error occurred while creating plot!"));
                    return CompletableFuture.completedFuture(null);
                }
            }
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while saving new plot to a schematic!", ex);
            player.sendMessage(Utils.getErrorMessageFormat("An error occurred while creating plot!"));
            return CompletableFuture.completedFuture(null);
        }

        try {
            // Upload to SFTP or FTP server if enabled
            FTPConfiguration ftpConfiguration = cityProject.getFTPConfiguration();
            if (ftpConfiguration != null) {

                String finalEnvironmentFilePath = environmentFilePath;
                if (CompletableFuture.supplyAsync(() -> {
                    try {
                        if(environmentEnabled)
                            return FTPManager.uploadSchematics(FTPManager.getFTPUrl(ftpConfiguration, cityProject.getID()), new File(plotFilePath), new File(finalEnvironmentFilePath));
                        else
                            return FTPManager.uploadSchematics(FTPManager.getFTPUrl(ftpConfiguration, cityProject.getID()), new File(plotFilePath));
                    } catch (URISyntaxException ex) {
                        Bukkit.getLogger().log(Level.SEVERE, "An error occurred while uploading schematic file to SFTP/FTP server!", ex);
                        return null;
                    }
                }).join() == null) throw new IOException();
            }

            // Save to database
            DatabaseConnection.createStatement("INSERT INTO plotsystem_plots (id, city_project_id, difficulty_id, mc_coordinates, outline, create_date, create_player) VALUES (?, ?, ?, ?, ?, ?, ?)")
                    .setValue(plotID)
                    .setValue(cityProject.getID())
                    .setValue(difficultyID)
                    .setValue(plotPolyRegion.getCenter().getX() + "," + plotPolyRegion.getCenter().getY() + "," + plotPolyRegion.getCenter().getZ())
                    .setValue(polyOutline)
                    .setValue(java.sql.Date.valueOf(LocalDate.now()))
                    .setValue(player.getUniqueId().toString()).executeUpdate();

            player.sendMessage(Utils.getInfoMessageFormat("Successfully created new plot! §f(City: §6" + cityProject.getName() + " §f| Plot-ID: §6" + plotID + "§f)"));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

            try {
                placePlotMarker(plotRegion, player, plotID);
            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.SEVERE, "An error occurred while placing plot marker!", ex);
            }
        } catch (SQLException | IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while saving new plot to database!", ex);
            player.sendMessage(Utils.getErrorMessageFormat("An error occurred while creating plot!"));

            try {
                Files.deleteIfExists(Paths.get(plotFilePath));
                if (environmentFilePath != null) Files.deleteIfExists(Paths.get(environmentFilePath));
            } catch (IOException e) {
                Bukkit.getLogger().log(Level.SEVERE, "An error occurred while deleting schematic!", ex);
            }
        }
        return CompletableFuture.completedFuture(null);
    }


    /**
     * Creates a plot schematic of a selected region from the player.
     *
     * @param region: Selected poly region of the player
     * @return the file path of the created schematic
     */
    private static String createPlotSchematic(AbstractRegion region, String filename, Player player, CityProject cityProject) throws SQLException, IOException, MaxChangedBlocksException {
        String filePath;

        // Create File
        filePath = Paths.get(schematicsPath, String.valueOf(cityProject.getServerID()), String.valueOf(cityProject.getID()), filename + ".schematic").toString();
        File schematic = new File(filePath);

        // Delete file if exists
        Files.deleteIfExists(schematic.getAbsoluteFile().toPath());
        boolean createdDirs = schematic.getParentFile().mkdirs();
        boolean createdFile = schematic.createNewFile();

        if ((!schematic.getParentFile().exists() && !createdDirs) || (!schematic.exists() && !createdFile))
            return errorCode;


        // Store content of region in schematic
        WorldEditPlugin worldEdit = PlotSystemTerra.DependencyManager.getWorldEditPlugin();

        Clipboard cb = new BlockArrayClipboard(region);
        cb.setOrigin(cb.getRegion().getCenter());
        LocalSession playerSession = PlotSystemTerra.DependencyManager.getWorldEdit().getSessionManager().findByName(player.getName());
        ForwardExtentCopy copy = new ForwardExtentCopy(playerSession.createEditSession(worldEdit.wrapPlayer(player)), region, cb, region.getMinimumPoint());
        Operations.completeLegacy(copy);

        try (ClipboardWriter writer = ClipboardFormat.SCHEMATIC.getWriter(new FileOutputStream(schematic, false))) {
            writer.write(cb, region.getWorld().getWorldData());
        }

        return filePath;
    }





    private static boolean containsSign(Polygonal2DRegion polyRegion, World world) {
        boolean hasSign = false;
            for (int i = polyRegion.getMinimumPoint().getBlockX(); i <= polyRegion.getMaximumPoint().getBlockX(); i++) {
                for (int j = polyRegion.getMinimumPoint().getBlockY(); j <= polyRegion.getMaximumPoint().getBlockY(); j++) {
                    for (int k = polyRegion.getMinimumPoint().getBlockZ(); k <= polyRegion.getMaximumPoint().getBlockZ(); k++) {
                        if (polyRegion.contains(new Vector(i, j, k))) {
                            Block block = world.getBlockAt(i, j, k);
                            if(block.getType().equals(Material.SIGN_POST) || block.getType().equals(Material.WALL_SIGN)) {
                                hasSign = true;

                                Sign sign = (Sign) block.getState();
                                for (int s = 0; s < 4; s++) {
                                    if(s == 1) {
                                        sign.setLine(s, "§c§lStreet Side");
                                    } else {
                                        sign.setLine(s, "");
                                    }
                                }
                                sign.update();
                            }
                        }
                    }
                }
            }
        return hasSign;
    }

    private static void placePlotMarker(Region plotRegion, Player player, int plotID) {
        Vector centerBlock = plotRegion.getCenter();
        Location highestBlock = player.getWorld().getHighestBlockAt(centerBlock.getBlockX(), centerBlock.getBlockZ()).getLocation();

        Bukkit.getScheduler().runTask(PlotSystemTerra.getPlugin(), () -> {
            player.getWorld().getBlockAt(highestBlock).setType(Material.SEA_LANTERN);
            player.getWorld().getBlockAt(highestBlock.add(0, 1, 0)).setType(Material.SIGN_POST);
            Block signBlock = player.getWorld().getBlockAt(highestBlock);

            Sign sign = (Sign) signBlock.getState();
            org.bukkit.material.Sign matSign =  new org.bukkit.material.Sign(Material.SIGN_POST);
            matSign.setFacingDirection(getPlayerFaceDirection(player).getOppositeFace());
            sign.setData(matSign);
            sign.setLine(0, "§8§lID: §c§l" + plotID);
            sign.setLine(2, "§8§lCreated By:");
            sign.setLine(3, "§c§l" + player.getName());
            sign.update();
        });
    }

    private static BlockFace getPlayerFaceDirection(Player player) {
        float y = player.getLocation().getYaw();
        if( y < 0 ){y += 360;}
        y %= 360;
        int i = (int)((y+8) / 22.5);
        return BlockFace.values()[i];
    }
}