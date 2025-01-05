package com.alpsbte.plotsystemterra;

import com.alpsbte.alpslib.io.YamlFileFactory;
import com.alpsbte.alpslib.io.config.ConfigNotImplementedException;
import com.alpsbte.alpslib.utils.head.AlpsHeadEventListener;
import com.alpsbte.plotsystemterra.commands.CMD_CreatePlot;
import com.alpsbte.plotsystemterra.commands.CMD_PastePlot;
import com.alpsbte.plotsystemterra.commands.CMD_PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.DatabaseConnection;
import com.alpsbte.plotsystemterra.core.config.ConfigPaths;
import com.alpsbte.plotsystemterra.core.config.ConfigUtil;
import com.alpsbte.plotsystemterra.core.config.DataMode;
import com.alpsbte.plotsystemterra.core.data.DataProvider;
import com.alpsbte.plotsystemterra.core.database.DataProviderSQL;
import com.alpsbte.plotsystemterra.core.plotsystem.PlotPaster;
import com.alpsbte.plotsystemterra.utils.Updater;
import com.alpsbte.plotsystemterra.utils.Utils;
import com.sk89q.worldedit.WorldEdit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.ipvp.canvas.MenuFunctionListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class PlotSystemTerra extends JavaPlugin {

    public static int SPIGOT_PROJECT_ID = 105323;

    private static PlotSystemTerra plugin;
    private static DataProvider dataProvider;
    private PlotPaster plotPaster;

    private boolean pluginEnabled = false;
    public String version;
    public String newVersion;
    public Updater updater;

    @Override
    public void onEnable() {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog"); // Disable Logging
        plugin = this;
        version = getPluginMeta().getVersion();

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

        // Init Config
        try {
            YamlFileFactory.registerPlugin(this);
            ConfigUtil.init();
        } catch (ConfigNotImplementedException ex) {
            Bukkit.getLogger().log(Level.WARNING, "Could not load configuration file.");
            Bukkit.getConsoleSender().sendMessage(Component.text("The config file must be configured!", NamedTextColor.YELLOW));
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        reloadConfig();

        // Initialize database connection
        try {
            FileConfiguration configFile = PlotSystemTerra.getPlugin().getConfig();

            if(configFile.getString(ConfigPaths.DATA_MODE).equalsIgnoreCase(DataMode.DATABASE.toString())){
                DatabaseConnection.InitializeDatabase();
                Bukkit.getConsoleSender().sendPlainMessage(successPrefix + "Successfully initialized database connection.");
            }



        } catch (Exception ex) {
            Bukkit.getConsoleSender().sendMessage(errorPrefix + "Could not initialize database connection.");
            Bukkit.getLogger().log(Level.SEVERE, ex.getMessage(), ex);

            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Set data provider
        //TODO: get from config
        dataProvider = new DataProviderSQL();

        // Register event listeners
        try {
            this.getServer().getPluginManager().registerEvents(new MenuFunctionListener(), this);
            this.getServer().getPluginManager().registerEvents(new AlpsHeadEventListener(), this);
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
    public @NotNull FileConfiguration getConfig() {
        return ConfigUtil.getInstance().configs[0];
    }

    @Override
    public void reloadConfig() {
        ConfigUtil.getInstance().reloadFiles();
        ConfigUtil.getInstance().saveFiles();
        Utils.ChatUtils.setChatFormat(getConfig().getString(ConfigPaths.CHAT_FORMAT_INFO_PREFIX),
                getConfig().getString(ConfigPaths.CHAT_FORMAT_ALERT_PREFIX));
    }

    @Override
    public void saveConfig() {
        ConfigUtil.getInstance().saveFiles();
    }


    public static DataProvider getDataProvider() {
        return dataProvider;
    }

    private String startUpdateChecker(){
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::checkForUpdates, 20*60*60,20*60*60);
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

            if (!pluginManager.isPluginEnabled("FastAsyncWorldEdit")) {
                missingDependencies.add("FastAsyncWorldEdit");
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
    }
}
