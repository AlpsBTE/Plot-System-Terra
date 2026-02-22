package com.alpsbte.plotsystemterra.core.plotsystem;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.config.ConfigPaths;
import com.alpsbte.plotsystemterra.core.data.DataException;
import com.alpsbte.plotsystemterra.core.model.CityProject;
import com.alpsbte.plotsystemterra.utils.Utils;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.*;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class PlotCreator {
    public static final String SCHEMATICS_PATH = Paths.get(PlotSystemTerra.getPlugin().getDataFolder().getAbsolutePath(), "schematics") + File.separator;
    public static final int MIN_OFFSET_Y = 5;
    private static final String[] DIFFICULTY = new String[] { "easy", "medium", "hard" };

    @FunctionalInterface
    public interface IPlotRegionsAction {
        void onSchematicsCreationComplete(Polygonal2DRegion plotRegion, AbstractRegion region, Vector3 plotCenter);
    }

    public static void create(Player player, int environmentRadius, IPlotRegionsAction plotRegionsAction) {
        Vector3 plotCenter;

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
        // Get poly region
        Polygonal2DRegion plotRegion;
        switch (rawPlotRegion) {
            case Polygonal2DRegion pr -> plotRegion = pr;
            case CuboidRegion cr -> plotRegion = new Polygonal2DRegion(
                    rawPlotRegion.getWorld(),
                    cr.polygonize(4),
                    cr.getMinimumY(),
                    cr.getMaximumY()
            );
            default -> {
                player.sendMessage(Utils.ChatUtils.getAlertFormat(text("Please use polygonal selection to create a new plot!")));
                return;
            }
        }

        // Check if the polygonal region is valid
        String text = "Please adjust your selection size!";
        int max = PlotSystemTerra.getPlugin().getConfig().getInt(ConfigPaths.PLOT_LENGTH_LIMIT, 100);
        if (plotRegion.getLength() > max) {
            player.sendMessage(Utils.ChatUtils.getAlertFormat(text(text + " Length is " + plotRegion.getLength() + " and can only be smaller than " + max + "!")));
            return;
        }
        max = PlotSystemTerra.getPlugin().getConfig().getInt(ConfigPaths.PLOT_WIDTH_LIMIT, 100);
        if (plotRegion.getWidth() > max) {
            player.sendMessage(Utils.ChatUtils.getAlertFormat(text(text + " Width is " + plotRegion.getWidth() + " and can only be smaller than " + max + "!")));
            return;
        }
        if (plotRegion.getHeight() > 256 - MIN_OFFSET_Y) {
            player.sendMessage(Utils.ChatUtils.getAlertFormat(text(text + " Height is " + plotRegion.getHeight() + " and can only be smaller than 256 - " + MIN_OFFSET_Y + "!")));
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

        AbstractRegion region = createCompleteRegion(
                plotRegion,
                environmentRadius,
                player.getWorld(),
                minY,
                maxY,
                minYOffset,
                maxYOffset
        );

        plotCenter = plotRegion.getCenter();
        plotRegionsAction.onSchematicsCreationComplete(plotRegion, region, plotCenter);
    }

    public static void createPlot(Player player, @NonNull CityProject cityProject, String difficultyID) {
        PlotCreator.createPlot(player, cityProject.getId(), difficultyID);
    }

    public static void createPlotManually(Player player, String cityProjectID, String difficultyID) {
        PlotSystemTerra.getDataProvider().getCityProjectData().getCache().thenAccept(cache -> {
            if(!cache.hasCache(cityProjectID)) {
                player.sendMessage(Utils.ChatUtils.getAlertFormat(text("City Project ID does not exist. Cannot create plot!")));
                return;
            }

            String difficulty = null;
            for(String id : DIFFICULTY) {
                if (id.equalsIgnoreCase(difficultyID)) {
                    difficulty = id;
                    break;
                }
            }

            if (difficulty == null) {
                player.sendMessage(Utils.ChatUtils.getAlertFormat(
                    text("Difficulty does not exist. Supported difficulties are: " + String.join(", ", DIFFICULTY))
                ));
                return;
            }

            PlotCreator.createPlot(player, cityProjectID, difficulty);
        });
    }

    private static void createPlot(Player player, String cityProjectID, String difficultyID) {
        CompletableFuture.runAsync(() -> {
            boolean environmentEnabled;

            // Read the config
            FileConfiguration config = PlotSystemTerra.getPlugin().getConfig();
            environmentEnabled = config.getBoolean(ConfigPaths.ENVIRONMENT_ENABLED);
            int environmentRadius = environmentEnabled ? config.getInt(ConfigPaths.ENVIRONMENT_RADIUS) : 0;

            create(player, environmentRadius, (plotRegion, schematicRegion, plotCenter) -> {
                byte[] initialSchematic;

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
                        points.add(point.x() + "," + point.z());
                    polyOutline = StringUtils.join(points, "|");

                    initialSchematic = createPlotSchematic(schematicRegion);

                    // Insert into database
                    int createdPlotID = PlotSystemTerra.getDataProvider().getPlotDataProvider().createPlot(
                            cityProjectID,
                            difficultyID,
                            polyOutline,
                            player.getUniqueId(),
                            initialSchematic
                    );
                    // Place plot markings on plot region
                    placePlotMarker(plotRegion, player, createdPlotID);
                    // TODO: Change top blocks of the plot region to mark plot as created
                    player.sendMessage(Utils.ChatUtils.getInfoFormat(text("Successfully created new plot!", GREEN)
                            .append(text(" (City-Id: " + cityProjectID + " | Plot-Id: " + createdPlotID + ")", WHITE))));
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                } catch (DataException | IOException ex) {
                    PlotSystemTerra.getPlugin().getComponentLogger().error("An error occurred while creating plot!", ex);
                    player.sendMessage(Utils.ChatUtils.getAlertFormat(text("An error occurred while creating plot!")));
                }
            });
        });
    }

    public static void createTutorialPlot(Player player, int environmentRadius) {
        CompletableFuture.runAsync(() -> create(player, environmentRadius, (plotRegion, environmentRegion, plotCenter) -> {
            try {
                // Inform player about the plot creation
                player.sendMessage(Utils.ChatUtils.getInfoFormat(text("Creating plot...")));

                // Convert polygon outline data to string
                String polyOutline;
                List<String> points = new ArrayList<>();

                for (BlockVector2 point : plotRegion.getPoints())
                    points.add(point.x() + "," + point.z());
                polyOutline = StringUtils.join(points, "|");
                PlotSystemTerra.getPlugin().getComponentLogger().info(text("Tutorial plot outlines: " + polyOutline));

                // Save plot and environment regions to schematic files
                // Get plot schematic file path
                byte[] plotSchematic = createPlotSchematic(plotRegion);
                Files.write(Paths.get(SCHEMATICS_PATH, "tutorials", "id-stage.schematic"), plotSchematic, StandardOpenOption.CREATE);

                // Get environment schematic file path
                if (environmentRadius > 0) {
                    byte[] environmentSchematic = createPlotSchematic(environmentRegion);
                    Files.write(Paths.get(SCHEMATICS_PATH, "tutorials", "id-env.schematic"), environmentSchematic, StandardOpenOption.CREATE);
                }

                player.sendMessage(Utils.ChatUtils.getAlertFormat(text("Successfully created new tutorial plot! Check your console for more information!")));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            } catch (Exception ex) {
                PlotSystemTerra.getPlugin().getComponentLogger().error(text("An error occurred while creating plot!"), ex);
                player.sendMessage(Utils.ChatUtils.getAlertFormat(text("An error occurred while creating plot!")));
            }
        }));
    }

    // This creates the Region which contains plot + environment where we later create a schematic from
    public static @NonNull AbstractRegion createCompleteRegion(@NonNull Polygonal2DRegion plotRegion, int environmentRadius, World world, int minY, int maxY, int minYOffset, int maxYOffset) {
        // If no environment is needed, return the plot region directly
        if (environmentRadius <= 0) {
            return plotRegion;
        }

        CylinderRegion environmentRegion;

        // Get the center of the plot region
        Vector3 plotRegionCenter = plotRegion.getCenter();

        // Calculate the maximum distance from center to any point in the plot region
        // This ensures all plot points will be inside the cylinder
        double maxDistanceFromCenter = 0;
        for (BlockVector2 point : plotRegion.getPoints()) {
            double distance = Math.sqrt(
                Math.pow(point.x() - plotRegionCenter.x(), 2) +
                Math.pow(point.z() - plotRegionCenter.z(), 2)
            );
            maxDistanceFromCenter = Math.max(maxDistanceFromCenter, distance);
        }

        // Add environment radius to ensure plot is fully contained with surrounding area
        int radius = (int) Math.ceil(maxDistanceFromCenter) + environmentRadius;

        // Create a new cylinder region with the size of the plot + the configured radius around it
        environmentRegion = new CylinderRegion(
                plotRegion.getWorld(),
                BlockVector3.at(Math.floor(plotRegionCenter.x()), plotRegionCenter.y(), Math.floor(plotRegionCenter.z())),
                Vector2.at(radius, radius),
                minY,
                maxY
        );

        // Convert region to polygonal region and save points
        final List<BlockVector2> environmentRegionPoints = environmentRegion.polygonize(-1);
        final AtomicInteger newYMin = new AtomicInteger(minY);

        // Iterate over the points and check for the lowest Y value
        environmentRegionPoints.forEach(p -> {
            int highestBlock = minYOffset;
            for (int y = minYOffset; y <= maxYOffset; y++) {
                if (world.getBlockAt(p.x(), y, p.z()).getType() != Material.AIR) highestBlock = y;
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
        return environmentRegion;
    }

    /**
     * Creates a plot schematic of a selected region from the player.
     *
     * @param region: Selected poly region of the player
     * @return the byte array of the created schematic
     */
    private static byte[] createPlotSchematic(AbstractRegion region) throws WorldEditException, IOException {
        // Store content of region in schematic
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(BlockVector3.at(region.getCenter().x(), region.getMinimumPoint().y(), region.getCenter().z()));
        ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                Objects.requireNonNull(region.getWorld()), region, clipboard, region.getMinimumPoint()
        );
        Operations.complete(forwardExtentCopy);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (ClipboardWriter writer = BuiltInClipboardFormat.FAST_V2.getWriter(outputStream)) {
            writer.write(clipboard);
        }

        return outputStream.toByteArray();
    }

    /**
     * Checks if polygon region contains a sign and update sign text
     *
     * @param polyRegion WorldEdit region
     * @param world      Region world
     * @return true if polygon region contains a sign, false otherwise
     */
    private static boolean containsSign(@NonNull Polygonal2DRegion polyRegion, World world) {
        boolean hasSign = false;
        for (int i = polyRegion.getMinimumPoint().x(); i <= polyRegion.getMaximumPoint().x(); i++) {
            for (int j = polyRegion.getMinimumPoint().y(); j <= polyRegion.getMaximumPoint().y(); j++) {
                for (int k = polyRegion.getMinimumPoint().z(); k <= polyRegion.getMaximumPoint().z(); k++) {
                    if (!polyRegion.contains(BlockVector3.at(i, j, k))) continue;
                    Block block = world.getBlockAt(i, j, k);
                    if (!block.getType().equals(Material.OAK_SIGN) && !block.getType().equals(Material.OAK_WALL_SIGN))
                        continue;
                    hasSign = true;

                    Bukkit.getScheduler().runTask(PlotSystemTerra.getPlugin(), () -> {
                        Sign sign = (Sign) block.getState();
                        for (int s = 0; s < 4; s++) {
                            if (s != 1) continue;
                            sign.getSide(Side.FRONT).line(s, text("Street Side", GOLD, BOLD));
                            sign.getSide(Side.BACK).line(s, text("Street Side", GOLD, BOLD));
                        }
                        sign.update();
                    });
                }
            }
        }
        return hasSign;
    }

    /**
     * Places a plot marker in the center of the polygon region
     *
     * @param plotRegion WorldEdit region
     * @param player     Player
     * @param plotID     Plot ID
     */
    private static void placePlotMarker(@NonNull Region plotRegion, @NonNull Player player, int plotID) {
        Vector3 centerBlock = plotRegion.getCenter();
        Location highestBlock = player.getWorld().getHighestBlockAt(centerBlock.toBlockPoint().x(), centerBlock.toBlockPoint().z()).getLocation();

        Bukkit.getScheduler().runTask(PlotSystemTerra.getPlugin(), () -> {
            player.getWorld().getBlockAt(highestBlock).setType(Material.SEA_LANTERN);
            player.getWorld().getBlockAt(highestBlock.add(0, 1, 0)).setType(Material.OAK_SIGN);
            Block signBlock = player.getWorld().getBlockAt(highestBlock);

            Sign sign = (Sign) signBlock.getState();
            org.bukkit.block.data.type.Sign matSign = (org.bukkit.block.data.type.Sign) sign.getBlockData();
            BlockFace rotation = getPlayerFaceDirection(player);
            matSign.setRotation(rotation == BlockFace.DOWN || rotation == BlockFace.UP ? BlockFace.NORTH : rotation);
            sign.setBlockData(matSign);
            sign.getSide(Side.FRONT).line(0, text("ID: ", GRAY, BOLD).append(text(plotID, GOLD, BOLD)));
            sign.getSide(Side.FRONT).line(2, text("Created By: ", GRAY, BOLD));
            sign.getSide(Side.FRONT).line(3, text(player.getName(), RED, BOLD));
            sign.update();
        });
    }

    /**
     * Gets the direction the player is facing
     *
     * @param player Player
     * @return Direction
     */
    private static BlockFace getPlayerFaceDirection(@NonNull Player player) {
        float y = player.getLocation().getYaw();
        if (y < 0) {
            y += 360;
        }
        y %= 360;
        int i = (int) ((y + 8) / 22.5);
        return BlockFace.values()[i];
    }
}