/*
 *  The MIT License (MIT)
 *
 *  Copyright © 2021-2025, Alps BTE <bte.atchli@gmail.com>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.alpsbte.plotsystemterra;

import com.alpsbte.alpslib.io.YamlFileFactory;
import com.alpsbte.alpslib.io.config.ConfigNotImplementedException;
import com.alpsbte.alpslib.io.database.DatabaseConfigPaths;
import com.alpsbte.alpslib.io.database.DatabaseConnection;
import com.alpsbte.alpslib.utils.head.AlpsHeadEventListener;
import com.alpsbte.plotsystemterra.commands.CMD_CreatePlot;
import com.alpsbte.plotsystemterra.commands.CMD_PastePlot;
import com.alpsbte.plotsystemterra.commands.CMD_PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.api.ApiConstants;
import com.alpsbte.plotsystemterra.core.api.DataProviderAPI;
import com.alpsbte.plotsystemterra.core.config.ConfigPaths;
import com.alpsbte.plotsystemterra.core.config.ConfigUtil;
import com.alpsbte.plotsystemterra.core.config.DataMode;
import com.alpsbte.plotsystemterra.core.data.DataProvider;
import com.alpsbte.plotsystemterra.core.database.DataProviderSQL;
import com.alpsbte.plotsystemterra.core.plotsystem.PlotPaster;
import com.alpsbte.plotsystemterra.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.ipvp.canvas.MenuFunctionListener;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

public class PlotSystemTerra extends JavaPlugin {
    private static PlotSystemTerra plugin;
    private static DataProvider dataProvider;
    private PlotPaster plotPaster;

    private boolean pluginEnabled = false;

    @Override
    public void onEnable() {
        plugin = this;

        Component successPrefix = text("[", DARK_GRAY)
                .append(text("✔", DARK_GREEN))
                .append(text("] ", DARK_GRAY))
                .color(GRAY);
        Component errorPrefix = text("[", DARK_GRAY)
                .append(text("X", RED))
                .append(text("] ", DARK_GRAY))
                .color(GRAY);

        // Init Config
        try {
            YamlFileFactory.registerPlugin(this);
            ConfigUtil.init();
        } catch (ConfigNotImplementedException ex) {
            getComponentLogger().warn(text("Could not load configuration file.")
                    .append(text("The config file must be configured!", YELLOW)), ex);
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
                DatabaseConnection.initializeDatabase(DatabaseConfigPaths.getConfig(getConfig()), getComponentLogger());
                getComponentLogger().info(successPrefix.append(text("Successfully initialized database connection.")));
            }
        } catch (Exception ex) {
            getComponentLogger().error(errorPrefix.append(text("Could not initialize database connection.")), ex);
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
            getComponentLogger().info(successPrefix.append(text("Successfully registered event listeners.")));
        } catch (Exception ex) {
            getComponentLogger().error(errorPrefix.append(text("Could not register event listeners.")), ex);
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register commands
        try {
            Objects.requireNonNull(getCommand("createplot")).setExecutor(new CMD_CreatePlot());
            Objects.requireNonNull(getCommand("pasteplot")).setExecutor(new CMD_PastePlot());
            Objects.requireNonNull(getCommand("plotsystemterra")).setExecutor(new CMD_PlotSystemTerra());
            getComponentLogger().info(successPrefix.append(text("Successfully registered commands.")));
        } catch (Exception ex) {
            getComponentLogger().error(errorPrefix.append(text("Could not register commands.")), ex);
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Start checking for plots to paste
        plotPaster = new PlotPaster();
        plotPaster.start();

        pluginEnabled = true;
        getComponentLogger().info(text("Enabled Plot-System-Terra plugin. Made by Alps BTE - GitHub: https://github.com/AlpsBTE/Plot-System-Terra", DARK_GREEN));
    }

    @Override
    public void onDisable() {
        if (!pluginEnabled) {
            Bukkit.getConsoleSender().sendMessage(empty());
            Bukkit.getConsoleSender().sendMessage(text("Disabled plugin. Made by Alps BTE - GitHub: https://github.com/AlpsBTE/Plot-System-Terra", RED));
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

    public static PlotSystemTerra getPlugin() {
        return plugin;
    }

    public PlotPaster getPlotPaster() {
        return plotPaster;
    }
}
