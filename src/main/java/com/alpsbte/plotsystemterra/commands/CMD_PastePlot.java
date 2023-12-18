package com.alpsbte.plotsystemterra.commands;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.Connection;
import com.alpsbte.plotsystemterra.core.plotsystem.CityProject;
import com.alpsbte.plotsystemterra.core.plotsystem.Plot;
import com.alpsbte.plotsystemterra.core.plotsystem.PlotPaster;
import com.alpsbte.plotsystemterra.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.logging.Level;

public class CMD_PastePlot implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        if(Utils.hasPermission(sender, "pasteplot")) {
            try {
                if (args.length >= 1 && Utils.tryParseInt(args[0]) != null) {
                    int plotID = Integer.parseInt(args[0]);

                    Connection connection = PlotSystemTerra.getPlugin().getConnection();
                    Plot plot = connection.getPlot(plotID);
                    CityProject cityProject = connection.getCityProject(plot.city_project_id);
                    if (plot.status.equals("completed")) {
                        PlotPaster plotPaster = PlotSystemTerra.getPlugin().getPlotPaster();

                         try {
                            PlotPaster.pastePlotSchematic(plotID, cityProject, plotPaster.world, plot.mc_coordinates, plot.version, plotPaster.fastMode);
                            Bukkit.broadcastMessage("§7§l>§a Pasted §61 §aplot!");
                        } catch (Exception ex) {
                            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while pasting plot with the ID " + plotID + "!", ex);
                            sender.sendMessage(Utils.getErrorMessageFormat("An error occurred while pasting plot!"));
                        }
                    } else 
                        sender.sendMessage(Utils.getErrorMessageFormat("Plot with the ID " + plotID + " is not completed!"));

                    
                } else {
                    sender.sendMessage(Utils.getErrorMessageFormat("Incorrect Input! Try /pasteplot <ID>"));
                }
            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.SEVERE, "An error occurred while pasting plot!", ex);
                sender.sendMessage(Utils.getErrorMessageFormat("An error occurred while pasting plot!"));
            }
        
        }//endif permission
        return true;
    }
}
