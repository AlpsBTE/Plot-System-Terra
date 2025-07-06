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
import com.alpsbte.plotsystemterra.core.data.CityProjectDataProvider;
import com.alpsbte.plotsystemterra.core.data.DataException;
import com.alpsbte.plotsystemterra.core.model.CityProject;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CityProjectDataProviderSQL implements CityProjectDataProvider {
    @Override
    public List<CityProject> getCityProjects() {
        String queryGetIds = "SELECT city_project_id FROM city_project";

        return SqlExceptionUtil.handle(() -> SqlHelper.runQuery(queryGetIds, ps -> {
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
