package com.alpsbte.plotsystemterra.core.model;

public class Plot {
    private final int id;
    private final String status;
    private final double plotVersion;
    private final int cityProjectId;
    private final String[] mcCoordinates;

    public Plot(int id, String status, double plotVersion, int cityProjectId, String[] mcCoordinates) {
        this.id = id;
        this.status = status;
        this.plotVersion = plotVersion;
        this.cityProjectId = cityProjectId;
        this.mcCoordinates = mcCoordinates;
    }

    public int getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public double getPlotVersion() {
        return plotVersion;
    }

    public int getCityProjectId() {
        return cityProjectId;
    }

    public String[] getMcCoordinates() {
        return mcCoordinates;
    }
}
