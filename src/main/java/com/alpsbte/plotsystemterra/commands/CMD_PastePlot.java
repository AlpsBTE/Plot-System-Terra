package com.alpsbte.plotsystemterra.commands;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.alpslib.libpsterra.core.Connection;
import com.alpsbte.alpslib.libpsterra.core.plotsystem.CityProject;
import com.alpsbte.alpslib.libpsterra.core.plotsystem.Plot;
import com.alpsbte.alpslib.libpsterra.core.plotsystem.PlotPaster;
import com.alpsbte.alpslib.libpsterra.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.logging.Level;

public class CMD_PastePlot implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        if(Utils.hasPermission(sender, "pasteplot")) {
            PlotSystemTerra plugin = PlotSystemTerra.getPlugin();

            try {

                if (args.length >= 1 && Utils.tryParseInt(args[0]) != null) {
                    int plotID = Integer.parseInt(args[0]);

                    Connection connection = PlotSystemTerra.getPlugin().getConnection();
                    Plot plot = connection.getPlot(plotID);
                    CityProject cityProject = connection.getCityProject(plot.city_project_id);
                    if (plot.status.equals("completed")) {
                        PlotPaster plotPaster = plugin.getPlotPaster();

                         try {
                            plotPaster.pastePlotSchematic(plotID, cityProject, plotPaster.world, plot.mc_coordinates, plot.version, plotPaster.fastMode);
                            Bukkit.broadcastMessage("§7§l>§a Pasted §61 §aplot!");
                        } catch (Exception ex) {
                            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while pasting plot with the ID " + plotID + "!", ex);
                            sender.sendMessage(Utils.getErrorMessageFormat("An error occurred while pasting plot!", plugin.getConfig()));
                        }
                    } else 
                        sender.sendMessage(Utils.getErrorMessageFormat("Plot with the ID " + plotID + " is not completed!", plugin.getConfig()));

                    
                } else {
                    sender.sendMessage(Utils.getErrorMessageFormat("Incorrect Input! Try /pasteplot <ID>", plugin.getConfig()));
                }
            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.SEVERE, "An error occurred while pasting plot!", ex);
                sender.sendMessage(Utils.getErrorMessageFormat("An error occurred while pasting plot!", plugin.getConfig()));
            }
        
        }//endif permission
        return true;
    }
}
