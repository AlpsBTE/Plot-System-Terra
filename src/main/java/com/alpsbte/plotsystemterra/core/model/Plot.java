package com.alpsbte.plotsystemterra.core.model;

public class Plot {
    private final int id;
    private final String status;
    private final String cityProjectId;
    private final double plotVersion;
    private final String mcVersion;
    private byte[] completedSchematic = null;

    public Plot(int id, String status, String cityProjectId, double plotVersion, String mcVersion) {
        this.id = id;
        this.status = status;
        this.cityProjectId = cityProjectId;
        this.plotVersion = plotVersion;
        this.mcVersion = mcVersion;
    }

    public Plot(int id, String status, String cityProjectId, double plotVersion, String mcVersion, byte[] completedSchematic) {
        this.id = id;
        this.status = status;
        this.cityProjectId = cityProjectId;
        this.plotVersion = plotVersion;
        this.mcVersion = mcVersion;
        this.completedSchematic = completedSchematic;
    }

    public int getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getCityProjectId() {
        return cityProjectId;
    }

    public double getPlotVersion() {
        return plotVersion;
    }

    public String getMcVersion() {
        return mcVersion;
    }

    public byte[] getCompletedSchematic() {
        return completedSchematic;
    }
}
