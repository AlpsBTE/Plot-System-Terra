package com.alpsbte.plotsystemterra.commands;

import com.alpsbte.plotsystemterra.core.plotsystem.CreatePlotMenu;
import com.alpsbte.plotsystemterra.core.plotsystem.PlotCreator;
import com.alpsbte.plotsystemterra.utils.Utils;
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
                    if (args.length > 1) {
                        if (args[0].equalsIgnoreCase("tutorial") && Utils.tryParseInt(args[1]) != null) {
                            PlotCreator.createTutorialPlot(((Player) sender).getPlayer(), Integer.parseInt(args[1]));
                            return true;
                        }
                    }
                    new CreatePlotMenu(((Player) sender).getPlayer());
                } catch (Exception ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "An error occurred while opening create plot menu!", ex);
                    sender.sendMessage(Utils.getErrorMessageFormat("An error occurred while opening create plot menu!"));
                }
            }
        }
        return true;
    }
}
