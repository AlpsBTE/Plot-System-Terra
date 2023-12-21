import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import com.alpsbte.plotsystemterra.core.DatabaseConnection;
import com.alpsbte.plotsystemterra.core.NetworkAPIConnection;
import com.alpsbte.plotsystemterra.core.api.PlotSystemAPI;
import com.alpsbte.plotsystemterra.core.config.ConfigPaths;
import com.alpsbte.plotsystemterra.core.plotsystem.CityProject;
import com.alpsbte.plotsystemterra.core.plotsystem.Country;
import com.alpsbte.plotsystemterra.core.plotsystem.Difficulty;
import com.alpsbte.plotsystemterra.core.plotsystem.FTPConfiguration;
import com.alpsbte.plotsystemterra.core.plotsystem.Plot;
import com.alpsbte.plotsystemterra.core.plotsystem.Server;
import com.sk89q.worldedit.Vector;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.*;

public class TestConnections {
    private DatabaseConnection createDBConnection() throws Exception{
        File configFile = new File("src/test/resources", "config.yml"); //The file (you can name it what you want)
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile); //Take the file and basically turning it into a .yml file
        
        //read db access
        String dbURL = config.getString(ConfigPaths.DATABASE_URL);
        String dbName = config.getString(ConfigPaths.DATABASE_NAME);
        String dbUusername = config.getString(ConfigPaths.DATABASE_USERNAME);
        String dbPassword = config.getString(ConfigPaths.DATABASE_PASSWORD);
        String teamApiKey = config.getString(ConfigPaths.API_KEY);
        return new DatabaseConnection(dbURL, dbName, dbUusername, dbPassword, teamApiKey);
    }

    private NetworkAPIConnection createAPIconnection() throws Exception{
        File configFile = new File("src/test/resources", "config.yml"); //The file (you can name it what you want)
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile); //Take the file and basically turning it into a .yml file
        
        String teamApiKey = config.getString(ConfigPaths.API_KEY);
        String apiHost = config.getString(ConfigPaths.API_URL);
        
        int apiPort = config.getInt(ConfigPaths.API_KEY);

        return new NetworkAPIConnection(apiHost, apiPort, teamApiKey);
    }

    private Plot getPlot(int plotID, PlotSystemAPI api, String teamApiKey) throws Exception {
        
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

    @Test
    public void testPlotSystemAPI_READ() throws Exception {
        File configFile = new File("src/test/resources", "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        String team_apikey = config.getString(ConfigPaths.API_KEY);
        String apiHost = config.getString(ConfigPaths.API_URL);

        //Test PlostSystemAPI class 
        PlotSystemAPI api = new PlotSystemAPI(apiHost,8080);
        int builderCount = api.getPSBuilderCount();
        System.out.println("Builder Count: " + builderCount);
        assertEquals(17, builderCount);

        Thread.sleep(200);

        List<Difficulty> difficulties = api.getPSDifficulties();
        System.out.println(difficulties);

        Thread.sleep(200);        
        System.out.println("-------countries-----------");

        List<Country> countries = api.getPSTeamCountries(team_apikey);
        for (Country c : countries){
            System.out.println((c));
        }

        Thread.sleep(200);        
        System.out.println("-------cities-----------");

        List<CityProject> cities = api.getPSTeamCities(team_apikey);
        for (CityProject c : cities){
            System.out.println((c.name + ": "+ c));
        }
               
        Thread.sleep(200);        
        System.out.println("-------plots-----------");

        List<Plot> plots = api.getPSTeamPlots(team_apikey);
        for (Plot p : plots){
            System.out.println((p.mc_coordinates + ": "+ p));
        }
        
        Thread.sleep(200);
        System.out.println("-------servers-----------");

        List<Server> servers = api.getPSTeamServers(team_apikey);
        for (Server s : servers){
            System.out.println((s.name + ": "+ s));
        }

        Thread.sleep(200);
        System.out.println("-------ftp configs-----------");

        List<FTPConfiguration> configs = api.getPSTeamFTPConfigurations(team_apikey);
        for (FTPConfiguration c : configs){
            System.out.println((c.address +  ": "+ c));
        }                     

    }

    @Test
    public void testPlotSystemAPI_MODIFY() throws Exception {
        File configFile = new File("src/test/resources", "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        String team_apikey = config.getString(ConfigPaths.API_KEY);
        String apiHost = config.getString(ConfigPaths.API_URL);

        //Test PlostSystemAPI class 
        PlotSystemAPI api = new PlotSystemAPI(apiHost,8080);
            
        //check plot 1 has "pasted: 1"
        Plot plot = getPlot(1, api, team_apikey);
        assertEquals(1, plot.id);
        assertEquals(1, plot.pasted);

        //change to "pasted: 0"
        api.updatePSPlot(1, Arrays.asList("\"pasted\": 0"), team_apikey);
        plot = getPlot(1, api, team_apikey);
        assertEquals(1, plot.id);
        assertEquals(0, plot.pasted);


        //change back
        api.updatePSPlot(1, Arrays.asList("\"pasted\": 1"), team_apikey);
        plot = getPlot(1, api, team_apikey);
        assertEquals(1, plot.id);
        assertEquals(1, plot.pasted);
    }

    @Test
    public void testPlotSystemAPI_CREATE() throws Exception {
        File configFile = new File("src/test/resources", "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        String team_apikey = config.getString(ConfigPaths.API_KEY);
        String apiHost = config.getString(ConfigPaths.API_URL);

        //Test PlostSystemAPI class 
        PlotSystemAPI api = new PlotSystemAPI(apiHost,8080);
            
        //plot direct creation, returns plotID for potential undo/delete
        int plotID = api.createPSPlot(1,1,
            new Vector(1.1,2.2,3.3),"4078352.0,-4550687.0|4078371.0,-4550675.0|4078370.0,-4550669.0", 
            3, team_apikey );


        api.deletePSPlot(plotID, team_apikey);
    }

    @Test
    public void compareConnections_Cities() throws Exception{
        DatabaseConnection db = createDBConnection();
        NetworkAPIConnection api = createAPIconnection();

        //test both interfaces return the same values

        //--------------- cityProjects ---------------------

        CityProject city1DB = db.getCityProject(1) ;
        CityProject city1API = db.getCityProject(1) ;
        assertNotNull(city1API);
        assertThat(city1DB, samePropertyValuesAs(city1API));

        // ------------ cityprojects: all --------------
        List<CityProject> citiesDB = new ArrayList<>();
        boolean resultDB = db.getAllCityProjects(citiesDB);
        List<CityProject> citiesAPI = new ArrayList<>();
        boolean resultAPI = api.getAllCityProjects(citiesAPI);
        
        assertEquals(true, resultDB);
        assertEquals(true, resultAPI);
        assertEquals(citiesDB.size(), citiesAPI.size());
        //assertThat(citiesDB, containsInAnyOrder(citiesAPI));
        for (CityProject cityDB : citiesDB){
            assertThat(citiesAPI, hasItem(samePropertyValuesAs(cityDB)));
        }
    }

    
    @Test
    public void compareConnections_Plots() throws Exception{
        DatabaseConnection db = createDBConnection();
        NetworkAPIConnection api = createAPIconnection();

        //test both interfaces return the same values

        //--------------plots --------------------------

        Plot plot1DB = db.getPlot(1);
        Plot plot1API = api.getPlot(1);
        assertThat(plot1DB, samePropertyValuesAs(plot1API));

        List<Plot> plotsDB = db.getCompletedAndUnpastedPlots() ;
        List<Plot> plotsAPI = api.getCompletedAndUnpastedPlots() ;
        assertEquals(plotsDB.size(), plotsAPI.size());
        for (Plot plotDB : plotsDB){
            assertThat(plotsAPI, hasItem(samePropertyValuesAs(plotDB)));
        }
    }

    @Test
    public void compareConnections_Countries() throws Exception{
        DatabaseConnection db = createDBConnection();
        NetworkAPIConnection api = createAPIconnection();

        //test both interfaces return the same values

        //--------------plots --------------------------

        Country c1DB = db.getCountry(1);
        Country c1API = api.getCountry(1);
        assertNotNull(c1DB);
        assertThat(c1DB, samePropertyValuesAs(c1API));

    }

    @Test
    public void compareConnections_Server() throws Exception{
        DatabaseConnection db = createDBConnection();
        NetworkAPIConnection api = createAPIconnection();

        //test both interfaces return the same values

        //--------------plots --------------------------

        Server s1DB = db.getServer(1);
        Server s1API = api.getServer(1);
        assertNotNull(s1API);
        assertThat(s1DB, samePropertyValuesAs(s1API));

    }

    @Test
    public void compareConnections_FTP() throws Exception{
        DatabaseConnection db = createDBConnection();
        NetworkAPIConnection api = createAPIconnection();

        //test both interfaces return the same values

        //--------------plots --------------------------

        FTPConfiguration s1DB = db.getFTPConfiguration(2);
        FTPConfiguration s1API = api.getFTPConfiguration(2);
        assertNotNull(s1API);
        assertThat(s1DB, samePropertyValuesAs(s1API));

    }

    //TODO test setplotpasted
    //TODO test create/rollback
}
