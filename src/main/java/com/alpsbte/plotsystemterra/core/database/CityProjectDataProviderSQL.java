package com.alpsbte.plotsystemterra.core.database;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.data.CityProjectDataProvider;
import com.alpsbte.plotsystemterra.core.data.DataException;
import com.alpsbte.plotsystemterra.core.model.CityProject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CityProjectDataProviderSQL implements CityProjectDataProvider {
    @Override
    public List<CityProject> getCityProjects() {
        List<CityProject> listProjects = new ArrayList<>();

        try (ResultSet rs = DatabaseConnection.createStatement("SELECT city_project_id FROM city_project").executeQuery()) {
            while (rs.next()) {
                CityProject city = PlotSystemTerra.getDataProvider().getCityProjectDataProvider().getCityProject(rs.getString(1));
                listProjects.add(city);
            }

            DatabaseConnection.closeResultSet(rs);
        } catch (SQLException ex) {
            throw new DataException(ex.getMessage(), ex);
        }

        return listProjects;
    }

    @Override
    public CompletableFuture<List<CityProject>> getCityProjectsAsync() throws DataException {
        CompletableFuture<List<CityProject>> completableFuture = new CompletableFuture<>();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                completableFuture.complete(getCityProjects());
                return null;
            });
        }
        return completableFuture;
    }

    @Override
    public CityProject getCityProject(String id) throws DataException {
        String countryCode, material, customModelData, serverName;
        Connection con = DatabaseConnection.getConnection();
        if (con == null) {
            throw new DataException("Database connection is null.");
        }
        boolean isVisible;

            try (PreparedStatement ps = con.prepareStatement("SELECT city.country_code, city.is_visible, c.material, c.custom_model_data, city.server_name " +
                    "FROM city_project city " +
                    "INNER JOIN country c " +
                    "ON c.country_code = city.country_code " +
                    "WHERE city.city_project_id = ?")) {
            ps.setString(1, id);

            ResultSet rsCity = ps.executeQuery();

            if (!rsCity.next()) return null;

            countryCode = rsCity.getString(1);
            isVisible = rsCity.getBoolean(2);
            material = rsCity.getString(3);
            customModelData = rsCity.getString(4);
            serverName = rsCity.getString(5);
            DatabaseConnection.closeResultSet(rsCity);
        } catch (SQLException ex) {
            throw new DataException(ex.getMessage(), ex);
        }
        return new CityProject(id, countryCode, isVisible, material, customModelData, serverName);
    }

    @Override
    public CompletableFuture<CityProject> getCityProjectAsync(String id) throws DataException {
        CompletableFuture<CityProject> completableFuture = new CompletableFuture<>();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                completableFuture.complete(getCityProject(id));
                return null;
            });
        }
        return completableFuture;
    }
}
