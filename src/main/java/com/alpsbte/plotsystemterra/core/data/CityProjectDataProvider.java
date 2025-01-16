package com.alpsbte.plotsystemterra.core.data;

import com.alpsbte.plotsystemterra.core.model.CityProject;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CityProjectDataProvider {
    List<CityProject> getCityProjects() throws DataException;
    CompletableFuture<List<CityProject>> getCityProjectsAsync() throws DataException;
    CityProject getCityProject(String id) throws DataException;
    CompletableFuture<CityProject> getCityProjectAsync(String id) throws DataException;
}
