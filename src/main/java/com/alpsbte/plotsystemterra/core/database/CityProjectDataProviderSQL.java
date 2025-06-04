package com.alpsbte.plotsystemterra.core.database;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.data.CityProjectDataProvider;
import com.alpsbte.plotsystemterra.core.data.DataException;
import com.alpsbte.plotsystemterra.core.model.CityProject;

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
            throw new DataException(ex.getMessage());
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
        boolean isVisible;
        try (ResultSet rsCity = DatabaseConnection.createStatement("SELECT city.country_code, city.is_visible, c.material, c.custom_model_data, city.server_name " +
                        "FROM city_project city " +
                        "INNER JOIN country c " +
                        "ON c.country_code = city.country_code " +
                        "WHERE city.city_project_id = ?")
                .setValue(id).executeQuery()) {

            if (!rsCity.next()) return null;

            countryCode = rsCity.getString(1);
            isVisible = rsCity.getBoolean(2);
            material = rsCity.getString(3);
            customModelData = rsCity.getString(4);
            serverName = rsCity.getString(5);
            DatabaseConnection.closeResultSet(rsCity);
        } catch (SQLException ex) {
            throw new DataException(ex.getMessage());
        }
        return new CityProject(id, countryCode, isVisible, material, customModelData, serverName);
    }

    public boolean checkCityProjectExist(String id) throws DataException {
        try (ResultSet rsCity = DatabaseConnection.createStatement(
                "SELECT EXISTS (" +
                "    SELECT 1 FROM city_project WHERE city_project_id = ?" +
                ")")
                .setValue(id).executeQuery()) {

            boolean exist = false;
            if (rsCity.next()) exist = rsCity.getBoolean(1);

            DatabaseConnection.closeResultSet(rsCity);
            return exist;
        } catch (SQLException ex) {
            throw new DataException(ex.getMessage());
        }
    }

    public boolean checkDifficultyExist(String id) throws DataException {
        try (ResultSet rs = DatabaseConnection.createStatement(
                "SELECT EXISTS (" +
                "    SELECT 1 FROM plot_difficulty WHERE difficulty_id = ?" +
                ")")
                .setValue(id).executeQuery()) {

            boolean exist = false;
            if(rs.next()) exist = rs.getBoolean(1);

            DatabaseConnection.closeResultSet(rs);
            return exist;
        } catch (SQLException ex) {
            throw new DataException(ex.getMessage());
        }
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
