package com.alpsbte.plotsystemterra;

import com.alpsbte.alpslib.io.YamlFileFactory;
import com.alpsbte.alpslib.io.config.ConfigNotImplementedException;
import com.alpsbte.alpslib.utils.head.AlpsHeadEventListener;
import com.alpsbte.plotsystemterra.commands.CMD_CreatePlot;
import com.alpsbte.plotsystemterra.commands.CMD_PastePlot;
import com.alpsbte.plotsystemterra.commands.CMD_PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.api.ApiConstants;
import com.alpsbte.plotsystemterra.core.api.DataProviderAPI;
import com.alpsbte.plotsystemterra.core.database.DatabaseConnection;
import com.alpsbte.plotsystemterra.core.config.ConfigPaths;
import com.alpsbte.plotsystemterra.core.config.ConfigUtil;
import com.alpsbte.plotsystemterra.core.config.DataMode;
import com.alpsbte.plotsystemterra.core.data.DataProvider;
import com.alpsbte.plotsystemterra.core.database.DataProviderSQL;
import com.alpsbte.plotsystemterra.core.plotsystem.CityProjectCache;
import com.alpsbte.plotsystemterra.core.plotsystem.PlotPaster;
import com.alpsbte.plotsystemterra.utils.Updater;
import com.alpsbte.plotsystemterra.utils.Utils;
import com.sk89q.worldedit.WorldEdit;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.ipvp.canvas.MenuFunctionListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class PlotSystemTerra extends JavaPlugin {

    public static final int SPIGOT_PROJECT_ID = 105323;

    private static PlotSystemTerra plugin;
    private static DataProvider dataProvider;
    private PlotPaster plotPaster;
    private CityProjectCache cache;

    private boolean pluginEnabled = false;
    public String version;
    public String newVersion;
    public Updater updater;

    @Override
    public void onEnable() {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog"); // Disable Logging
        plugin = this;

        // there is no better way to do this according to the paper devs
        //noinspection UnstableApiUsage
        version = getPluginMeta().getVersion();

        Component successPrefix = text("[", DARK_GRAY)
                .append(text("✔", DARK_GREEN))
                .append(text("] ", DARK_GRAY))
                .color(GRAY);
        Component errorPrefix = text("[", DARK_GRAY)
                .append(text("X", RED))
                .append(text("] ", DARK_GRAY))
                .color(GRAY);

        Bukkit.getConsoleSender().sendMessage(text("--------------- Plot-System-Terra V" + version + " ----------------", GOLD));
        Bukkit.getConsoleSender().sendMessage(text("Starting plugin...", DARK_GREEN));
        Bukkit.getConsoleSender().sendMessage(empty());

        // Check for required dependencies, if it returns false disable plugin
        if (!DependencyManager.checkForRequiredDependencies()) {
            Bukkit.getConsoleSender().sendMessage(errorPrefix.append(text("Could not load required dependencies.")));
            Bukkit.getConsoleSender().sendMessage(text("Missing Dependencies:", YELLOW));
            DependencyManager.missingDependencies.forEach(dependency -> Bukkit.getConsoleSender().sendMessage(text(" - " + dependency, YELLOW)));

            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Bukkit.getConsoleSender().sendMessage(successPrefix.append(text("Successfully loaded required dependencies.")));

        // Init Config
        try {
            YamlFileFactory.registerPlugin(this);
            ConfigUtil.init();
        } catch (ConfigNotImplementedException ex) {
            getComponentLogger().warn(text("Could not load configuration file."));
            Bukkit.getConsoleSender().sendMessage(text("The config file must be configured!", YELLOW));
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        reloadConfig();

        // Get DataMode
        FileConfiguration configFile = PlotSystemTerra.getPlugin().getConfig();
        DataMode dataMode = DataMode.NONE;
        try {
            dataMode = DataMode.valueOf(configFile.getString(ConfigPaths.DATA_MODE));
        } catch (IllegalArgumentException e) {
            getComponentLogger().error(text("invalid DataMode!"), e);
            getServer().getPluginManager().disablePlugin(this);
        }

        // Initialize database connection
        try {
            if (dataMode == DataMode.DATABASE) {
                DatabaseConnection.InitializeDatabase();
                Bukkit.getConsoleSender().sendMessage(successPrefix.append(text("Successfully initialized database connection.")));
            }
        } catch (Exception ex) {
            Bukkit.getConsoleSender().sendMessage(errorPrefix.append(text("Could not initialize database connection.")));
            getComponentLogger().error(text(ex.getMessage()), ex);
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize API constants
        ApiConstants.updateApiConstants();

        // Set data provider
        switch (dataMode) {
            case DATABASE -> dataProvider = new DataProviderSQL();
            case API -> dataProvider = new DataProviderAPI();
            default -> {
                getComponentLogger().error(text("No Data Provider has been set! Disabling plugin..."));
                this.getServer().getPluginManager().disablePlugin(this);
            }
        }

        // Register event listeners
        try {
            this.getServer().getPluginManager().registerEvents(new MenuFunctionListener(), this);
            this.getServer().getPluginManager().registerEvents(new AlpsHeadEventListener(), this);
            Bukkit.getConsoleSender().sendMessage(successPrefix.append(text("Successfully registered event listeners.")));
        } catch (Exception ex) {
            Bukkit.getConsoleSender().sendMessage(errorPrefix.append(text("Could not register event listeners.")));
            getComponentLogger().error(text(ex.getMessage()), ex);

            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register commands
        try {
            Objects.requireNonNull(getCommand("createplot")).setExecutor(new CMD_CreatePlot());
            Objects.requireNonNull(getCommand("pasteplot")).setExecutor(new CMD_PastePlot());
            Objects.requireNonNull(getCommand("plotsystemterra")).setExecutor(new CMD_PlotSystemTerra());
            Bukkit.getConsoleSender().sendMessage(successPrefix.append(text("Successfully registered commands.")));
        } catch (Exception ex) {
            Bukkit.getConsoleSender().sendMessage(errorPrefix.append(text("Could not register commands.")));
            getComponentLogger().error(text(ex.getMessage()), ex);

            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Check for updates
        if (ConfigUtil.getInstance().configs[0].getBoolean(ConfigPaths.CHECK_FOR_UPDATES)) {
            Bukkit.getConsoleSender().sendMessage(empty());
            String result = startUpdateChecker();
            Bukkit.getConsoleSender().sendMessage(text("Update-Checker: " + result, GOLD));
        }

        // Start checking for plots to paste
        plotPaster = new PlotPaster();
        plotPaster.start();

        this.cache = new CityProjectCache(this);

        pluginEnabled = true;
        Bukkit.getConsoleSender().sendMessage(empty());
        Bukkit.getConsoleSender().sendMessage(text("Enabled Plot-System-Terra plugin.", DARK_GREEN));
        Bukkit.getConsoleSender().sendMessage(text("------------------------------------------------------", GOLD));
        Bukkit.getConsoleSender().sendMessage(text("> ", DARK_GRAY).append(text("Made by ", GRAY)).append(text("Alps BTE (AT/CH/LI)", RED)));
        Bukkit.getConsoleSender().sendMessage(text("> ", DARK_GRAY).append(text("GitHub: ", GRAY)).append(text("https://github.com/AlpsBTE/Plot-System-Terra", WHITE)));
        Bukkit.getConsoleSender().sendMessage(text("------------------------------------------------------", GOLD));
    }

    @Override
    public void onDisable() {
        if (!pluginEnabled) {
            Bukkit.getConsoleSender().sendMessage(empty());
            Bukkit.getConsoleSender().sendMessage(text("Disabling plugin...", RED));
            Bukkit.getConsoleSender().sendMessage(text("------------------------------------------------------", GOLD));
            Bukkit.getConsoleSender().sendMessage(text("> ", DARK_GRAY).append(text("Made by ", GRAY)).append(text("Alps BTE (AT/CH/LI)", RED)));
            Bukkit.getConsoleSender().sendMessage(text("> ", DARK_GRAY).append(text("GitHub: ", GRAY)).append(text("https://github.com/AlpsBTE/Plot-System-Terra", WHITE)));
            Bukkit.getConsoleSender().sendMessage(text("------------------------------------------------------", GOLD));
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

    public CityProjectCache getCache() {
        return cache;
    }

    private String startUpdateChecker() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::checkForUpdates, 20 * 60 * 60, 20 * 60 * 60);
        return checkForUpdates();
    }

    public String checkForUpdates() {
        updater = new Updater(this, SPIGOT_PROJECT_ID, this.getFile(), Updater.UpdateType.CHECK_DOWNLOAD, false);
        Updater.Result result = updater.getResult();

        return switch (result) {
            case BAD_ID -> "Failed to update the plugin: Wrong Spigot ID.";
            case FAILED -> "Failed to update the plugin.";
            case NO_UPDATE -> "The plugin is up to date.";
            case SUCCESS -> "Plugin successfully updated to version " + updater.getVersion() + ".";
            case UPDATE_FOUND -> "Found an update (v" + updater.getVersion() + ") for the plugin.";
        };
    }

    public void setUpdateInstalled(String newVersion) {
        this.newVersion = newVersion;

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> Updater.notifyUpdate(newVersion), 20 * 5);

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
         *
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
