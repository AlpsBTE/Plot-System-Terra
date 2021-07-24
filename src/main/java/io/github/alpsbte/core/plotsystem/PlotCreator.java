package github.alpsbte.core.plotsystem;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import github.alpsbte.PlotSystemTerra;
import github.alpsbte.core.DatabaseConnection;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class PlotCreator {

    private final static String schematicsPath = Paths.get(PlotSystemTerra.getPlugin().getDataFolder().getAbsolutePath(), "schematics") + File.separator;

    public static void Create(Player player, CityProject cityProject, int difficultyID) {
        int plotID = 1;
        Region plotRegion;

        // Get WorldEdit selection of player
        try {
            plotRegion = Objects.requireNonNull(WorldEdit.getInstance().getSessionManager().findByName(player.getDisplayName())).getSelection(
                    Objects.requireNonNull(WorldEdit.getInstance().getSessionManager().findByName(player.getDisplayName())).getSelectionWorld());
        } catch (NullPointerException | IncompleteRegionException ex) {
            player.sendMessage("§7§l>> §cPlease select a plot using WorldEdit!");
            return;
        }

        // Conversion
        Polygonal2DRegion polyRegion;
        try {
            // Check if WorldEdit selection is polygonal
            if (plotRegion instanceof Polygonal2DRegion) {
                // Cast WorldEdit region to polygonal region
                polyRegion = (Polygonal2DRegion) plotRegion;

                if (polyRegion.getLength() > 100 || polyRegion.getWidth() > 100 || polyRegion.getHeight() > 250) {
                    player.sendMessage("§7§l>> §cPlease adjust your selection size!");
                    return;
                }

                // Set minimum selection height under player location
                polyRegion.setMinimumY((int) player.getLocation().getY() - 5);

                if (polyRegion.getMaximumY() <= player.getLocation().getY() + 1) {
                    polyRegion.setMaximumY((int) player.getLocation().getY() + 1);
                }

                // Check if selection contains sign
                try {
                    if(!containsSign(polyRegion.iterator(), player.getWorld())) {
                        Bukkit.getLogger().log(Level.SEVERE, "An error occurred while creating a new plot!");
                        player.sendMessage("§7§l>> §cPlease place a minimum of one sign for the street side.");
                        return;
                    }
                } catch (Exception ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "An error occurred while checking for sign(s) in selection!", ex);
                }
            } else {
                player.sendMessage("§7§l>> §cPlease use poly selection to create a new plot!");
                return;
            }
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while creating a new plot!", ex);
            player.sendMessage("§7§l>> §cAn error occurred while creating plot!");
            return;
        }

        // Saving schematic
        String filePath;
        try (Connection connection = DatabaseConnection.getConnection()) {
            ResultSet rs = Objects.requireNonNull(connection).createStatement().executeQuery("SELECT (t1.idplot + 1) as firstID FROM plots t1 " +
                    "WHERE NOT EXISTS (SELECT t2.idplot FROM plots t2 WHERE t2.idplot = t1.idplot + 1)");
            if (rs.next()) {
                plotID = rs.getInt(1);
            }

            filePath = Paths.get(schematicsPath, String.valueOf(cityProject.getID()), plotID + ".schematic").toString();
            File schematic = new File(filePath);

            if(!schematic.getParentFile().mkdirs() || !schematic.createNewFile()) { throw new Exception(); };

            WorldEditPlugin worldEdit = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");

            Clipboard cb = new BlockArrayClipboard(polyRegion);
            cb.setOrigin(cb.getRegion().getCenter());
            LocalSession playerSession = WorldEdit.getInstance().getSessionManager().findByName(player.getDisplayName());
            ForwardExtentCopy copy = new ForwardExtentCopy(playerSession.createEditSession(worldEdit.wrapPlayer(player)), polyRegion, cb, polyRegion.getMinimumPoint());
            Operations.completeLegacy(copy);

            try (ClipboardWriter writer = ClipboardFormat.SCHEMATIC.getWriter(new FileOutputStream(schematic, false))) {
                writer.write(cb, polyRegion.getWorld().getWorldData());
            }
            rs.close();

            // Clear player selection
            try {
                if (worldEdit.getSelection(player.getPlayer()) != null) {
                    worldEdit.setSelection(player.getPlayer(), null);
                }
            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.WARNING, "An error occurred while trying to clear players selection!", ex);
            }
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while saving new plot to a schematic!", ex);
            player.sendMessage("§7§l>> §cAn error occurred while creating plot!");
            return;
        }

        // Save to database
        try (Connection connection = DatabaseConnection.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO plots (idplot, idcity, mcCoordinates, iddifficulty) VALUES (?, ?, ?, ?)");

            statement.setInt(1, plotID);
            statement.setInt(2, cityProject.getID());
            statement.setString(3, player.getLocation().getX() + "," + player.getLocation().getY() + "," + player.getLocation().getZ());
            statement.setInt(4, difficultyID);

            statement.execute();
            statement.close();

            player.sendMessage("§7§l>> §aSuccessfully created new plot!§7 (City: " + cityProject.getName() + " | Plot-ID: " + plotID + ")");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

            try {
                placePlotMarker(plotRegion, player.getWorld(), plotID);
            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.SEVERE, "An error occurred while placing plot marker!", ex);
            }
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while saving new plot to database!", ex);
            player.sendMessage("§7§l>> §cAn error occurred while creating plot!");

            try {
                Files.deleteIfExists(Paths.get(filePath));
            } catch (IOException e) {
                Bukkit.getLogger().log(Level.SEVERE, "An error occurred while deleting schematic!", ex);
            }
        }
    }

    private static boolean containsSign(Iterator<BlockVector> vectors, World world) {
        List<BlockVector> blockVectors = new ArrayList<>();
        vectors.forEachRemaining(blockVectors::add);

        AtomicBoolean hasSign = new AtomicBoolean(false);
        Bukkit.getScheduler().runTaskAsynchronously(PlotSystemTerra.getPlugin(), () -> {
            for (BlockVector blockVector : blockVectors) {
                Bukkit.getScheduler().runTask(PlotSystemTerra.getPlugin(), () -> {
                    Block block = world.getBlockAt(blockVector.getBlockX(), blockVector.getBlockY(), blockVector.getBlockZ());

                    if(block.getType().equals(Material.SIGN)) {
                        hasSign.set(true);
                        Sign sign = (Sign) block;

                        for (int i = 0; i < 4; i++) {
                            if(i == 1) {
                                sign.setLine(i, "Street Side");
                            } else {
                                sign.setLine(i, "");
                            }
                        }
                    }
                });
            }
        });
        return hasSign.get();
    }

    private static void placePlotMarker(Region plotRegion, World world, int plotID) {
        Vector centerBlock = plotRegion.getCenter();
        Location highestBlock = world.getHighestBlockAt(centerBlock.getBlockX(), centerBlock.getBlockZ()).getLocation();

        world.getBlockAt(highestBlock.add(0, 1, 0)).setType(Material.SEA_LANTERN);
        Block signBlock = highestBlock.add(0, 2, 0).getBlock();
        signBlock.setType(Material.SIGN);
        Sign sign = (Sign) signBlock.getState();
        sign.setLine(1, "ID: " + plotID);
    }
}
