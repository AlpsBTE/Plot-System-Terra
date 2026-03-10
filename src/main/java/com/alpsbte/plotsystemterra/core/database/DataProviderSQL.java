package com.alpsbte.plotsystemterra.core.database;

import com.alpsbte.alpslib.io.database.DatabaseConfigPaths;
import com.alpsbte.alpslib.io.database.DatabaseConnection;
import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.data.CityProjectData;
import com.alpsbte.plotsystemterra.core.data.CityProjectDataProvider;
import com.alpsbte.plotsystemterra.core.data.DataProvider;
import com.alpsbte.plotsystemterra.core.data.PlotDataProvider;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.text;

public class DataProviderSQL implements DataProvider {
    private final CityProjectDataProvider cityProjectDataProvider;
    private final PlotDataProvider plotDataProvider;
    private final CityProjectData cityProjectData;

    public DataProviderSQL(@NotNull PlotSystemTerra plugin, @NotNull Component successPrefix, int expiryMinute) throws ClassNotFoundException {
        // Initialize database connection
        DatabaseConnection.initializeDatabase(DatabaseConfigPaths.getConfig(plugin.getConfig()), true);
        plugin.getComponentLogger().info(successPrefix.append(text("Successfully initialized database connection.")));

        cityProjectDataProvider = new CityProjectDataProviderSQL();
        plotDataProvider = new PlotDataProviderSQL();
        cityProjectData = new CityProjectData(cityProjectDataProvider, expiryMinute);
    }

    @Override
    public void shutdown() {
        // Close database connection
        DatabaseConnection.shutdown();
    }

    @Override
    public CityProjectDataProvider getCityProjectDataProvider() {
        return cityProjectDataProvider;
    }

    @Override
    public PlotDataProvider getPlotDataProvider() {
        return plotDataProvider;
    }

    @Override
    public CityProjectData getCityProjectData() {
        return cityProjectData;
    }
}
