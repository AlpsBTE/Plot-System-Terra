package com.alpsbte.plotsystemterra.core.database;

import com.alpsbte.plotsystemterra.core.data.DataException;
import com.alpsbte.plotsystemterra.core.data.PlotDataProvider;
import com.alpsbte.plotsystemterra.core.model.Plot;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.alpsbte.plotsystemterra.core.plotsystem.PlotCreator.PLOT_VERSION;

public class PlotDataProviderSQL implements PlotDataProvider {
    @Override
    public Plot getPlot(int id) throws DataException {
        try (ResultSet rs = DatabaseConnection.createStatement("SELECT status, city_project_id, plot_version, mc_version FROM plot WHERE id = ?")
                .setValue(id).executeQuery()) {

            if (!rs.next()) return null;

            String status = rs.getString(1);
            String cityProjectId = rs.getString(2);
            double plotVersion = rs.getDouble(3);
            String mcVersion = rs.getString(4);

            DatabaseConnection.closeResultSet(rs);

            return new Plot(
                    id,
                    status,
                    cityProjectId,
                    plotVersion,
                    mcVersion
            );
        } catch (SQLException e) {
            throw new DataException(e.getMessage());
        }
    }

    @Override
    public int createPlot(String cityProjectId, String difficultyId, String outlineBounds, UUID createPlayerUUID, byte[] initialSchematic) throws DataException {
        int createdPlotId;

        Connection connection = DatabaseConnection.getConnection();
        try {
            if (connection != null) {
                connection.setAutoCommit(false);

                try (PreparedStatement stmt = Objects.requireNonNull(connection).prepareStatement("INSERT INTO plot (city_project_id, difficulty_id, outline_bounds, initial_schematic, plot_version, created_by) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, cityProjectId);
                    stmt.setString(2, difficultyId);
                    stmt.setString(3, outlineBounds);
                    stmt.setBytes(4, initialSchematic);
                    stmt.setDouble(5, PLOT_VERSION); //TODO: switch to decimal
                    stmt.setString(6, createPlayerUUID.toString());

                    stmt.executeUpdate();

                    // Get the id of the new plot
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            createdPlotId = rs.getInt(1);
                        } else throw new DataException("Could not obtain generated key");
                    }
                }
            } else throw new DataException("Could not connect to database");

            // Finalize database transaction
            connection.commit();
            connection.close();
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.close();
            } catch (SQLException ex) {
                throw new DataException(ex.getMessage());
            }

            throw new DataException(e.getMessage());
        }
        return createdPlotId;
    }

    @Override
    public void setPasted(int id) throws DataException {
        try {
            DatabaseConnection.createStatement("UPDATE plot SET is_pasted = '1' WHERE plot_id = ?")
                    .setValue(id).executeUpdate();
        } catch (SQLException e) {
            throw new DataException(e.getMessage());
        }
    }

    @Override
    public List<Plot> getPlotsToPaste() throws DataException {
        List<Plot> plots = new ArrayList<>();

        try (ResultSet rs = DatabaseConnection
                .createStatement("SELECT plot_id, status, city_project_id, plot_version, mc_version, completed_schematic FROM plot WHERE status = 'completed' AND is_pasted = '0' LIMIT 20")
                .executeQuery()) {
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
            throw new DataException(e.getMessage());
        }
        return plots;
    }
}
