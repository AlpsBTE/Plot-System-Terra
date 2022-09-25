package com.alpsbte.plotsystemterra;

import com.alpsbte.plotsystemterra.commands.CMD_CreatePlot;
import com.alpsbte.plotsystemterra.commands.CMD_PastePlot;
import com.alpsbte.plotsystemterra.core.DatabaseConnection;
import com.alpsbte.plotsystemterra.core.EventListener;
import com.alpsbte.plotsystemterra.core.config.ConfigManager;
import com.alpsbte.plotsystemterra.core.config.ConfigNotImplementedException;
import com.alpsbte.plotsystemterra.core.plotsystem.PlotPaster;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.ipvp.canvas.MenuFunctionListener;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.logging.Level;

public class PlotSystemTerra extends JavaPlugin {

    private static final String VERSION = "3.0";

    private static PlotSystemTerra plugin;
    private ConfigManager configManager;
    private PlotPaster plotPaster;

    private boolean pluginEnabled = false;

    @Override
    public void onEnable() {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog"); // Disable Logging
        plugin = this;

        String successPrefix = ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GREEN + "✔" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;
        String errorPrefix = ChatColor.DARK_GRAY + "[" + ChatColor.RED + "X" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;

        Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "--------------- Plot-System-Terra V" + VERSION + " ----------------");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GREEN + "Starting plugin...");
        Bukkit.getConsoleSender().sendMessage(" ");

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
            this.getCommand("pasteplot").setExecutor(new CMD_PastePlot());
            Bukkit.getConsoleSender().sendMessage(successPrefix + "Successfully registered commands.");
        } catch (Exception ex) {
            Bukkit.getConsoleSender().sendMessage(errorPrefix + "Could not register commands.");
            Bukkit.getLogger().log(Level.SEVERE, ex.getMessage(), ex);

            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Check for updates
        Bukkit.getConsoleSender().sendMessage(" ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "Update-Checker:");

        UpdateChecker.getVersion(version -> {
            if (version.equalsIgnoreCase(VERSION)) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "You are using the latest stable version.");
            } else {
                UpdateChecker.isUpdateAvailable = true;
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "You are using a outdated version!");
                Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "Latest version: " + ChatColor.GREEN + version + ChatColor.GRAY + " | Your version: " + ChatColor.RED + VERSION);
                Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "Update here: " + ChatColor.AQUA + "https://github.com/AlpsBTE/Plot-System/releases");
            }
        });

        // Start checking for plots to paste
        plotPaster = new PlotPaster();
        plotPaster.start();

        pluginEnabled = true;
        Bukkit.getConsoleSender().sendMessage(" ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GREEN + "Enabled Plot-System-Terra plugin.");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "------------------------------------------------------");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "> " + ChatColor.GRAY + "Made by " + ChatColor.RED + "Alps BTE (AT/CH/LI)");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "> " + ChatColor.GRAY + "GitHub: " + ChatColor.WHITE + "https://github.com/AlpsBTE/Plot-System-Terra");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "------------------------------------------------------");
    }

    @Override
    public void onDisable() {
        if (!pluginEnabled) {
            Bukkit.getConsoleSender().sendMessage(" ");
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Disabling plugin...");
            Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "------------------------------------------------------");
            Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "> " + ChatColor.GRAY + "Made by " + ChatColor.RED + "Alps BTE (AT/CH/LI)");
            Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "> " + ChatColor.GRAY + "GitHub: " + ChatColor.WHITE + "https://github.com/AlpsBTE/Plot-System-Terra");
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

    public PlotPaster getPlotPaster() {
        return plotPaster;
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

    public static class UpdateChecker {
        private final static int RESOURCE_ID = 95921;
        private static boolean isUpdateAvailable = false;

        /**
         * Get latest plugin version from SpigotMC
         * @param version Returns latest stable version
         */
        public static void getVersion(final Consumer<String> version) {
            try (InputStream inputStream = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + RESOURCE_ID).openStream(); Scanner scanner = new Scanner(inputStream)) {
                if (scanner.hasNext()) {
                    version.accept(scanner.next());
                }
            } catch (IOException ex) {
                Bukkit.getLogger().log(Level.WARNING, "Cannot look for new updates: " + ex.getMessage());
            }
        }

        /**
         * @return True if an update is available
         */
        public static boolean updateAvailable() {
            return isUpdateAvailable;
        }
    }
}
