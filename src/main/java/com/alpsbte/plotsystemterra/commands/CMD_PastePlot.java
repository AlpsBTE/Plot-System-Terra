package com.alpsbte.plotsystemterra.commands;

import com.alpsbte.alpslib.utils.AlpsUtils;
import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.data.DataException;
import com.alpsbte.plotsystemterra.core.model.Plot;
import com.alpsbte.plotsystemterra.core.model.CityProject;
import com.alpsbte.plotsystemterra.core.plotsystem.PlotPaster;
import com.alpsbte.plotsystemterra.utils.Utils;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;

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

        try {
            Plot plot = PlotSystemTerra.getDataProvider().getPlotDataProvider().getPlot(plotID);

            if (plot == null) {
                sender.sendMessage(Utils.ChatUtils.getAlertFormat(text("Plot with the ID " + plotID + " could not be found!")));
                return true;
            }

            if (plot.getStatus().equals("completed")) {
                PlotPaster plotPaster = PlotSystemTerra.getPlugin().getPlotPaster();

                BlockVector3 mcCoordinates = BlockVector3.at(
                        Float.parseFloat(plot.getMcCoordinates()[0]),
                        Float.parseFloat(plot.getMcCoordinates()[1]),
                        Float.parseFloat(plot.getMcCoordinates()[2])
                );

                CityProject cityProject = PlotSystemTerra.getDataProvider().getCityProjectDataProvider()
                        .getCityProject(plot.getCityProjectId());

                if (PlotPaster.pastePlotSchematic(
                        plotID,
                        cityProject,
                        plotPaster.world,
                        mcCoordinates,
                        plot.getPlotVersion(),
                        plotPaster.fastMode)) {
                    Bukkit.broadcast(Utils.ChatUtils.getInfoFormat(text("Pasted ", GREEN).append(text(1, GOLD).append(text(" plot!", GREEN)))));
                }
            } else
                sender.sendMessage(Utils.ChatUtils.getAlertFormat(text("Plot with the ID " + plotID + " is not completed!")));
        } catch (DataException | SQLException | IOException | URISyntaxException e) {
            PlotSystemTerra.getPlugin().getComponentLogger().error(text("An error occurred while pasting plot!"), e);
            sender.sendMessage(Utils.ChatUtils.getAlertFormat(text("An error occurred while pasting plot!")));
        }
        return true;
    }
}
