package github.alpsbte;

import github.alpsbte.commands.CMD_CreatePlot;
import github.alpsbte.core.DatabaseConnection;
import github.alpsbte.core.EventListener;
import github.alpsbte.core.config.ConfigManager;
import github.alpsbte.core.config.ConfigNotImplementedException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.ipvp.canvas.MenuFunctionListener;

import java.util.logging.Level;

public class PlotSystemTerra extends JavaPlugin {

    private static PlotSystemTerra plugin;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        plugin = this;

        try {
            configManager = new ConfigManager();
        } catch (ConfigNotImplementedException ex) {
            return;
        }

        reloadConfig();

        // Initialize database
        DatabaseConnection.InitializeDatabase();

        // Add listeners
        this.getServer().getPluginManager().registerEvents(new EventListener(), plugin);
        this.getServer().getPluginManager().registerEvents(new MenuFunctionListener(), plugin);

        this.getCommand("createplot").setExecutor(new CMD_CreatePlot());

        getLogger().log(Level.INFO, "Successfully enabled Plot-System-Terra plugin.");
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
}
