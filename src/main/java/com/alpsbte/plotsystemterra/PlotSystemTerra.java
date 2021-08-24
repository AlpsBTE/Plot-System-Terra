package com.alpsbte.plotsystemterra;

import com.alpsbte.plotsystemterra.commands.CMD_CreatePlot;
import com.alpsbte.plotsystemterra.core.DatabaseConnection;
import com.alpsbte.plotsystemterra.core.EventListener;
import com.alpsbte.plotsystemterra.core.config.ConfigManager;
import com.alpsbte.plotsystemterra.core.config.ConfigNotImplementedException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.ipvp.canvas.MenuFunctionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class PlotSystemTerra extends JavaPlugin {

    private static PlotSystemTerra plugin;
    private ConfigManager configManager;

    private boolean pluginEnabled = false;

    @Override
    public void onEnable() {
        plugin = this;

        String successPrefix = ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GREEN + "✔" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;
        String errorPrefix = ChatColor.DARK_GRAY + "[" + ChatColor.RED + "X" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;

        Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "--------------- Plot-System-Terra V1.2 ----------------");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GREEN + "Starting plugin...");
        Bukkit.getConsoleSender().sendMessage("");

        // Check for required dependencies, if it returns false disable plugin
        if (!DependencyManager.checkForRequiredDependencies()) {
            Bukkit.getConsoleSender().sendMessage(errorPrefix + "Could not load required dependencies.");
            Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "Missing Dependencies:");
            DependencyManager.missingDependencies.forEach(dependency -> Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + " - " + dependency));

            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Bukkit.getConsoleSender().sendMessage(successPrefix + "Successfully loaded required dependencies.");

        // Load config, if it throws an exception disable plugin
        try {
            configManager = new ConfigManager();
            Bukkit.getConsoleSender().sendMessage(successPrefix + "Successfully loaded configuration file.");
        } catch (ConfigNotImplementedException ex) {
            Bukkit.getConsoleSender().sendMessage(errorPrefix + "Could not load configuration file.");
            Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "The config file must be configured!");

            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.configManager.reloadConfigs();

        // Initialize database connection
        try {
            DatabaseConnection.InitializeDatabase();
            Bukkit.getConsoleSender().sendMessage(successPrefix + "Successfully initialized database connection.");
        } catch (Exception ex) {
            Bukkit.getConsoleSender().sendMessage(errorPrefix + "Could not initialize database connection.");
            Bukkit.getLogger().log(Level.SEVERE, ex.getMessage(), ex);

            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register event listeners
        try {
            this.getServer().getPluginManager().registerEvents(new EventListener(), this);
            this.getServer().getPluginManager().registerEvents(new MenuFunctionListener(), this);
            Bukkit.getConsoleSender().sendMessage(successPrefix + "Successfully registered event listeners.");
        } catch (Exception ex) {
            Bukkit.getConsoleSender().sendMessage(errorPrefix + "Could not register event listeners.");
            Bukkit.getLogger().log(Level.SEVERE, ex.getMessage(), ex);

            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register commands
        try {
            this.getCommand("createplot").setExecutor(new CMD_CreatePlot());
            Bukkit.getConsoleSender().sendMessage(successPrefix + "Successfully registered commands.");
        } catch (Exception ex) {
            Bukkit.getConsoleSender().sendMessage(errorPrefix + "Could not register commands.");
            Bukkit.getLogger().log(Level.SEVERE, ex.getMessage(), ex);

            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        pluginEnabled = true;
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GREEN + "Enabled Plot-System-Terra plugin.");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "------------------------------------------------------");
    }

    @Override
    public void onDisable() {
        if (!pluginEnabled) {
            Bukkit.getConsoleSender().sendMessage("");
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Disabling plugin...");
            Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "------------------------------------------------------");
        }
    }

    @Override
    public FileConfiguration getConfig() {
        return this.configManager.getConfig();
    }

    @Override
    public void reloadConfig() {
        this.configManager.reloadConfigs();
    }

    @Override
    public void saveConfig() {
        this.configManager.saveConfigs();
    }

    public static PlotSystemTerra getPlugin() {
        return plugin;
    }

    public static class DependencyManager {

        // List with all missing dependencies
        private final static List<String> missingDependencies = new ArrayList<>();

        /**
         * Check for all required dependencies and inform in console about missing dependencies
         * @return True if all dependencies are present
         */
        private static boolean checkForRequiredDependencies() {
            PluginManager pluginManager = plugin.getServer().getPluginManager();

            if (!pluginManager.isPluginEnabled("WorldEdit")) {
                missingDependencies.add("WorldEdit (V6.1.9)");
            }

            if (!pluginManager.isPluginEnabled("WorldGuard")) {
                missingDependencies.add("WorldGuard (V6.2.2)");
            }

            if (!pluginManager.isPluginEnabled("HeadDatabase")) {
                missingDependencies.add("HeadDatabase");
            }

            return missingDependencies.isEmpty();
        }

        /**
         * @return World Edit instance
         */
        public static WorldEdit getWorldEdit() {
            return WorldEdit.getInstance();
        }

        /**
         * @return World Edit Plugin
         */
        public static WorldEditPlugin getWorldEditPlugin() {
            return (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
        }
    }
}
