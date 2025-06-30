/*
 *  The MIT License (MIT)
 *
 *  Copyright © 2021-2025, Alps BTE <bte.atchli@gmail.com>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.alpsbte.plotsystemterra.core.api;

import com.alpsbte.plotsystemterra.core.data.DataException;
import com.alpsbte.plotsystemterra.core.data.PlotDataProvider;
import com.alpsbte.plotsystemterra.core.model.Plot;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
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

public class PlotDataProviderAPI extends PlotDataProvider {
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
            byte[] completedSchematic = Base64.getDecoder().decode((String) jsonObj.get("completedSchematic"));
            return new Plot(
                    id,
                    status,
                    cityProjectId,
                    plotVersion,
                    mcVersion,
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
                byte[] completedSchematic = Base64.getDecoder().decode((String) jsonObj.get("completedSchematic"));

                output.add(new Plot(
                        id,
                        status,
                        cityProjectId,
                        plotVersion,
                        mcVersion,
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
