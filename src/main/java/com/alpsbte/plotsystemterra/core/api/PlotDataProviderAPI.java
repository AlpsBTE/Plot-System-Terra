package com.alpsbte.plotsystemterra.core.api;

import com.alpsbte.plotsystemterra.core.data.DataException;
import com.alpsbte.plotsystemterra.core.data.PlotDataProvider;
import com.alpsbte.plotsystemterra.core.model.Plot;
import okhttp3.*;
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
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class PlotDataProviderAPI implements PlotDataProvider {
    @Override
    public Plot getPlot(int id) throws DataException {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(ApiConstants.getApiUrl() + "plot/" + id))
                    .header("x-api-key", ApiConstants.getApiKey())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) throw new DataException("Invalid status code!: " + response.body());

            JSONParser parser = new JSONParser();
            JSONObject jsonObj = (JSONObject) parser.parse(response.body());

            String status = (String) jsonObj.get("status");
            String cityProjectId = (String) jsonObj.get("cityProjectId");
            double plotVersion = ((Number) jsonObj.get("plotVersion")).doubleValue();
            String mcVersion = (String) jsonObj.get("mcVersion");
            String createdByUuid = (String) jsonObj.get("createdByUuid");
            String ownerUuid = (String) jsonObj.get("ownerUuid");
            byte[] completedSchematic = Base64.getDecoder().decode((String) jsonObj.get("completedSchematic"));
            return new Plot(
                    id,
                    status,
                    cityProjectId,
                    plotVersion,
                    mcVersion,
                    createdByUuid,
                    ownerUuid,
                    completedSchematic
            );
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new DataException(e.getMessage(), e);
        }
    }

    @Override
    public int createPlot(String cityProjectId, String difficultyId, String outlineBounds, UUID createPlayerUUID, byte[] initialSchematic) throws DataException {
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("cityProjectId", cityProjectId)
                .addFormDataPart("difficultyId", difficultyId)
                .addFormDataPart("outlineBounds", outlineBounds)
                .addFormDataPart("createdBy", createPlayerUUID.toString())
                .addFormDataPart("initialSchematic", "initialSchematic.schematic",
                        RequestBody.create(initialSchematic, MediaType.parse("application/octet-stream")))
                .build();
        Request request = new Request.Builder()
                .url(ApiConstants.getApiUrl() + "plot")
                .method("POST", body)
                .addHeader("x-api-key", ApiConstants.getApiKey())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() != 200) throw new DataException("Invalid status code!: " + response.code() + " " + response.body().string());

            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(response.body().string());

            String stringId = jsonObject.get("id").toString();
            return Integer.parseInt(stringId);
        } catch (Exception e) {
            throw new DataException(e.getMessage(), e);
        }
    }

    @Override
    public void setPasted(int id) throws DataException {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(ApiConstants.getApiUrl() + "plot/" + id + "/setPasted"))
                    .header("x-api-key", ApiConstants.getApiKey())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new DataException("Invalid status code!: " + response.body());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    // org.json.simple.JSONArray is marked as unchecked internally. can't do anything about this
    @Override
    public List<Plot> getPlotsToPaste() throws DataException {
        List<Plot> output = new ArrayList<>();
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(ApiConstants.getApiUrl() + "plot/toPaste"))
                    .header("x-api-key", ApiConstants.getApiKey())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new DataException("Invalid status code!: " + response.body());

            JSONParser parser = new JSONParser();
            JSONArray jsonArray = (JSONArray) parser.parse(response.body());
            jsonArray.forEach(object -> {
                JSONObject jsonObj = (JSONObject) object;
                int id = ((Number) jsonObj.get("id")).intValue();
                String status = (String) jsonObj.get("status");
                String cityProjectId = (String) jsonObj.get("cityProjectId");
                double plotVersion = ((Number) jsonObj.get("plotVersion")).doubleValue();
                String mcVersion = (String) jsonObj.get("mcVersion");
                String createdByUuid = (String) jsonObj.get("createdByUuid");
                String ownerUuid = (String) jsonObj.get("ownerUuid");
                byte[] completedSchematic = Base64.getDecoder().decode((String) jsonObj.get("completedSchematic"));

                output.add(new Plot(
                        id,
                        status,
                        cityProjectId,
                        plotVersion,
                        mcVersion,
                        createdByUuid,
                        ownerUuid,
                        completedSchematic
                ));
            });
        } catch (IOException | InterruptedException | ParseException e) {
            Thread.currentThread().interrupt();
            throw new DataException(e.getMessage(), e);
        }
        return output;
    }
}
