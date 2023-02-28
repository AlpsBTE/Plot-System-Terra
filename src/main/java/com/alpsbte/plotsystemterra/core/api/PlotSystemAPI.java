package com.alpsbte.plotsystemterra.core.api;

import org.bukkit.Bukkit;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class PlotSystemAPI {

    private static String GET_BUILDERS_URL = "http://%DOMAIN%:8080/api/builders";
    private static String HOST = "psapi.buildtheearth.net";

    public static List<UUID> getBuilders() {
        List<UUID> builders = new ArrayList<>();
        try {

            //Pasrse this JSON string to a JSONArray "[{"uuid":"299e70fa-6c62-4e6c-bef5-4bab04a91e08","name":"Mondaysucc","score":0,"completed_plots":0,"first_slot":null,"second_slot":null,"third_slot":null,"lang":null,"setting_plot_type":1},{"uuid":"307940e1-ecae-4fa7-997d-e55812bb1f01","name":"filippo_the_king","score":0,"completed_plots":0,"first_slot":null,"second_slot":null,"third_slot":null,"lang":null,"setting_plot_type":1}"
            JSONParser parser = new JSONParser();
            String json = get(new URL(GET_BUILDERS_URL.replace("%DOMAIN%", HOST)));
            JSONArray jsonArray = (JSONArray) parser.parse(json);

            //Iterate over the JSONArray and add the UUIDs to the list
            jsonArray.forEach(object -> {
                builders.add(UUID.fromString(((org.json.simple.JSONObject) object).get("uuid").toString()));
            });
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while getting the list of builders from the PlotSystem API!");
            e.printStackTrace();
        }
        return builders;
    }

    // A function that returns the content of a GET Request from a given URL
    private static String get(URL url) {
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            // Add headers to the request
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            return content.toString();
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while performin a GET request to the PlotSystem API!");
            e.printStackTrace();
        }

        return null;
    }

}
