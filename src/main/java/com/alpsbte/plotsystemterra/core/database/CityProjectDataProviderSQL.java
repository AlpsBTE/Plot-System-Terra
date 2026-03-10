package com.alpsbte.plotsystemterra.core.database;

import com.alpsbte.alpslib.io.database.SqlHelper;
import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.config.ConfigPaths;
import com.alpsbte.plotsystemterra.core.data.CityProjectDataProvider;
import com.alpsbte.plotsystemterra.core.data.DataException;
import com.alpsbte.plotsystemterra.core.model.CityProject;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CityProjectDataProviderSQL implements CityProjectDataProvider {
    @Override
    public List<CityProject> getCityProjects() {
        String queryGetIds = "SELECT city_project_id FROM city_project  WHERE server_name = ?";

        var serverName = PlotSystemTerra.getPlugin().getConfig().getString(ConfigPaths.SERVER_NAME, "NOTHING_CONFIGURED");
        return SqlExceptionUtil.handle(() -> SqlHelper.runQuery(queryGetIds, ps -> {
            ps.setString(1, serverName);
            List<CityProject> listProjects = new ArrayList<>();
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                CityProject city = getCityProject(rs.getString(1));
                if (city != null) {
                    listProjects.add(city);
                }
            }
            return listProjects;
        }));
    }

    public CityProject getCityProject(String id) throws DataException {
        String queryGetCityProject = "SELECT city.country_code, city.is_visible, c.material, c.custom_model_data, city.server_name " +
                "FROM city_project city " +
                "INNER JOIN country c ON c.country_code = city.country_code " +
                "WHERE city.city_project_id = ?";

        return SqlExceptionUtil.handle(() -> SqlHelper.runQuery(queryGetCityProject, ps -> {
            ps.setString(1, id);
            ResultSet rsCity = ps.executeQuery();
            if (!rsCity.next()) return null;

            String countryCode = rsCity.getString(1);
            boolean isVisible = rsCity.getBoolean(2);
            String material = rsCity.getString(3);
            String customModelData = rsCity.getString(4);
            String serverName = rsCity.getString(5);

            return new CityProject(id, countryCode, isVisible, material, customModelData, serverName);
        }));
    }
}
