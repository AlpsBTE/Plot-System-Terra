package github.alpsbte.commands;

import github.alpsbte.core.plotsystem.CreatePlotMenu;
import github.alpsbte.utils.Utils;
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
            if(sender.hasPermission("plotsystem.createPlot")) {
                try {
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
