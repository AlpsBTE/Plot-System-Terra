package com.alpsbte.plotsystemterra.core.api;

import com.alpsbte.plotsystemterra.core.data.CityProjectDataProvider;
import com.alpsbte.plotsystemterra.core.data.DataProvider;
import com.alpsbte.plotsystemterra.core.data.PlotDataProvider;

public class DataProviderAPI implements DataProvider {
    private final CityProjectDataProvider cityProjectDataProvider;
    private final PlotDataProvider plotDataProvider;

    public DataProviderAPI() {
        cityProjectDataProvider = new CityProjectDataProviderAPI();
        plotDataProvider = new PlotDataProviderAPI();
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
