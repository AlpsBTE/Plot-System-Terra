package com.alpsbte.plotsystemterra.core.database;

import com.alpsbte.plotsystemterra.core.DatabaseConnection;
import com.alpsbte.plotsystemterra.core.data.DataException;
import com.alpsbte.plotsystemterra.core.data.PlotDataProvider;
import com.alpsbte.plotsystemterra.core.model.Plot;

import java.sql.*;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import static com.alpsbte.plotsystemterra.core.plotsystem.PlotCreator.PLOT_VERSION;

public class PlotDataProviderSQL implements PlotDataProvider {
    @Override
    public Plot getPlot(int id) throws DataException {
        try (ResultSet rs = DatabaseConnection.createStatement("SELECT status, city_project_id, mc_coordinates, version FROM plotsystem_plots WHERE id = ?")
                .setValue(id).executeQuery()) {

            if (!rs.next()) return null;

            String status = rs.getString(1);
            int cityProjectId = rs.getInt(2);
            String[] splitCoordinates = rs.getString(3).split(",");
            double version = rs.getDouble(4);

            DatabaseConnection.closeResultSet(rs);

            return new Plot(
                    id,
                    status,
                    version,
                    cityProjectId,
                    splitCoordinates
            );
        } catch (SQLException e) {
            throw new DataException(e.getMessage());
        }
    }

    @Override
    public int createPlot(int cityProjectId, int difficulty, String mcCoordinates, String outline, UUID createPlayerUUID) throws DataException {
        int createdPlotId = -1;

        Connection connection = DatabaseConnection.getConnection();
        try {
            if (connection != null) {
                connection.setAutoCommit(false);

                try (PreparedStatement stmt = Objects.requireNonNull(connection).prepareStatement("INSERT INTO plotsystem_plots (city_project_id, difficulty_id, mc_coordinates, outline, create_date, create_player, version) VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, cityProjectId);
                    stmt.setInt(2, difficulty);
                    stmt.setString(3, mcCoordinates);
                    stmt.setString(4, outline);
                    stmt.setDate(5, java.sql.Date.valueOf(LocalDate.now()));
                    stmt.setString(6, createPlayerUUID.toString());
                    stmt.setDouble(7, PLOT_VERSION);
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
}
