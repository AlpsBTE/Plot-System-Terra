package com.alpsbte.plotsystemterra.core.data;

import com.alpsbte.plotsystemterra.core.model.Plot;

import java.util.UUID;

public interface PlotDataProvider {
    Plot getPlot(int id) throws DataException;
    int createPlot(int cityProjectId, int difficulty, String mcCoordinates, String outline, UUID createPlayerUUID) throws DataException;
}
