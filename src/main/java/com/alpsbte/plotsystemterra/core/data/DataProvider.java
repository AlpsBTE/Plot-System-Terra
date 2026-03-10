package com.alpsbte.plotsystemterra.core.data;

public interface DataProvider {
    CityProjectDataProvider getCityProjectDataProvider();
    PlotDataProvider getPlotDataProvider();
    CityProjectData getCityProjectData();

    void shutdown();
}
