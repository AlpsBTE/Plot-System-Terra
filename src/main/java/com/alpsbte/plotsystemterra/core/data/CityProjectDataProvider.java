package com.alpsbte.plotsystemterra.core.data;

import com.alpsbte.plotsystemterra.core.model.CityProject;

import java.util.List;

public interface CityProjectDataProvider {
    List<CityProject> getCityProjects() throws DataException;
    CityProject getCityProject(int id) throws DataException;
}
