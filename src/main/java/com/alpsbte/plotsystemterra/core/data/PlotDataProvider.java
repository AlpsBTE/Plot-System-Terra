package com.alpsbte.plotsystemterra.core.data;

import com.alpsbte.plotsystemterra.core.model.Plot;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlotDataProvider {
    Plot getPlot(int id) throws DataException;
    CompletableFuture<Plot> getPlotAsync(int id) throws DataException;

    int createPlot(String cityProjectId, String difficultyId, String outlineBounds, UUID createPlayerUUID, byte[] initialSchematic) throws DataException;
    CompletableFuture<Integer> createPlotAsync(String cityProjectId, String difficultyId, String outlineBounds, UUID createPlayerUUID, byte[] initialSchematic) throws DataException;

    void setPasted(int id) throws DataException;
    CompletableFuture<Void> setPastedAsync(int id) throws DataException;

    List<Plot> getPlotsToPaste() throws DataException;
    CompletableFuture<List<Plot>> getPlotsToPasteAsync() throws DataException;
}
