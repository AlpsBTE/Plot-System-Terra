package com.alpsbte.plotsystemterra.core.data;

import com.alpsbte.plotsystemterra.core.model.CityProject;

import java.util.List;

public interface CityProjectDataProvider {

    /**
     * Retrieves all CityProjects.
     *
     * @return A list of all CityProjects.
     * @throws DataException If an error occurs while retrieving the CityProjects.
     */
    public abstract List<CityProject> getCityProjects();

    /**
     * Retrieves a CityProject by its ID.
     *
     * @param id The ID of the CityProject to retrieve.
     * @return The CityProject with the specified ID, or null if not found.
     * @throws DataException If an error occurs while retrieving the CityProject.
     */
    public abstract CityProject getCityProject(String id) throws DataException;
}
