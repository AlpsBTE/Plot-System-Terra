package com.alpsbte.plotsystemterra.core.plotsystem;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.DatabaseConnection;
import com.alpsbte.plotsystemterra.core.config.ConfigPaths;
import com.alpsbte.plotsystemterra.utils.FTPManager;
import com.alpsbte.plotsystemterra.utils.Utils;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.AbstractRegion;
import com.sk89q.worldedit.regions.CylinderRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
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

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class PlotCreator {
    @FunctionalInterface
    public interface IPlotRegionsAction {
        void onSchematicsCreationComplete(Polygonal2DRegion plotRegion, CylinderRegion environmentRegion, Vector3 plotCenter);
    }

    public final static double PLOT_VERSION = 3.0;
    public final static String schematicsPath = Paths.get(PlotSystemTerra.getPlugin().getDataFolder().getAbsolutePath(), "schematics") + File.separator;
    public final static int MIN_OFFSET_Y = 5;

    public static void create(Player player, int environmentRadius, IPlotRegionsAction plotRegionsAction) {
        Polygonal2DRegion plotRegion;
        Vector3 plotCenter;
        CylinderRegion environmentRegion = null;

        // Get WorldEdit selection of player
        Region rawPlotRegion;
        try {
            rawPlotRegion = Objects.requireNonNull(WorldEdit.getInstance().getSessionManager().findByName(player.getName())).getSelection(
                    Objects.requireNonNull(WorldEdit.getInstance().getSessionManager().findByName(player.getName())).getSelectionWorld());
        } catch (NullPointerException | IncompleteRegionException ex) {
            player.sendMessage(Utils.ChatUtils.getAlertFormat(text("Please select a plot using WorldEdit!")));
            return;
        }

        // Create plot and environment regions
        // Check if WorldEdit selection is polygonal
        if (rawPlotRegion instanceof Polygonal2DRegion) {
            // Cast WorldEdit region to polygonal region
            plotRegion = (Polygonal2DRegion) rawPlotRegion;

            // Check if the polygonal region is valid
            if (plotRegion.getLength() > 100 || plotRegion.getWidth() > 100 || (plotRegion.getHeight() > 256 - MIN_OFFSET_Y)) {
                player.sendMessage(Utils.ChatUtils.getAlertFormat(text("Please adjust your selection size!")));
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
            if (environmentRadius > 0) {
                // Get min region size for environment radius
                int radius = Math.max(plotRegion.getWidth() / 2 + environmentRadius, plotRegion.getLength() / 2 + environmentRadius);

                // Create a new cylinder region with the size of the plot + the configured radius around it
                Vector3 plotRegionCenter = plotRegion.getCenter();
                environmentRegion = new CylinderRegion(
                        plotRegion.getWorld(),
                        BlockVector3.at(Math.floor(plotRegionCenter.getX()), plotRegionCenter.getY(), Math.floor(plotRegionCenter.getZ())),
                        Vector2.at(radius, radius),
                        minY,
                        maxY
                );

                // Convert environment region to polygonal region and save points
                final List<BlockVector2> environmentRegionPoints = environmentRegion.polygonize(-1);
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
            plotRegionsAction.onSchematicsCreationComplete(plotRegion, environmentRegion, plotCenter);
        } else {
            player.sendMessage(Utils.ChatUtils.getAlertFormat(text("Please use polygonal selection to create a new plot!")));
        }
    }

    public static void createPlot(Player player, CityProject cityProject, int difficultyID) {
        CompletableFuture.runAsync(() -> {
            boolean environmentEnabled;

                // Read the config
                FileConfiguration config = PlotSystemTerra.getPlugin().getConfig();
                environmentEnabled = config.getBoolean(ConfigPaths.ENVIRONMENT_ENABLED);
                int environmentRadius = config.getInt(ConfigPaths.ENVIRONMENT_RADIUS);

                create(player, environmentEnabled ? environmentRadius : -1, (plotRegion, environmentRegion, plotCenter) -> {
                    int plotID;
                    String plotFilePath;
                    String environmentFilePath = null;
                    Connection connection = null;

                    try {
                        // Inform player about the plot creation
                        player.sendMessage(Utils.ChatUtils.getInfoFormat(text("Creating plot...")));

                        // Check if selection contains sign
                        if (!containsSign(plotRegion, player.getWorld())) {
                            player.sendMessage(Utils.ChatUtils.getAlertFormat(text("Please place a minimum of one sign for the street side!")));
                            return;
                        }


                        // Convert polygon outline data to string
                        String polyOutline;
                        List<String> points = new ArrayList<>();

                        for (BlockVector2 point : plotRegion.getPoints())
                            points.add(point.getX() + "," + point.getZ());
                        polyOutline = StringUtils.join(points, "|");


                        // Insert into database
                        connection = DatabaseConnection.getConnection();

                        if (connection != null) {
                            connection.setAutoCommit(false);

                            try (PreparedStatement stmt = Objects.requireNonNull(connection).prepareStatement("INSERT INTO plotsystem_plots (city_project_id, difficulty_id, mc_coordinates, outline, create_date, create_player, version) VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                                stmt.setInt(1, cityProject.getID());
                                stmt.setInt(2, difficultyID);
                                stmt.setString(3, plotCenter.getX() + "," + plotCenter.getY() + "," + plotCenter.getZ());
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
                        plotFilePath = createPlotSchematic(plotRegion, Paths.get(schematicsPath, String.valueOf(cityProject.getServerID()), String.valueOf(cityProject.getID()), plotID + ".schem").toString());

                        if (plotFilePath == null) {
                            Bukkit.getLogger().log(Level.SEVERE, "Could not create plot schematic file!");
                            player.sendMessage(Utils.ChatUtils.getAlertFormat(text("An error occurred while creating plot!")));
                            return;
                        }

                        // Get environment schematic file path
                        if (environmentEnabled) {
                            environmentFilePath = createPlotSchematic(environmentRegion, Paths.get(schematicsPath, String.valueOf(cityProject.getServerID()), String.valueOf(cityProject.getID()), plotID + "-env.schem").toString());

                            if (environmentFilePath == null) {
                                Bukkit.getLogger().log(Level.SEVERE, "Could not create environment schematic file!");
                                player.sendMessage(Utils.ChatUtils.getAlertFormat(text("An error occurred while creating plot!")));
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

                        player.sendMessage(Utils.ChatUtils.getInfoFormat(text("Successfully created new plot!", GREEN)
                                .append(text(" (City: " + cityProject.getName() + " | Plot-Id: " + plotID + ")", WHITE))));
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
                        player.sendMessage(Utils.ChatUtils.getAlertFormat(text("An error occurred while creating plot!")));
                    }
                });
        });
    }

    public static void createTutorialPlot(Player player, int environmentRadius) {
        CompletableFuture.runAsync(() -> {
            create(player, environmentRadius, (plotRegion, environmentRegion, plotCenter) -> {
                try {
                    // Inform player about the plot creation
                    player.sendMessage(Utils.ChatUtils.getInfoFormat(text("Creating plot...")));


                    // Convert polygon outline data to string
                    String polyOutline;
                    List<String> points = new ArrayList<>();

                    for (BlockVector2 point : plotRegion.getPoints())
                        points.add(point.getX() + "," + point.getZ());
                    polyOutline = StringUtils.join(points, "|");
                    Bukkit.getLogger().log(Level.INFO, "Tutorial plot outlines: " + polyOutline);

                    // Save plot and environment regions to schematic files
                    // Get plot schematic file path
                    String plotFilePath = createPlotSchematic(plotRegion, Paths.get(schematicsPath, "tutorials", "id-stage.schematic").toString());

                    if (plotFilePath == null) {
                        Bukkit.getLogger().log(Level.SEVERE, "Could not create plot schematic file!");
                        player.sendMessage(Utils.ChatUtils.getAlertFormat(text("An error occurred while creating plot!")));
                        return;
                    }
                    Bukkit.getLogger().log(Level.INFO, "Tutorial plot schematic path: " + plotFilePath);

                    // Get environment schematic file path
                    if (environmentRadius > 0) {
                        String environmentFilePath = createPlotSchematic(environmentRegion, Paths.get(schematicsPath, "tutorials", "id-env.schematic").toString());

                        if (environmentFilePath == null) {
                            Bukkit.getLogger().log(Level.SEVERE, "Could not create environment schematic file!");
                            player.sendMessage(Utils.ChatUtils.getAlertFormat(text("An error occurred while creating plot!")));
                            return;
                        }
                        Bukkit.getLogger().log(Level.INFO, "Tutorial environment schematic path: " + environmentFilePath);
                    }

                    player.sendMessage(Utils.ChatUtils.getAlertFormat(text("Successfully created new tutorial plot! Check your console for more information!")));
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                } catch (Exception ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "An error occurred while creating plot!", ex);
                    player.sendMessage(Utils.ChatUtils.getAlertFormat(text("An error occurred while creating plot!")));
                }
            });
        });
    }



    /**
     * Creates a plot schematic of a selected region from the player.
     *
     * @param region: Selected poly region of the player
     * @return the file path of the created schematic
     */
    private static String createPlotSchematic(AbstractRegion region, String filePath) throws IOException, WorldEditException {
        // Create File
        File schematic = new File(filePath);

        // Delete file if exists
        Files.deleteIfExists(schematic.getAbsoluteFile().toPath());
        boolean createdDirs = schematic.getParentFile().mkdirs();
        boolean createdFile = schematic.createNewFile();

        if ((!schematic.getParentFile().exists() && !createdDirs) || (!schematic.exists() && !createdFile))
            return null;

        // Store content of region in schematic
        Clipboard cb = new BlockArrayClipboard(region);
        cb.setOrigin(BlockVector3.at(region.getCenter().getX(), region.getMinimumPoint().getY(), region.getCenter().getZ()));
        EditSession editSession = PlotSystemTerra.DependencyManager.getWorldEdit().getEditSessionFactory().getEditSession(region.getWorld(), -1);
        ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(editSession, region, cb, region.getMinimumPoint());
        Operations.complete(forwardExtentCopy);

        try(ClipboardWriter writer = Objects.requireNonNull(ClipboardFormats.findByFile(schematic)).getWriter(new FileOutputStream(schematic, false))) {
            writer.write(cb);
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
                    if (polyRegion.contains(BlockVector3.at(i, j, k))) {
                        Block block = world.getBlockAt(i, j, k);
                        if(block.getType().equals(Material.OAK_SIGN) || block.getType().equals(Material.OAK_WALL_SIGN)) {
                            hasSign = true;

                            Bukkit.getScheduler().runTask(PlotSystemTerra.getPlugin(), () -> {
                                Sign sign = (Sign) block.getState();
                                for (int s = 0; s < 4; s++) {
                                    if(s == 1) {
                                        sign.getSide(Side.FRONT).line(s, text("Street Side", GOLD, BOLD));
                                        sign.getSide(Side.BACK).line(s, text("Street Side", GOLD, BOLD));
                                    }
                                }
                                sign.update();
                            });
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
        Vector3 centerBlock = plotRegion.getCenter();
        Location highestBlock = player.getWorld().getHighestBlockAt(centerBlock.toBlockPoint().getBlockX(), centerBlock.toBlockPoint().getZ()).getLocation();

        Bukkit.getScheduler().runTask(PlotSystemTerra.getPlugin(), () -> {
            player.getWorld().getBlockAt(highestBlock).setType(Material.SEA_LANTERN);
            player.getWorld().getBlockAt(highestBlock.add(0, 1, 0)).setType(Material.OAK_SIGN);
            Block signBlock = player.getWorld().getBlockAt(highestBlock);

            Sign sign = (Sign) signBlock.getState();
            org.bukkit.block.data.type.Sign matSign = (org.bukkit.block.data.type.Sign) sign.getBlockData();
            BlockFace rotation = getPlayerFaceDirection(player);
            matSign.setRotation(rotation == BlockFace.DOWN || rotation == BlockFace.UP ? BlockFace.NORTH : rotation);
            sign.setBlockData(matSign);
            sign.getSide(Side.FRONT).line(0,  text("ID: ", GRAY, BOLD).append(text(plotID, GOLD, BOLD)));
            sign.getSide(Side.FRONT).line(2,  text("Created By: ", GRAY, BOLD));
            sign.getSide(Side.FRONT).line(3,  text(player.getName(), RED, BOLD));
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