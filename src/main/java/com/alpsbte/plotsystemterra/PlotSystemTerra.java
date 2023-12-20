package com.alpsbte.plotsystemterra;

import com.alpsbte.plotsystemterra.commands.CMD_CreatePlot;
import com.alpsbte.plotsystemterra.commands.CMD_PastePlot;
import com.alpsbte.plotsystemterra.commands.CMD_PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.Connection;
import com.alpsbte.plotsystemterra.core.DatabaseConnection;
import com.alpsbte.plotsystemterra.core.EventListener;
import com.alpsbte.plotsystemterra.core.NetworkAPIConnection;
import com.alpsbte.plotsystemterra.core.config.ConfigManager;
import com.alpsbte.plotsystemterra.core.config.ConfigNotImplementedException;
import com.alpsbte.plotsystemterra.core.config.ConfigPaths;
import com.alpsbte.plotsystemterra.core.config.DataMode;
import com.alpsbte.plotsystemterra.core.plotsystem.PlotPaster;
import com.alpsbte.plotsystemterra.utils.Updater;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.ipvp.canvas.MenuFunctionListener;


import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.logging.Level;

public class PlotSystemTerra extends JavaPlugin {

    public static int SPIGOT_PROJECT_ID = 105323;

    private static PlotSystemTerra plugin;
    private ConfigManager configManager;
    private PlotPaster plotPaster;

    private boolean pluginEnabled = false;
    public String version;
    public String newVersion;
    public boolean updateInstalled = false;
    public Updater updater;

    private Connection connection;

    @Override
    public void onEnable() {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog"); // Disable Logging
        plugin = this;
        version = getDescription().getVersion();

        String successPrefix = ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GREEN + "✔" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;
        String errorPrefix = ChatColor.DARK_GRAY + "[" + ChatColor.RED + "X" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;

        Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "--------------- Plot-System-Terra V" + version + " ----------------");
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
            FileConfiguration configFile = getConfig();

            if(configFile.getString(ConfigPaths.DATA_MODE).equalsIgnoreCase(DataMode.DATABASE.toString())){

                String URL = configFile.getString(ConfigPaths.DATABASE_URL);
                String name = configFile.getString(ConfigPaths.DATABASE_NAME);
                String username = configFile.getString(ConfigPaths.DATABASE_USERNAME);
                String password = configFile.getString(ConfigPaths.DATABASE_PASSWORD);
                String teamApiKey = configFile.getString(ConfigPaths.API_KEY);
                
                connection = new DatabaseConnection(URL, name, username, password, teamApiKey);// DatabaseConnection.InitializeDatabase();
                Bukkit.getConsoleSender().sendMessage(successPrefix + "Successfully initialized database connection.");
            }else{
                String teamApiKey = configFile.getString(ConfigPaths.API_KEY);
                String apiHost = configFile.getString(ConfigPaths.API_URL);
                
                int apiPort = configFile.getInt(ConfigPaths.API_KEY);

                connection = new NetworkAPIConnection(apiHost, apiPort, teamApiKey);
                // String name = configFile.getString(ConfigPaths.DATABASE_NAME);
                // String username = configFile.getString(ConfigPaths.DATABASE_USERNAME);
                // String password = configFile.getString(ConfigPaths.DATABASE_PASSWORD);

            }


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
            this.getCommand("plotsystemterra").setExecutor(new CMD_PlotSystemTerra());
            Bukkit.getConsoleSender().sendMessage(successPrefix + "Successfully registered commands.");
        } catch (Exception ex) {
            Bukkit.getConsoleSender().sendMessage(errorPrefix + "Could not register commands.");
            Bukkit.getLogger().log(Level.SEVERE, ex.getMessage(), ex);

            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Check for updates
        Bukkit.getConsoleSender().sendMessage(" ");

        String result = startUpdateChecker();
        Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "Update-Checker: " + result);


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

    private String startUpdateChecker(){
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                checkForUpdates();
            }
        }, 20*60*60,20*60*60);

        return checkForUpdates();
    }

    public String checkForUpdates(){
        updater = new Updater(this, SPIGOT_PROJECT_ID, this.getFile(), Updater.UpdateType.CHECK_DOWNLOAD, false);
        Updater.Result result = updater.getResult();

        String resultMessage = "";
        switch (result){
            case BAD_ID: resultMessage = "Failed to update the plugin: Wrong Spigot ID."; break;
            case FAILED: resultMessage = "Failed to update the plugin."; break;
            case NO_UPDATE: resultMessage = "The plugin is up to date."; break;
            case SUCCESS: resultMessage = "Plugin successfully updated to version " + updater.getVersion() + "."; break;
            case UPDATE_FOUND: resultMessage = "Found an update (v" + updater.getVersion() + ") for the plugin."; break;
            default: resultMessage = "No result for update search"; break;
        }

        return resultMessage;
    }

    public void setUpdateInstalled(String newVersion) {
        this.newVersion = newVersion;

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                Updater.notifyUpdate(newVersion);
            }
        }, 20*5);

    }


    public static PlotSystemTerra getPlugin() {
        return plugin;
    }

    public Connection getConnection(){
        return connection;
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
}
