package com.alpsbte.plotsystemterra.commands;

import com.alpsbte.alpslib.utils.AlpsUtils;
import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.data.DataException;
import com.alpsbte.plotsystemterra.core.model.Plot;
import com.alpsbte.plotsystemterra.core.model.CityProject;
import com.alpsbte.plotsystemterra.core.plotsystem.PlotPaster;
import com.alpsbte.plotsystemterra.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;

public class CMD_PastePlot implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String @NotNull [] args) {
        if (!Utils.hasPermission(sender, "pasteplot")) return false;

        if (args.length < 1 || AlpsUtils.tryParseInt(args[0]) == null) {
            sender.sendMessage(Utils.ChatUtils.getAlertFormat(text("Incorrect Input! Try /pasteplot <ID>")));
            return true;
        }

        int plotID = Integer.parseInt(args[0]);

        sender.sendMessage(Utils.ChatUtils.getInfoFormat(text("Fetching plot data...")));
        PlotSystemTerra.getDataProvider().getPlotDataProvider().getPlotAsync(plotID)
                .thenAccept(plot -> {
                    plotValidation(sender, plot, plotID);
                });
        return true;
    }

    private void plotValidation(CommandSender sender, Plot plot, int plotId) {
        if (plot == null) {
            sender.sendMessage(Utils.ChatUtils.getAlertFormat(text("Plot with the ID " + plotId + " could not be found!")));
            return;
        }

        if (!plot.getStatus().equals("completed")) {
            sender.sendMessage(Utils.ChatUtils.getAlertFormat(text("Plot with the ID " + plotId + " is not completed!")));
            return;
        }

        sender.sendMessage(Utils.ChatUtils.getInfoFormat(text("Fetching city project data...")));
        PlotSystemTerra.getDataProvider().getCityProjectDataProvider().getCityProjectAsync(plot.getCityProjectId())
                .thenAccept(cityProject -> {
                    plotPasting(sender, plot, cityProject);
                });
    }

    private void plotPasting(CommandSender sender, Plot plot, CityProject cityProject) {
        PlotPaster plotPaster = PlotSystemTerra.getPlugin().getPlotPaster();
        try {
            if (PlotPaster.pastePlotSchematic(
                    plot,
                    cityProject,
                    plotPaster.world,
                    plot.getCompletedSchematic(),
                    plot.getPlotVersion())) {
                Bukkit.broadcast(Utils.ChatUtils.getInfoFormat(text("Pasted ", GREEN).append(text(1, GOLD).append(text(" plot!", GREEN)))));
            }
        } catch (IOException e) {
            PlotSystemTerra.getPlugin().getComponentLogger().error(text("An error occurred while pasting plot!"), e);
            sender.sendMessage(Utils.ChatUtils.getAlertFormat(text("An error occurred while pasting plot!")));
        }
    }
}
