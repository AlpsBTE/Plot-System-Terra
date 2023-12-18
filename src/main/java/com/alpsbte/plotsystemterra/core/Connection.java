package com.alpsbte.plotsystemterra.core;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.slf4j.event.Level;

import com.sk89q.worldedit.Vector;

import com.alpsbte.plotsystemterra.core.plotsystem.CityProject;
import com.alpsbte.plotsystemterra.core.plotsystem.Country;
import com.alpsbte.plotsystemterra.core.plotsystem.FTPConfiguration;
import com.alpsbte.plotsystemterra.core.plotsystem.Plot;
import com.alpsbte.plotsystemterra.core.plotsystem.Server;

public interface Connection {
    //public List<Integer> getBuilders() throws Exception;
    
    public boolean getAllCityProjects(List<CityProject> resultList) throws Exception;
    public CityProject getCityProject(int cityID) throws Exception;

    public Plot getPlot(int plotID) throws Exception;
    public List<Plot> getCompletedAndUnpastedPlots() throws Exception;
    
    public Server getServer(int serverID) throws Exception;
    public FTPConfiguration getFTPConfiguration(int ftp_configuration_id) throws Exception;
    public default FTPConfiguration getFTPConfiguration(CityProject cityProject) throws Exception
    {
        Country c = getCountry(cityProject.country_id);
        Server s = getServer(c.server_id);
        return (getFTPConfiguration(s.ftp_configuration_id));
    }
    public default int getServerID(CityProject cityProject) throws Exception
    {
        Country c = getCountry(cityProject.country_id);
        return getServer(c.server_id).id;
    }

    public Country getCountry(int countryID) throws Exception;
    
    //plot creation and modification
    public void setPlotPasted(int plotID) throws Exception;


    public int prepareCreatePlot(CityProject cityProject, int difficultyID, Vector plotCenter, String polyOutline, Player player, double plotVersion) throws Exception;
    public void commitPlot() throws Exception;
    public void rollbackPlot() throws Exception; //aborts a started transaction that created a new plot
}
