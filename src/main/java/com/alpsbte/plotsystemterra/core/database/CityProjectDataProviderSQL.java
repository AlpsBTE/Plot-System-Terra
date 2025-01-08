package com.alpsbte.plotsystemterra.core.database;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.DatabaseConnection;
import com.alpsbte.plotsystemterra.core.data.CityProjectDataProvider;
import com.alpsbte.plotsystemterra.core.data.DataException;
import com.alpsbte.plotsystemterra.core.model.CityProject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
    public CityProject getCityProject(String id) throws DataException {
        String countryCode = null;
        boolean isVisible;
        String material = null;
        String customModelData = null;
        try (ResultSet rsCity = DatabaseConnection.createStatement("SELECT city.country_code, city.is_visible, c.material, c.custom_model_data " +
                        "FROM city_project city " +
                        "INNER JOIN country c " +
                        "ON c.country_code = city.country_code " +
                        "WHERE city.city_project_id = ?")
                .setValue(id).executeQuery()) {

            if (!rsCity.next()) {
                return null;
            }

            countryCode = rsCity.getString(1);
            isVisible = rsCity.getBoolean(2);
            material = rsCity.getString(3);
            customModelData = rsCity.getString(4);

            DatabaseConnection.closeResultSet(rsCity);
        } catch (SQLException ex) {
            throw new DataException(ex.getMessage());
        }
        return new CityProject(id, countryCode, isVisible, material, customModelData);
    }
}
