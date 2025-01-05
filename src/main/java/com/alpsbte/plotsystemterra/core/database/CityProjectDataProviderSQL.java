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

        try (ResultSet rs = DatabaseConnection.createStatement("SELECT id FROM plotsystem_city_projects").executeQuery()) {
            while (rs.next()) {
                CityProject city = PlotSystemTerra.getDataProvider().getCityProjectDataProvider().getCityProject(rs.getInt(1));
                listProjects.add(city);
            }

            DatabaseConnection.closeResultSet(rs);
        } catch (SQLException ex) {
            throw new DataException(ex.getMessage());
        }

        return listProjects;
    }

    @Override
    public CityProject getCityProject(int id) throws DataException {
        int countryId;
        String name;
        String headId = null;
        try (ResultSet rsCity = DatabaseConnection.createStatement("SELECT country_id, name FROM plotsystem_city_projects WHERE id = ?")
                .setValue(id).executeQuery()) {

            if (!rsCity.next()) {
                return null;
            }

            countryId = rsCity.getInt(1);
            name = rsCity.getString(2);

            DatabaseConnection.closeResultSet(rsCity);

            try (ResultSet rsCountry = DatabaseConnection.createStatement("SELECT head_id FROM plotsystem_countries WHERE id = ?")
                    .setValue(countryId).executeQuery()) {

                if (rsCountry.next()) {
                    headId = rsCountry.getString(1);
                }

                DatabaseConnection.closeResultSet(rsCountry);
            }
        } catch (SQLException ex) {
            throw new DataException(ex.getMessage());
        }
        return new CityProject(id, name, countryId, headId);
    }
}
