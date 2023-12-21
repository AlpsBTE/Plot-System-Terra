package com.alpsbte.plotsystemterra.commands;

import com.alpsbte.alpslib.libpsterra.core.plotsystem.CreatePlotMenu;
import com.alpsbte.alpslib.libpsterra.core.plotsystem.PlotCreator;
import com.alpsbte.alpslib.libpsterra.utils.Utils;
import com.alpsbte.plotsystemterra.PlotSystemTerra;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class CMD_CreatePlot implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        if(sender instanceof Player) {
            if(Utils.hasPermission(sender, "createplot")) {
                try {
                    PlotSystemTerra plugin = PlotSystemTerra.getPlugin();
                    if (args.length > 1) {
                        if (args[0].equalsIgnoreCase("tutorial") && Utils.tryParseInt(args[1]) != null) {
                            plugin.getPlotCreator().createTutorialPlot(((Player) sender).getPlayer(), Integer.parseInt(args[1]));
                            return true;
                        }
                    }
                    new CreatePlotMenu(((Player) sender).getPlayer(), plugin.getConnection(), plugin.getPlotCreator());
                } catch (Exception ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "An error occurred while opening create plot menu!", ex);
                    sender.sendMessage(Utils.getErrorMessageFormat("An error occurred while opening create plot menu!", PlotSystemTerra.getPlugin().getConfig()));
                }
            }
        }
        return true;
    }
}
