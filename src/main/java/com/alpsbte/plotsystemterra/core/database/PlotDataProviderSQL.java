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

import com.alpsbte.alpslib.io.database.SqlHelper;
import com.alpsbte.plotsystemterra.core.data.DataException;
import com.alpsbte.plotsystemterra.core.data.PlotDataProvider;
import com.alpsbte.plotsystemterra.core.model.Plot;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlotDataProviderSQL extends PlotDataProvider {

    @Override
    public Plot getPlot(int id) throws DataException {
        String queryGetPlot = "SELECT status, city_project_id, plot_version, mc_version FROM plot WHERE plot_id = ?";

        return SqlExceptionUtil.handle(() -> SqlHelper.runQuery(queryGetPlot, ps -> {
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
        }));
    }

    @Override
    public int createPlot(String cityProjectId, String difficultyId, String outlineBounds,
                          @NotNull UUID createPlayerUUID, byte[] initialSchematic) throws DataException {

        String queryInsert = "INSERT INTO plot (city_project_id, difficulty_id, outline_bounds, initial_schematic, plot_version, created_by)" +
                "VALUES (?, ?, ?, ?, (SELECT si.current_plot_version FROM system_info si WHERE system_id = 1), ?)";

        return SqlExceptionUtil.handle(() -> SqlHelper.runInsertQuery(queryInsert, ps -> {
            ps.setString(1, cityProjectId);
            ps.setString(2, difficultyId);
            ps.setString(3, outlineBounds);
            ps.setBytes(4, initialSchematic);
            ps.setString(5, createPlayerUUID.toString());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DataException("Creating plot failed, no rows affected.");
            }

            // Get the id of the new plot
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new DataException("Could not obtain generated key");
            }
        }));
    }

    @Override
    public void setPasted(int id) throws DataException {
        String queryUpdatePasted = "UPDATE plot SET is_pasted = '1' WHERE plot_id = ?";

        SqlExceptionUtil.handle(() -> SqlHelper.runStatement(queryUpdatePasted, ps -> ps.setInt(1, id)));
    }

    @Override
    public List<Plot> getPlotsToPaste() throws DataException {
        String queryPlotsToPaste = "SELECT plot_id, status, city_project_id, plot_version, mc_version, " +
                "complete_schematic FROM plot WHERE status = 'completed' AND is_pasted = '0' LIMIT 20";

        return SqlExceptionUtil.handle(() -> SqlHelper.runQuery(queryPlotsToPaste, ps -> {
            List<Plot> plots = new ArrayList<>();
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt(1);
                String status = rs.getString(2);
                String cityProjectId = rs.getString(3);
                double plotVersion = rs.getDouble(4);
                String mcVersion = rs.getString(5);
                byte[] completedSchematic = rs.getBytes(6);

                plots.add(new Plot(id, status, cityProjectId, plotVersion, mcVersion, completedSchematic));
            }
            return plots;
        }));
    }
}
