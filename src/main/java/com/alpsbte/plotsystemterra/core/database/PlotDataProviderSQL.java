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

import com.alpsbte.alpslib.io.database.DatabaseConnection;
import com.alpsbte.plotsystemterra.core.data.DataException;
import com.alpsbte.plotsystemterra.core.data.PlotDataProvider;
import com.alpsbte.plotsystemterra.core.model.Plot;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlotDataProviderSQL implements PlotDataProvider {
    @Override
    public Plot getPlot(int id) throws DataException {
        try (PreparedStatement ps = Objects.requireNonNull(DatabaseConnection.getConnection()).prepareStatement("SELECT status, city_project_id, plot_version, mc_version FROM plot WHERE plot_id = ?")) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;

            String status = rs.getString(1);
            String cityProjectId = rs.getString(2);
            double plotVersion = rs.getDouble(3);
            String mcVersion = rs.getString(4);

            return new Plot(
                    id,
                    status,
                    cityProjectId,
                    plotVersion,
                    mcVersion
            );
        } catch (SQLException e) {
            throw new DataException(e.getMessage(), e);
        }
    }

    @Override
    public CompletableFuture<Plot> getPlotAsync(int id) throws DataException {
        CompletableFuture<Plot> completableFuture = new CompletableFuture<>();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                completableFuture.complete(getPlot(id));
                return null;
            });
        }
        return completableFuture;
    }

    @Override
    public int createPlot(String cityProjectId, String difficultyId, String outlineBounds, @NotNull UUID createPlayerUUID, byte[] initialSchematic) throws DataException {
        int createdPlotId;
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
                connection.setAutoCommit(false);

                try (PreparedStatement stmt = Objects.requireNonNull(connection).prepareStatement("INSERT INTO plot (city_project_id, difficulty_id, outline_bounds, initial_schematic, plot_version, created_by)" +
                        "VALUES (?, ?, ?, ?, (SELECT si.current_plot_version FROM system_info si WHERE system_id = 1), ?)", Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, cityProjectId);
                    stmt.setString(2, difficultyId);
                    stmt.setString(3, outlineBounds);
                    stmt.setBytes(4, initialSchematic);
                    stmt.setString(5, createPlayerUUID.toString());

                    stmt.executeUpdate();

                    // Get the id of the new plot
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            createdPlotId = rs.getInt(1);
                        } else throw new DataException("Could not obtain generated key");
                    }
                }

            // Finalize database transaction
            connection.commit();
        } catch (SQLException | DataException e) {

            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    throw new DataException("Failed to rollback transaction: " + rollbackEx.getMessage(), rollbackEx);
                }
            }

            throw new DataException(e.getMessage(), e);
        }
        return createdPlotId;
    }

    @Override
    public CompletableFuture<Integer> createPlotAsync(String cityProjectId, String difficultyId, String outlineBounds, UUID createPlayerUUID, byte[] initialSchematic) throws DataException {
        CompletableFuture<Integer> completableFuture = new CompletableFuture<>();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                completableFuture.complete(createPlot(cityProjectId, difficultyId, outlineBounds, createPlayerUUID, initialSchematic));
                return null;
            });
        }
        return completableFuture;
    }

    @Override
    public void setPasted(int id) throws DataException {
        try (PreparedStatement ps = Objects.requireNonNull(DatabaseConnection.getConnection()).prepareStatement("UPDATE plot SET is_pasted = '1' WHERE plot_id = ?")){
            ps.setInt(1, id);
        } catch (SQLException e) {
            throw new DataException(e.getMessage(), e);
        }
    }

    @Override
    public CompletableFuture<Void> setPastedAsync(int id) throws DataException {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                setPasted(id);
                completableFuture.complete(null);
                return null;
            });
        }
        return completableFuture;
    }

    @Override
    public List<Plot> getPlotsToPaste() throws DataException {
        List<Plot> plots = new ArrayList<>();

        try (var s = DatabaseConnection.getConnection().createStatement()) {
            ResultSet rs = s.executeQuery("SELECT plot_id, status, city_project_id, plot_version, mc_version, complete_schematic FROM plot WHERE status = 'completed' AND is_pasted = '0' LIMIT 20");
            if (!rs.next()) return plots;

            int id = rs.getInt(1);
            String status = rs.getString(2);
            String cityProjectId = rs.getString(3);
            double plotVersion = rs.getDouble(4);
            String mcVersion = rs.getString(5);
            byte[] completedSchematic = rs.getBytes(6);

            plots.add(new Plot(
               id, status, cityProjectId, plotVersion, mcVersion, completedSchematic
            ));

        } catch (SQLException e) {
            throw new DataException(e.getMessage(), e);
        }
        return plots;
    }

    @Override
    public CompletableFuture<List<Plot>> getPlotsToPasteAsync() throws DataException {
        CompletableFuture<List<Plot>> completableFuture = new CompletableFuture<>();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                completableFuture.complete(getPlotsToPaste());
                return null;
            });
        }
        return completableFuture;
    }
}
