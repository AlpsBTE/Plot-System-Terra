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

        try (var statement = DatabaseConnection.getConnection().createStatement()) {
            ResultSet rs = statement.executeQuery("SELECT city_project_id FROM city_project");
            while (rs.next()) {
                CityProject city = PlotSystemTerra.getDataProvider().getCityProjectDataProvider().getCityProject(rs.getString(1));
                listProjects.add(city);
            }
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
    public CityProject getCityProject(String id) throws DataException, SQLException {
        String countryCode, material, customModelData, serverName;
        Connection con = DatabaseConnection.getConnection();

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
