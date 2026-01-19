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
