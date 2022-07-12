package com.alpsbte.plotsystemterra.core.plotsystem;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.DatabaseConnection;
import com.alpsbte.plotsystemterra.core.config.ConfigPaths;
import com.alpsbte.plotsystemterra.utils.FTPManager;
import com.alpsbte.plotsystemterra.utils.Utils;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class PlotCreator {
    public final static double PLOT_VERSION = 3.0;
    public final static String schematicsPath = Paths.get(PlotSystemTerra.getPlugin().getDataFolder().getAbsolutePath(), "schematics") + File.separator;
    public final static int MIN_OFFSET_Y = 5;

    public static void Create(Player player, CityProject cityProject, int difficultyID) {
        CompletableFuture.runAsync(() -> {
            int plotID;
            boolean environmentEnabled;

            Polygonal2DRegion plotRegion;
            String plotFilePath;
            Vector plotCenter;
            CylinderRegion environmentRegion = null;
            String environmentFilePath = null;

            Connection connection = null;

            try {
                // Read the config
                FileConfiguration config = PlotSystemTerra.getPlugin().getConfig();
                environmentEnabled = config.getBoolean(ConfigPaths.ENVIRONMENT_ENABLED);
                int environmentRadius = config.getInt(ConfigPaths.ENVIRONMENT_RADIUS);


                // Get WorldEdit selection of player
                Region rawPlotRegion;
                try {
                    rawPlotRegion = Objects.requireNonNull(WorldEdit.getInstance().getSessionManager().findByName(player.getName())).getSelection(
                            Objects.requireNonNull(WorldEdit.getInstance().getSessionManager().findByName(player.getName())).getSelectionWorld());
                } catch (NullPointerException | IncompleteRegionException ex) {
                    player.sendMessage(Utils.getErrorMessageFormat("Please select a plot using WorldEdit!"));
                    return;
                }


                // Create plot and environment regions
                // Check if WorldEdit selection is polygonal
                if (rawPlotRegion instanceof Polygonal2DRegion) {
                    // Cast WorldEdit region to polygonal region
                    plotRegion = (Polygonal2DRegion) rawPlotRegion;

                    // Check if the polygonal region is valid
                    if (plotRegion.getLength() > 100 || plotRegion.getWidth() > 100 || (plotRegion.getHeight() > 256 - MIN_OFFSET_Y)) {
                        player.sendMessage(Utils.getErrorMessageFormat("Please adjust your selection size!"));
                        return;
                    }

                    // Get plot minY and maxY
                    double offsetHeight = (256 - plotRegion.getHeight()) / 2d;
                    final int minYOffset = plotRegion.getMinimumY() - (int) Math.ceil(offsetHeight);
                    final int maxYOffset = plotRegion.getMaximumY() + (int) Math.floor(offsetHeight);
                    final int minY = plotRegion.getMinimumY() - MIN_OFFSET_Y;
                    final int maxY = maxYOffset + (int) Math.ceil(offsetHeight) - MIN_OFFSET_Y;

                    plotRegion.setMinimumY(minY);
                    plotRegion.setMaximumY(maxY);

                    // Create the environment selection
                    if (environmentEnabled) {
                        // Get min region size for environment radius
                        int radius = Math.max(plotRegion.getWidth() / 2 + environmentRadius, plotRegion.getLength() / 2 + environmentRadius);

                        // Create a new cylinder region with the size of the plot + the configured radius around it
                        Vector plotRegionCenter = plotRegion.getCenter();
                        environmentRegion = new CylinderRegion(
                                plotRegion.getWorld(),
                                new Vector(Math.floor(plotRegionCenter.getX()), plotRegionCenter.getY(), Math.floor(plotRegionCenter.getZ())),
                                new Vector2D(radius, radius),
                                minY,
                                maxY
                        );

                        // Convert environment region to polygonal region and save points
                        final List<BlockVector2D> environmentRegionPoints = environmentRegion.polygonize(-1);
                        final AtomicInteger newYMin = new AtomicInteger(minY);

                        // Iterate over the points and check for the lowest Y value
                        final World world = player.getWorld();
                        environmentRegionPoints.forEach(p -> {
                            int highestBlock = minYOffset;
                            for (int y = minYOffset; y <= maxYOffset; y++) {
                                if (world.getBlockAt(p.getBlockX(), y, p.getBlockZ()).getType() != Material.AIR) highestBlock = y;
                            }
                            if (highestBlock < newYMin.get()) newYMin.set(highestBlock);
                        });

                        // Update plot and environment min and max Y to new value if necessary
                        if (newYMin.get() < minY) {
                            int heightDif = (minY - newYMin.get()) + MIN_OFFSET_Y;
                            plotRegion.setMinimumY(newYMin.get() - MIN_OFFSET_Y);
                            environmentRegion.setMinimumY(newYMin.get() - MIN_OFFSET_Y);
                            plotRegion.setMaximumY(maxY - heightDif);
                            environmentRegion.setMaximumY(maxY - heightDif);
                        }
                    }
                    plotCenter = plotRegion.getCenter();
                } else {
                    player.sendMessage(Utils.getErrorMessageFormat("Please use polygonal selection to create a new plot!"));
                    return;
                }


                // Check if selection contains sign
                if (!containsSign(plotRegion, player.getWorld())) {
                    player.sendMessage(Utils.getErrorMessageFormat("Please place a minimum of one sign for the street side!"));
                    return;
                }


                // Inform player about the plot creation
                player.sendMessage(Utils.getInfoMessageFormat("Creating plot..."));


                // Convert polygon outline data to string
                String polyOutline;
                List<String> points = new ArrayList<>();

                for (BlockVector2D point : plotRegion.getPoints())
                    points.add(point.getX() + "," + point.getZ());
                polyOutline = StringUtils.join(points, "|");


                // Insert into database
                connection = DatabaseConnection.getConnection();

                if (connection != null) {
                    connection.setAutoCommit(false);

                    try (PreparedStatement stmt = Objects.requireNonNull(connection).prepareStatement("INSERT INTO plotsystem_plots (city_project_id, difficulty_id, mc_coordinates, outline, create_date, create_player, version) VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                        stmt.setInt(1, cityProject.getID());
                        stmt.setInt(2, difficultyID);
                        stmt.setString(3, plotCenter.getX() + "," + plotCenter.getBlockY() + "," + plotCenter.getZ());
                        stmt.setString(4, polyOutline);
                        stmt.setDate(5, java.sql.Date.valueOf(LocalDate.now()));
                        stmt.setString(6, player.getUniqueId().toString());
                        stmt.setDouble(7, PLOT_VERSION);
                        stmt.executeUpdate();

                        // Get the id of the new plot
                        try (ResultSet rs = stmt.getGeneratedKeys()) {
                            if (rs.next()) {
                                plotID = rs.getInt(1);
                            } else throw new SQLException("Could not obtain generated key");
                        }
                    }
                } else throw new SQLException("Could not connect to database");


                // Save plot and environment regions to schematic files
                // Get plot schematic file path
                plotFilePath = createPlotSchematic(plotRegion, plotID + "", cityProject);

                if (plotFilePath == null) {
                    Bukkit.getLogger().log(Level.SEVERE, "Could not create plot schematic file!");
                    player.sendMessage(Utils.getErrorMessageFormat("An error occurred while creating plot!"));
                    return;
                }

                // Get environment schematic file path
                if (environmentEnabled) {
                    environmentFilePath = createPlotSchematic(environmentRegion, plotID + "-env", cityProject);

                    if (environmentFilePath == null) {
                        Bukkit.getLogger().log(Level.SEVERE, "Could not create environment schematic file!");
                        player.sendMessage(Utils.getErrorMessageFormat("An error occurred while creating plot!"));
                        return;
                    }
                }


                // Upload schematic files to SFTP/FTP server if enabled
                FTPConfiguration ftpConfiguration = cityProject.getFTPConfiguration();
                if (ftpConfiguration != null) {
                    if (environmentEnabled) FTPManager.uploadSchematics(FTPManager.getFTPUrl(ftpConfiguration, cityProject.getID()), new File(plotFilePath), new File(environmentFilePath));
                    else FTPManager.uploadSchematics(FTPManager.getFTPUrl(ftpConfiguration, cityProject.getID()), new File(plotFilePath));
                }


                // Place plot markings on plot region
                placePlotMarker(plotRegion, player, plotID);
                // TODO: Change top blocks of the plot region to mark plot as created


                // Finalize database transaction
                connection.commit();
                connection.close();

                player.sendMessage(Utils.getInfoMessageFormat("Successfully created new plot! §f(City: §6" + cityProject.getName() + " §f| Plot-ID: §6" + plotID + "§f)"));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            } catch (Exception ex) {
                try {
                    if (connection != null) {
                        connection.rollback();
                        connection.close();
                    }
                } catch (SQLException sqlEx) {
                    Bukkit.getLogger().log(Level.SEVERE, "A SQL error occurred!", sqlEx);
                }
                Bukkit.getLogger().log(Level.SEVERE, "An error occurred while creating plot!", ex);
                player.sendMessage(Utils.getErrorMessageFormat("An error occurred while creating plot!"));
            }
        });
    }




    /**
     * Creates a plot schematic of a selected region from the player.
     *
     * @param region: Selected poly region of the player
     * @return the file path of the created schematic
     */
    private static String createPlotSchematic(AbstractRegion region, String filename, CityProject cityProject) throws SQLException, IOException, WorldEditException {
        // Create File
        String filePath = Paths.get(schematicsPath, String.valueOf(cityProject.getServerID()), String.valueOf(cityProject.getID()), filename + ".schematic").toString();
        File schematic = new File(filePath);

        // Delete file if exists
        Files.deleteIfExists(schematic.getAbsoluteFile().toPath());
        boolean createdDirs = schematic.getParentFile().mkdirs();
        boolean createdFile = schematic.createNewFile();

        if ((!schematic.getParentFile().exists() && !createdDirs) || (!schematic.exists() && !createdFile))
            return null;


        // Store content of region in schematic
        BlockArrayClipboard cb = new BlockArrayClipboard(region);
        cb.setOrigin(cb.getRegion().getCenter());
        EditSession editSession = PlotSystemTerra.DependencyManager.getWorldEdit().getEditSessionFactory().getEditSession(region.getWorld(), -1);
        ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(editSession, region, cb, region.getMinimumPoint());
        Operations.complete(forwardExtentCopy);

        try(ClipboardWriter writer = ClipboardFormat.SCHEMATIC.getWriter(new FileOutputStream(schematic, false))) {
            writer.write(cb, Objects.requireNonNull(region.getWorld()).getWorldData());
        }

        return filePath;
    }

    /**
     * Checks if polygon region contains a sign and update sign text
     * @param polyRegion WorldEdit region
     * @param world Region world
     * @return true if polygon region contains a sign, false otherwise
     */
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

    /**
     * Places a plot marker in the center of the polygon region
     * @param plotRegion WorldEdit region
     * @param player Player
     * @param plotID Plot ID
     */
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

    /**
     * Gets the direction the player is facing
     * @param player Player
     * @return Direction
     */
    private static BlockFace getPlayerFaceDirection(Player player) {
        float y = player.getLocation().getYaw();
        if( y < 0 ){y += 360;}
        y %= 360;
        int i = (int)((y+8) / 22.5);
        return BlockFace.values()[i];
    }
}