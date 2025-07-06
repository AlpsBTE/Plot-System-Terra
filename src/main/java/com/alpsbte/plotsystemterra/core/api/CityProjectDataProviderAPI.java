package com.alpsbte.plotsystemterra.core.api;

import com.alpsbte.plotsystemterra.core.data.CityProjectDataProvider;
import com.alpsbte.plotsystemterra.core.data.DataException;
import com.alpsbte.plotsystemterra.core.model.CityProject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CityProjectDataProviderAPI implements CityProjectDataProvider {

    @SuppressWarnings("unchecked") // org.json.simple.JSONArray is marked as unchecked internally. can't do anything about this
    @Override
    public List<CityProject> getCityProjects() throws DataException {
        List<CityProject> output = new ArrayList<>();
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(ApiConstants.getApiUrl() + "cityproject"))
                    .header("x-api-key", ApiConstants.getApiKey())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) throw new DataException("Invalid status code!: " + response.body());

            JSONParser parser = new JSONParser();
            JSONArray jsonArray = (JSONArray) parser.parse(response.body());
            jsonArray.forEach(object -> {
                JSONObject jsonObj = (JSONObject) object;
                String id = (String) jsonObj.get("id");
                String countryCode = (String) jsonObj.get("countryCode");
                boolean isVisible = (boolean) jsonObj.get("isVisible");
                String material = (String) jsonObj.get("material");
                String customModelData = (String) jsonObj.get("customModelData");
                String serverName = (String) jsonObj.get("serverName");

                output.add(new CityProject(id, countryCode, isVisible, material, customModelData, serverName));
            });
        } catch (IOException | InterruptedException | ParseException e) {
            Thread.currentThread().interrupt();
            throw new DataException(e.getMessage(), e);
        }

        return output;
    }

    @Override
    public CompletableFuture<List<CityProject>> getCityProjectsAsync() throws DataException {
        CompletableFuture<List<CityProject>> completableFuture = new CompletableFuture<>();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                completableFuture.complete(getCityProjects());
                return null;
            });
        }
        return completableFuture;
    }

    @Override
    public CityProject getCityProject(String id) throws DataException {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(ApiConstants.getApiUrl() + "cityproject/" + id))
                    .header("x-api-key", ApiConstants.getApiKey())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) throw new DataException("Invalid status code!: " + response.body());

            JSONParser parser = new JSONParser();
            JSONObject jsonObj = (JSONObject) parser.parse(response.body());

            String cityProjectId = (String) jsonObj.get("id");
            String countryCode = (String) jsonObj.get("countryCode");
            boolean isVisible = (boolean) jsonObj.get("isVisible");
            String material = (String) jsonObj.get("material");
            String customModelData = (String) jsonObj.get("customModelData");
            String serverName = (String) jsonObj.get("customModelData");

            return new CityProject(cityProjectId, countryCode, isVisible, material, customModelData, serverName);
        } catch (IOException | InterruptedException | ParseException e) {
            Thread.currentThread().interrupt();
            throw new DataException(e.getMessage(), e);
        }
    }

    @Override
    public CompletableFuture<CityProject> getCityProjectAsync(String id) throws DataException {
        CompletableFuture<CityProject> completableFuture = new CompletableFuture<>();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                completableFuture.complete(getCityProject(id));
                return null;
            });
        }
        return completableFuture;
    }
}
