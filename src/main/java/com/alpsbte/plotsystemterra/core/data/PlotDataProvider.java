package com.alpsbte.plotsystemterra.core.data;

import com.alpsbte.plotsystemterra.core.model.Plot;

import java.util.List;
import java.util.UUID;

public interface PlotDataProvider {
    public abstract Plot getPlot(int id) throws DataException;

    public abstract int createPlot(String cityProjectId, String difficultyId, String outlineBounds, UUID createPlayerUUID, byte[] initialSchematic) throws DataException;

    public abstract void setPasted(int id) throws DataException;

    public abstract List<Plot> getPlotsToPaste() throws DataException;
}
