package com.alpsbte.plotsystemterra.commands;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class CMD_PlotSystemTerra implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String[] args) {
        if (args.length == 0) {
            sendPluginInfo(sender);
            return true;
        }

        if (!(sender instanceof Player p)) return true;

        p.sendMessage(text("Usage: /plotsystemterra", RED));
        return true;
    }

    public static void sendPluginInfo(CommandSender sender) {
        // there is no better way to do this according to the paper devs
        //noinspection UnstableApiUsage
        String pluginVersion = PlotSystemTerra.getPlugin().getPluginMeta().getVersion();
        String pluginName = PlotSystemTerra.getPlugin().getName();

        sender.sendMessage(empty());
        sender.sendMessage(text("============== ", GRAY)
                .decoration(TextDecoration.STRIKETHROUGH, true)
                .append(text(pluginName, YELLOW)
                        .decoration(TextDecoration.STRIKETHROUGH, false)
                        .decoration(TextDecoration.BOLD, true))
                .append(text(" ==============", GRAY)));
        sender.sendMessage(empty());

        sender.sendMessage(text("Current Version: ", YELLOW).append(text(pluginVersion, GRAY)));
        sender.sendMessage(text("Latest Version: ", YELLOW).append(text(PlotSystemTerra.getPlugin().updater.getVersion(), GRAY)));

        int length = pluginName.length();
        char[] array = new char[length];
        Arrays.fill(array, '=');
        String bottom = "==============================" + new String(array);
        sender.sendMessage(empty());
        sender.sendMessage(text(bottom, GRAY).decoration(TextDecoration.STRIKETHROUGH, true));
    }
}
