package com.alpsbte.plotsystemterra;

import com.alpsbte.alpslib.libpsterra.core.Connection;
import com.alpsbte.alpslib.libpsterra.core.PSTerraSetup;
import com.alpsbte.alpslib.libpsterra.core.config.ConfigManager;
import com.alpsbte.alpslib.libpsterra.core.plotsystem.PlotCreator;
import com.alpsbte.alpslib.libpsterra.core.plotsystem.PlotPaster;
import com.alpsbte.alpslib.libpsterra.utils.IUpdateReceiver;
import com.alpsbte.alpslib.libpsterra.utils.Updater;
import com.alpsbte.plotsystemterra.commands.CMD_PlotSystemTerra;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Level;

public class PlotSystemTerra extends JavaPlugin implements IUpdateReceiver{

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

    private PlotCreator plotCreator;

    @Override
    public void onEnable() {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog"); // Disable Logging
        plugin = this;
        version = getDescription().getVersion();
        String errorPrefix = ChatColor.DARK_GRAY + "[" + ChatColor.RED + "X" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;

        try {
            PSTerraSetup setup = PSTerraSetup.setupPlugin(this, version);
            this.connection = setup.connection;
            this.plotCreator = setup.plotCreator;
            this.plotPaster = setup.plotPaster;
            this.configManager = setup.configManager;
        } catch (Exception ex) {
            Bukkit.getConsoleSender().sendMessage(errorPrefix + "Error setting up PlotSystemTerra: " + ex.getMessage());
            Bukkit.getLogger().log(Level.SEVERE, ex.getMessage(), ex);

            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        plugin.getCommand("plotsystemterra").setExecutor(new CMD_PlotSystemTerra());

        String result = startUpdateChecker();
        Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "Update-Checker: " + result);

        
        Bukkit.getConsoleSender().sendMessage(" ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GREEN + "Enabled Plot-System-Terra plugin.");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "------------------------------------------------------");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "> " + ChatColor.GRAY + "Made by " + ChatColor.RED + "Alps BTE (AT/CH/LI)");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "> " + ChatColor.GRAY + "GitHub: " + ChatColor.WHITE + "https://github.com/AlpsBTE/Plot-System-Terra");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "------------------------------------------------------");


        pluginEnabled = true;
        
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
        updater = new Updater(this, SPIGOT_PROJECT_ID, this.getFile(), Updater.UpdateType.CHECK_DOWNLOAD, false, this);
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

    @Override
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

    public PlotCreator getPlotCreator() {
        return plotCreator;
    }
    
}
