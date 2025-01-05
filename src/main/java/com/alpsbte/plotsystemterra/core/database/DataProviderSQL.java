package com.alpsbte.plotsystemterra.core.database;

import com.alpsbte.plotsystemterra.core.data.CityProjectDataProvider;
import com.alpsbte.plotsystemterra.core.data.DataProvider;
import com.alpsbte.plotsystemterra.core.data.PlotDataProvider;

public class DataProviderSQL implements DataProvider {
    private final CityProjectDataProvider cityProjectDataProvider;
    private final PlotDataProvider plotDataProvider;
    public DataProviderSQL() {
        cityProjectDataProvider = new CityProjectDataProviderSQL();
        plotDataProvider = new PlotDataProviderSQL();
    }

    @Override
    public CityProjectDataProvider getCityProjectDataProvider() {
        return cityProjectDataProvider;
    }

    @Override
    public PlotDataProvider getPlotDataProvider() {
        return plotDataProvider;
    }
}
