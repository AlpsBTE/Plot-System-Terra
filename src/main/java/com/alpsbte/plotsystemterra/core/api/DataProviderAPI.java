package com.alpsbte.plotsystemterra.core.api;

import com.alpsbte.plotsystemterra.core.data.CityProjectData;
import com.alpsbte.plotsystemterra.core.data.CityProjectDataProvider;
import com.alpsbte.plotsystemterra.core.data.DataProvider;
import com.alpsbte.plotsystemterra.core.data.PlotDataProvider;

public class DataProviderAPI implements DataProvider {
    private final CityProjectDataProvider cityProjectDataProvider;
    private final PlotDataProvider plotDataProvider;
    private final CityProjectData cityProjectData;

    public DataProviderAPI(int expiryMinute) {
        // Initialize API constants
        ApiConstants.updateApiConstants();

        cityProjectDataProvider = new CityProjectDataProviderAPI();
        plotDataProvider = new PlotDataProviderAPI();
        cityProjectData = new CityProjectData(cityProjectDataProvider, expiryMinute);
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

    @Override
    public void shutdown() {
        // no need to shut down anything for API
    }
}
