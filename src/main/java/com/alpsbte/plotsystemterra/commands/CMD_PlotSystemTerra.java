package com.alpsbte.plotsystemterra.commands;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.utils.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

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
        ChatUtil.sendMessageBox(sender, "Plot System Terra Plugin", () -> {
            sender.sendMessage("§eCurrent Version: §7" + PlotSystemTerra.getPlugin().getDescription().getVersion());
            sender.sendMessage("§eLatest Version: §7" + PlotSystemTerra.getPlugin().updater.getVersion());
        });
    }
}
