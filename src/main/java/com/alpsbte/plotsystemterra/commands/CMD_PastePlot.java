package com.alpsbte.plotsystemterra.commands;

import com.alpsbte.alpslib.utils.AlpsUtils;
import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.DatabaseConnection;
import com.alpsbte.plotsystemterra.core.plotsystem.CityProject;
import com.alpsbte.plotsystemterra.core.plotsystem.PlotPaster;
import com.alpsbte.plotsystemterra.utils.Utils;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.ResultSet;
import java.util.logging.Level;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;

public class CMD_PastePlot implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        if(Utils.hasPermission(sender, "pasteplot")) {
            try {
                if (args.length >= 1 && AlpsUtils.tryParseInt(args[0]) != null) {
                    int plotID = Integer.parseInt(args[0]);

                    try (ResultSet rs = DatabaseConnection.createStatement("SELECT status, city_project_id, mc_coordinates, version FROM plotsystem_plots WHERE id = ?")
                            .setValue(plotID).executeQuery()) {

                        if (rs.next() && rs.getString(1).equals("completed")) {
                            PlotPaster plotPaster = PlotSystemTerra.getPlugin().getPlotPaster();

                            String[] splitCoordinates = rs.getString(3).split(",");
                            BlockVector3 mcCoordinates = BlockVector3.at(
                                    Float.parseFloat(splitCoordinates[0]),
                                    Float.parseFloat(splitCoordinates[1]),
                                    Float.parseFloat(splitCoordinates[2])
                            );

                            try {
                                PlotPaster.pastePlotSchematic(plotID, new CityProject(rs.getInt(2)), plotPaster.world, mcCoordinates, rs.getDouble(4), plotPaster.fastMode);
                                Bukkit.broadcast(Utils.ChatUtils.getInfoFormat(text("Pasted ", GREEN).append(text(1, GOLD).append(text(" plot!", GREEN)))));
                            } catch (Exception ex) {
                                Bukkit.getLogger().log(Level.SEVERE, "An error occurred while pasting plot with the ID " + plotID + "!", ex);
                                sender.sendMessage(Utils.ChatUtils.getAlertFormat(text("An error occurred while pasting plot!")));
                            }
                        } else sender.sendMessage(Utils.ChatUtils.getAlertFormat(text("Plot with the ID " + plotID + " is not completed!")));

                        DatabaseConnection.closeResultSet(rs);
                    }
                } else {
                    sender.sendMessage(Utils.ChatUtils.getAlertFormat(text("Incorrect Input! Try /pasteplot <ID>")));
                }
            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.SEVERE, "An error occurred while pasting plot!", ex);
                sender.sendMessage(Utils.ChatUtils.getAlertFormat(text("An error occurred while pasting plot!")));
            }
        }
        return true;
    }
}
