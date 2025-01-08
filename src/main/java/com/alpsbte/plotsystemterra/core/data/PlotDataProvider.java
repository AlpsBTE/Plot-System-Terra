package com.alpsbte.plotsystemterra.core.data;

import com.alpsbte.plotsystemterra.core.model.Plot;

import java.util.List;
import java.util.UUID;

public interface PlotDataProvider {
    Plot getPlot(int id) throws DataException;
    int createPlot(String cityProjectId, String difficultyId, String outlineBounds, UUID createPlayerUUID, byte[] initialSchematic) throws DataException;
    void setPasted(int id) throws DataException;
    List<Plot> getPlotsToPaste() throws DataException;
}
