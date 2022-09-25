package com.alpsbte.plotsystemterra.commands;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.DatabaseConnection;
import com.alpsbte.plotsystemterra.core.plotsystem.CityProject;
import com.alpsbte.plotsystemterra.core.plotsystem.PlotPaster;
import com.alpsbte.plotsystemterra.utils.Utils;
import com.sk89q.worldedit.Vector;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.ResultSet;
import java.util.logging.Level;

public class CMD_PastePlot implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        if(Utils.hasPermission(sender, "pasteplot")) {
            try {
                if (args.length >= 1 && Utils.tryParseInt(args[0]) != null) {
                    int plotID = Integer.parseInt(args[0]);

                    try (ResultSet rs = DatabaseConnection.createStatement("SELECT status, city_project_id, mc_coordinates, version FROM plotsystem_plots WHERE id = ?")
                            .setValue(plotID).executeQuery()) {

                        if (rs.next() && rs.getString(1).equals("completed")) {
                            PlotPaster plotPaster = PlotSystemTerra.getPlugin().getPlotPaster();

                            String[] splitCoordinates = rs.getString(3).split(",");
                            Vector mcCoordinates = Vector.toBlockPoint(
                                    Float.parseFloat(splitCoordinates[0]),
                                    Float.parseFloat(splitCoordinates[1]),
                                    Float.parseFloat(splitCoordinates[2])
                            );

                            try {
                                PlotPaster.pastePlotSchematic(plotID, new CityProject(rs.getInt(2)), plotPaster.world, mcCoordinates, rs.getDouble(4), plotPaster.fastMode);
                                Bukkit.broadcastMessage("§7§l>§a Pasted §61 §aplot!");
                            } catch (Exception ex) {
                                Bukkit.getLogger().log(Level.SEVERE, "An error occurred while pasting plot with the ID " + plotID + "!", ex);
                                sender.sendMessage(Utils.getErrorMessageFormat("An error occurred while pasting plot!"));
                            }
                        } else sender.sendMessage(Utils.getErrorMessageFormat("Plot with the ID " + plotID + " is not completed!"));

                        DatabaseConnection.closeResultSet(rs);
                    }
                } else {
                    sender.sendMessage(Utils.getErrorMessageFormat("Incorrect Input! Try /pasteplot <ID>"));
                }
            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.SEVERE, "An error occurred while pasting plot!", ex);
                sender.sendMessage(Utils.getErrorMessageFormat("An error occurred while pasting plot!"));
            }
        }
        return true;
    }
}
