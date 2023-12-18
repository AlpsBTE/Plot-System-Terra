package com.alpsbte.plotsystemterra.core.api;

import org.bukkit.Bukkit;

import com.alpsbte.plotsystemterra.core.plotsystem.CityProject;
import com.alpsbte.plotsystemterra.core.plotsystem.Country;
import com.alpsbte.plotsystemterra.core.plotsystem.Difficulty;
import com.alpsbte.plotsystemterra.core.plotsystem.FTPConfiguration;
import com.alpsbte.plotsystemterra.core.plotsystem.Plot;
import com.alpsbte.plotsystemterra.core.plotsystem.Server;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.sk89q.worldedit.Vector;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class PlotSystemAPI {
    private class BooleanDeserializer implements JsonDeserializer<Boolean> {
        @Override
        public Boolean deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            // Customize the deserialization logic based on your needs
            int intValue = json.getAsInt();
            return intValue != 0;
        }
    }

    private String host;// = "http://nwapi.buildtheearth.net";
    private int port;


    private static String GET_PS_BUILDERS_URL = "/api/plotsystem/builders";
    private static String GET_PS_DIFFICULTIES_URL = "/api/plotsystem/difficulties";
    private static String GET_PS_CITIES_URL = "/api/plotsystem/teams/%API_KEY%/cities";
    private static String GET_PS_COUNTRIES_URL = "/api/plotsystem/teams/%API_KEY%/countries";
    private static String GET_PS_SERVERS_URL = "/api/plotsystem/teams/%API_KEY%/countries";
    private static String GET_PS_PlOTS_URL = "/api/plotsystem/teams/%API_KEY%/plots";
    private static String GET_PS_FTP_URL = "/api/plotsystem/teams/%API_KEY%/ftp";
    private static String POST_PS_CREATE_PLOT_ORDER_URL = "/api/plotsystem/teams/%API_KEY%/ftp";
    private static String GET_PS_CONFIRM_PLOT_ORDER_URL = "/api/plotsystem/teams/%API_KEY%/confirm/%ORDER%/confirm";

    private static String PUT_PS_UPDATE_PLOT_URL = "/api/plotsystem/teams/%API_KEY%/plots";

    public PlotSystemAPI(String host, int port){
        this.host = host;
        this.port = port;
    }
    // A function that returns the content of a GET Request from a given URL
    private String httpGET(String endpoint) {
        String apiUrl = host + endpoint;

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            // Check if the request was successful (HTTP status code 200)
            if (response.statusCode() == 200) {
                String jsonResponse = response.body();
                return jsonResponse;
            } else {
                Bukkit.getLogger().log(Level.SEVERE, "API request return error code (HTTP status): " + response.statusCode());
                return null;
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Exception occurred while making the API request: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    private String httpPUT(String endpoint, String jsonBodyString) {
        String apiUrl = host + endpoint;


        
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBodyString))
                .build();
        
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            // Check if the request was successful (HTTP status code 200)
            if (response.statusCode() == 200) {
                String jsonResponse = response.body();
                return jsonResponse;
            } else {
                String errorMessage = "API request return error code (HTTP status): " + response.statusCode();
                System.out.println(errorMessage);

                return null;
            }
        } catch (Exception e) {
            String errorMessage = "Exception occurred while making the API request: " + e.getMessage();
            System.out.println(errorMessage);
            //Bukkit.getLogger().log(Level.SEVERE, "Exception occurred while making the API request: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }


    public int getPSBuilderCount(){
        String jsonResponse = httpGET(GET_PS_BUILDERS_URL);

        //response looks like this: array with single object with "builders:X"
        /*
                [
                    {
                        "builders": "17"
                    }
                ]
         */
        try {
            JsonArray responseArray = new JsonParser().parse(jsonResponse).getAsJsonArray();
            JsonObject firstObject = responseArray.get(0).getAsJsonObject();
            int builderCount = firstObject.get("builders").getAsInt();

            return builderCount;
        } catch (Exception e) {
            System.out.println("Error parsing JSON response: " + e.getMessage());
        }

        // Return a default value or throw an exception based on your requirements
        return -1;
    }

    public List<Difficulty> getPSDifficulties(){
        List<Difficulty> difficulties = new ArrayList<>();
        String jsonResponse = httpGET(GET_PS_DIFFICULTIES_URL);

        //response looks like this: array with difficulty objects
        /*
[
    {
        "id": 1,
        "multiplier": 1,
        "name": "EASY",
        "score_requirment": 0
    },
    ...
]
         */
        try {
            JsonArray responseArray = new JsonParser().parse(jsonResponse).getAsJsonArray();
            for (JsonElement element : responseArray){
                JsonObject o = element.getAsJsonObject();
                difficulties.add(new Difficulty(o.get("name").getAsString(),
                                     o.get("id").getAsInt(), o.get("multiplier").getAsFloat(), o.get("score_requirment").getAsInt()));
                //api actually returns difficulty with a typo "requirment"
            }
        } catch (Exception e) {
            System.out.println("Error parsing JSON response: " + e.getMessage());
        }

        // Return a default value or throw an exception based on your requirements
        return difficulties;
    }

    public List<CityProject> getPSTeamCities(String teamApiKey){
        List<CityProject> cities = new ArrayList<>();
        String jsonResponse = httpGET(GET_PS_CITIES_URL.replace("%API_KEY%", teamApiKey));
        //List<Country> allCountries = getPSTeamCountries(teamApiKey);
        //response looks like this: array with city objects
        /*
[
    {
        "country_id": 1,
        "description": "Test-City in the beautiful Test-Country",
        "id": 1,
        "name": "Test-City",
        "visible": 1
    },
]
         */
        try {
            JsonArray responseArray = new JsonParser().parse(jsonResponse).getAsJsonArray();
            for (JsonElement element : responseArray){
                JsonObject o = element.getAsJsonObject();
                int countryID = o.get("country_id").getAsInt();

                //Country country = findCountryWithID(countryID, allCountries);
                CityProject city = new CityProject(
                    o.get("id").getAsInt(), countryID, o.get("name").getAsString()/* country.head_id*/);
                cities.add(city);    
            }
        } catch (Exception e) {
            System.out.println("Error parsing JSON response: " + e.getMessage());
        }

        // Return a default value or throw an exception based on your requirements
        return cities;
    }

    public List<Country> getPSTeamCountries(String teamApiKey){
        List<Country> countries = new ArrayList<>();
        String jsonResponse = httpGET(GET_PS_COUNTRIES_URL.replace("%API_KEY%", teamApiKey));

        //response looks like this: object with map (countryID => countryObject) with country objects
        /*
{
    "1": {
        "continent": "asia",
        "head_id": "24208",
        "id": 1,
        "name": "Test-Country",
        "server_id": 1
    }
}
         */
        Gson gson = new Gson();
        try {
            JsonObject responseObject = new JsonParser().parse(jsonResponse).getAsJsonObject();

            Type mapType = new TypeToken<Map<String, Country>>() {}.getType();
            Map<String, Country> countryMap = gson.fromJson(responseObject, mapType);
            for (Country c :  countryMap.values()){
                //Country country = gson.fromJson(element, Country.class);
                countries.add(c);
                }
        } catch (Exception e) {
            System.out.println("Error parsing JSON response: " + e.getMessage());
        }

        // Return a default value or throw an exception based on your requirements
        return countries;
    }

    public List<Plot> getPSTeamPlots(String teamApiKey) {
        List<Plot> plots = new ArrayList<>();
        String jsonResponse = httpGET(GET_PS_PlOTS_URL.replace("%API_KEY%", teamApiKey));


/** array of objects
 *[
    {
        "city_project_id": 1,
        "create_date": "2022-07-14T00:00:00.000Z",
        "create_player": "3b350308-d857-4ecc-8b71-c93a2cf3c87b",
        "difficulty_id": 1,
        "id": 1,
        "last_activity": "2022-07-17T00:00:00.000Z",
        "mc_coordinates": "3190699.5,690.5,-4673990.0",
        "member_uuids": null,
        "outline": "3190689.0,-4673973.0|3190718.0,-4673987.0|3190712.0,-4674007.0|3190689.0,-4674001.0|3190681.0,-4673994.0",
        "owner_uuid": "3b350308-d857-4ecc-8b71-c93a2cf3c87b",
        "pasted": 1,
        "review_id": 5,
        "score": 16,
        "status": "completed",
        "type": 2,
        "version": 3
    },...
 */
        try {
            JsonArray responseArray = new JsonParser().parse(jsonResponse).getAsJsonArray();
            for (JsonElement element : responseArray){
                JsonObject obj = element.getAsJsonObject();
                String[] splitCoordinates = obj.get("mc_coordinates").getAsString().split(",");
                Vector mcCoordinates = Vector.toBlockPoint(
                        Float.parseFloat(splitCoordinates[0]),
                        Float.parseFloat(splitCoordinates[1]),
                        Float.parseFloat(splitCoordinates[2])
                );
                Plot p = new Plot(obj.get("id").getAsInt(), obj.get("status").getAsString(), 
                    obj.get("city_project_id").getAsInt(), mcCoordinates,
                    obj.get("pasted").getAsInt(), obj.get("version").getAsFloat());
                    
                plots.add(p);
            }
        } catch (Exception e) {
            System.out.println("Error parsing JSON response: " + e.getMessage());
        }

        // Return a default value or throw an exception based on your requirements
        return plots;
    }
    
    public List<Server> getPSTeamServers(String teamApiKey) {
        List<Server> servers = new ArrayList<>();
        String jsonResponse = httpGET(GET_PS_SERVERS_URL.replace("%API_KEY%", teamApiKey));

        //response looks like this: object with map (serverid => server data)
        /*
{
    "1": {
        "ftp_configuration_id": 2,
        "id": 1,
        "name": "BT-1"
    },...
         */
        Gson gson = new Gson();
        try {
            JsonObject responseObject = new JsonParser().parse(jsonResponse).getAsJsonObject();
            Type mapType = new TypeToken<Map<String, Server>>() {}.getType();
            Map<String, Server> serverMap = gson.fromJson(responseObject, mapType);
            for (Server s :  serverMap.values()){
                servers.add(s);
                }
        } catch (Exception e) {
            System.out.println("Error parsing JSON response: " + e.getMessage());
        }

        // Return a default value or throw an exception based on your requirements
        return servers;
    }
    
    public List<FTPConfiguration> getPSTeamFTPConfigurations(String teamApiKey) {
        List<FTPConfiguration> configs = new ArrayList<>();
        String jsonResponse = httpGET(GET_PS_FTP_URL.replace("%API_KEY%", teamApiKey));

        //response object with map serverID => array (!?) of ftp configs
        /*
{
    "1": [
        {
            "address": "xx.xxx.xxx.xxx",
            "id": 2,
            "isSFTP": 1,
            "password": "<redacted>",
            "port": 22,
            "schematics_path": "/home/PlotSystem/",
            "username": "<redacted>"
        }
    ],...
}
         */
        //register type adapter for boolean from integer
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(boolean.class, new BooleanDeserializer())
                .create();
        try {
            JsonObject responseObject = new JsonParser().parse(jsonResponse).getAsJsonObject();
            // Type mapType = new TypeToken<Map<String, FTPConfiguration>>() {}.getType();
            // Map<String, FTPConfiguration> serverMap = gson.fromJson(responseObject, mapType);
            // for (FTPConfiguration c :  serverMap.values()){
            //     configs.add(c);
            // }

            Type mapType = new TypeToken<Map<String, JsonArray>>() {}.getType();
            Map<String, JsonArray> serverMap = gson.fromJson(responseObject, mapType);
            for (JsonArray ftpArray : serverMap.values()){
                for (JsonElement element : ftpArray){

                    FTPConfiguration c = gson.fromJson(element, FTPConfiguration.class);

                    configs.add(c);
                }
            }

            
        } catch (Exception e) {
            System.out.println("Error parsing JSON response: " + e.getMessage());
        }
        return configs;
    }
    public void updatePSPlot(int plotID, List<String> changeList, String teamApiKey) {
        String requestBody = "[{"+ String.join(",", changeList ) +"}]";
        String jsonResponse = httpPUT(PUT_PS_UPDATE_PLOT_URL.replace("%API_KEY%", teamApiKey), requestBody);


        System.out.println(jsonResponse);
    }
    

}

