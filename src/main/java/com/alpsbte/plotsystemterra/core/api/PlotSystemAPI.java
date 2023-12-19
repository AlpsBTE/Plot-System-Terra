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

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.Builder;
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
    private static String POST_PS_CREATE_PLOT_ORDER_URL = "/api/plotsystem/teams/%API_KEY%/plots";
    private static String GET_PS_CONFIRM_PLOT_ORDER_URL = "/api/plotsystem/teams/%API_KEY%/orders/%ORDER_ID%/confirm";

    private static String PUT_PS_UPDATE_PLOT_URL = "/api/plotsystem/teams/%API_KEY%/plots";

    public PlotSystemAPI(String host, int port){
        this.host = host;
        this.port = port;
    }
    
    private enum RequestMethod{GET, PUT, POST}

    private String makeHttpRequest(RequestMethod method, String endpoint, String jsonBody) throws Exception{
        String apiUrl = host + endpoint;
        
        HttpClient httpClient = HttpClient.newHttpClient();

        //build request. depending on type, add body and header
        Builder b = HttpRequest.newBuilder().uri(URI.create(apiUrl));
        BodyPublisher body = null;
        if (jsonBody != null){
            b = b.header("Content-Type", "application/json");
            body = HttpRequest.BodyPublishers.ofString(jsonBody);
        }
        HttpRequest request = null;
        switch (method) {
            case GET:
                request = b.GET().build();                
                break;
            case PUT:
                request = b.PUT(body).build();
                break;
            case POST:
                request = b.POST(body).build();
                break;
        }
       
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Check if the request was successful (HTTP status code 200)
        if (response.statusCode() == 200) {
            String jsonResponse = response.body();
            return jsonResponse;
        } else {
            String errorMessage = "API HTTP request return error code (HTTP status): " + response.statusCode();
            System.out.println(errorMessage);
            throw new IOException(errorMessage);
        }

    }
    // A function that returns the content of a GET Request from a given URL
    private String httpGET(String endpoint) throws Exception{
        return makeHttpRequest(RequestMethod.GET, endpoint, null);
    }    
    private String httpPUT(String endpoint, String jsonBodyString) throws Exception{
        return makeHttpRequest(RequestMethod.PUT, endpoint, jsonBodyString);
    }
    private String httpPOST(String endpoint, String jsonBodyString) throws Exception{
        return makeHttpRequest(RequestMethod.POST, endpoint, jsonBodyString);
    }


    public int getPSBuilderCount() throws Exception{
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

    public List<Difficulty> getPSDifficulties() throws Exception{
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

    public List<CityProject> getPSTeamCities(String teamApiKey) throws Exception{
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

    public List<Country> getPSTeamCountries(String teamApiKey) throws Exception{
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

    public List<Plot> getPSTeamPlots(String teamApiKey) throws Exception {
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
    
    public List<Server> getPSTeamServers(String teamApiKey) throws Exception{
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
    
    public List<FTPConfiguration> getPSTeamFTPConfigurations(String teamApiKey) throws Exception{
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
    public void updatePSPlot(int plotID, List<String> changeList, String teamApiKey) throws Exception{
        //Request body is an array with a single element, usind identifier and any parameters to change
        String requestBody = "[\n\t{\n\t\t\"id\": "+plotID+",\n\t\t"
                    + String.join(",\n\t\t", changeList ) +"\n\t}\n]";
        // System.out.println("PUT " +PUT_PS_UPDATE_PLOT_URL.replace("%API_KEY%", teamApiKey));
        // System.out.println("Body:\n" + requestBody);
        httpPUT(PUT_PS_UPDATE_PLOT_URL.replace("%API_KEY%", teamApiKey), requestBody);


        //System.out.println(jsonResponse);
    }

    public class PlotCreateResult{
        public PlotCreateResult(int plotID){
            this.plotID = plotID;
            this.transactionID = null;
        }
        public PlotCreateResult(int plotID, String transactionID)
        {
            this.plotID = plotID;
            this.transactionID = transactionID;
        }
        public int plotID;
        public String transactionID; //only used for creation with "asOrder = true" / transaction
    }

    public PlotCreateResult createPSPlot(boolean asTransaction, int cityProjectID, int difficultyID, Vector plotCoords, String polyOutline, double plotVersion, String teamApiKey) throws Exception{
        String vectorString = plotCoords.toString();
        vectorString = vectorString.substring(1,vectorString.length()-1); //remove brackets
        String requestBody = "[\n\t{\n"
            +"\t\t\"city_project_id\": "+cityProjectID+",\n"
            +"\t\t\"difficulty_id\": "+difficultyID+",\n"
            +"\t\t\"mc_coordinates\": \""+vectorString+"\",\n"
            +"\t\t\"outline\": \""+polyOutline+"\",\n"
            +"\t\t\"version\": "+plotVersion+",\n"
            +"\t\t\"is_order\": "+ (asTransaction ? "true" : "false")+"\n"            
            +"\n\t}\n]";
        System.out.println("POST " +PUT_PS_UPDATE_PLOT_URL.replace("%API_KEY%", teamApiKey));
        System.out.println("Body:\n" + requestBody);
        String jsonResponse = httpPOST(POST_PS_CREATE_PLOT_ORDER_URL.replace("%API_KEY%", teamApiKey), requestBody);

        //get plot id from json-response

        JsonObject jsonObject = new JsonParser().parse(jsonResponse).getAsJsonObject();
        int plotID = jsonObject.get("plot_id").getAsInt();
        if (asTransaction){
            String transactionID = jsonObject.get("order_id").getAsString();
            return new PlotCreateResult(plotID, transactionID);
        } else {
            return new PlotCreateResult(plotID);
        }
        //is_order = false:
        // {
        //     "plot_id": "55",
        //     "success": true
        // }
        //is_order = true
        // {
        //     "order_id": "<uuid>ba1dffb7-0764-4bb6-b7ad-5a67e674d3bb",
        //     "success": true
        // }        
    }
    public void confirmTransaction(String currentTransactionID, String teamApiKey) throws Exception {
        httpGET(GET_PS_CONFIRM_PLOT_ORDER_URL
            .replace("%API_KEY%", teamApiKey)
            .replace("%ORDER_ID%", currentTransactionID));

    }
    

}

