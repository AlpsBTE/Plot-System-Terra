package com.alpsbte.plotsystemterra.core;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.api.PlotSystemAPI;
import com.alpsbte.plotsystemterra.core.api.PlotSystemAPI.PlotCreateResult;
import com.alpsbte.plotsystemterra.core.plotsystem.CityProject;
import com.alpsbte.plotsystemterra.core.plotsystem.Country;
import com.alpsbte.plotsystemterra.core.plotsystem.FTPConfiguration;
import com.alpsbte.plotsystemterra.core.plotsystem.Plot;
import com.alpsbte.plotsystemterra.core.plotsystem.Server;
import com.alpsbte.plotsystemterra.utils.Utils;
import com.sk89q.worldedit.Vector;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.time.LocalDate;

public class NetworkAPIConnection implements Connection{
    private String teamApiKey;
    // private String host;
    // private int port;
    private PlotSystemAPI api;
    private String currentTransactionID;

    public NetworkAPIConnection(String host, int port, String teamApiKey) {    
        // this.host = host;
        // this.port = port;
        this.teamApiKey = teamApiKey;
        this.api = new PlotSystemAPI(host, port);
    }

    @Override
    public CityProject getCityProject(int cityID) {
        try {
            List<CityProject> cities = api.getPSTeamCities(teamApiKey);
            for (CityProject c : cities){
                if (c.id == cityID)
                    return c;
            }
            return null;
        } catch (Exception ex) {
             return null;
        }
    }

    @Override
    public boolean getAllCityProjects(List<CityProject> resultList) {
        try {
            resultList.addAll(api.getPSTeamCities(teamApiKey));
            return true;
        } catch (Exception ex) {
             return false;
        }
    }
    
    @Override
    public int prepareCreatePlot(CityProject cityProject, int difficultyID, Vector plotCoords, String polyOutline, Player player, double plotVersion) throws Exception{
        PlotCreateResult newPlot = api.createPSPlot(true, cityProject.id, difficultyID, plotCoords, polyOutline, plotVersion, teamApiKey);
        this.currentTransactionID = newPlot.transactionID;
        return newPlot.plotID;       
    }




    @Override
    public void commitPlot() throws Exception{
        //confirm transaction/order with order-id
        api.confirmTransaction(this.currentTransactionID, teamApiKey);
    }

    @Override
    public void rollbackPlot() throws Exception{
        //nothing to do, the plot-creation order is never confirmed and does not need to be canceled
    }


    @Override
    public Plot getPlot(int plotID) throws Exception {
        
        try {
            List<Plot> plots = api.getPSTeamPlots(teamApiKey);
            for (Plot p : plots){
                if (p.id == plotID)
                    return p;
        }
            return null;
        } catch (Exception ex) {
             return null;
        }
    }

    @Override
    public List<Plot> getCompletedAndUnpastedPlots() throws Exception {
        List<Plot> unpastedPlots = new ArrayList<>();

        // try (ResultSet rs = createStatement("SELECT id, city_project_id, mc_coordinates, version FROM plotsystem_plots WHERE status = 'completed' AND pasted = '0' LIMIT 20")
        //     .executeQuery()) {

        List<Plot> allPlots = api.getPSTeamPlots(teamApiKey);
        for (Plot p : allPlots){
            if (p.status == "completed" && p.pasted==0)
                unpastedPlots.add(p);
        }
        return unpastedPlots; 
    }


    @Override
    public void setPlotPasted(int plotID) throws Exception {
        api.updatePSPlot(plotID, Arrays.asList("\"pasted\":1 "), teamApiKey);

    }

    @Override
    public Server getServer(int serverID) throws Exception {
        List<Server> servers = api.getPSTeamServers(teamApiKey);
        for (Server s : servers){
            if (s.id == serverID)
                return s;
        }
        throw new java.io.IOException("Could not find server with id " + serverID);
    }

    @Override
    public FTPConfiguration getFTPConfiguration(int ftp_configuration_id) throws Exception {

        List<FTPConfiguration> configs = api.getPSTeamFTPConfigurations(teamApiKey);
        for (FTPConfiguration c : configs){
            if (c.id == ftp_configuration_id)
                return c;
        }
        throw new java.io.IOException("Could not find ftp config with id " + ftp_configuration_id + " (or it is from another build team).");
    }

    @Override
    public Country getCountry(int countryID) throws Exception {
        List<Country> countries = api.getPSTeamCountries(teamApiKey);
        for (Country c : countries){
            if (c.id == countryID)
                return c;
        }
        throw new java.io.IOException("Could not find country with id " + countryID);
    }

}
