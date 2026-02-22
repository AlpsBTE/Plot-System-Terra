package com.alpsbte.plotsystemterra.core.model;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

public class Plot {
    @Getter
    private final int id;
    @Getter
    private final String status;
    @Getter
    private final String cityProjectId;
    @Getter
    private final double plotVersion;
    @Getter
    private final String mcVersion;
    @Getter
    private final @Nullable String createdBy;
    @Getter
    private final @Nullable String owner;
    @Getter
    private byte[] completedSchematic = null;

    public Plot(int id, String status, String cityProjectId, double plotVersion, String mcVersion) {
        this(id, status, cityProjectId, plotVersion, mcVersion, null, null, null);
    }

    public Plot(int id, String status, String cityProjectId, double plotVersion, String mcVersion, @Nullable String createdBy) {
        this(id, status, cityProjectId, plotVersion, mcVersion, createdBy, null, null);
    }

    public Plot(int id, String status, String cityProjectId, double plotVersion, String mcVersion, byte[] completedSchematic) {
        this(id, status, cityProjectId, plotVersion, mcVersion, null, null, completedSchematic);
    }

    public Plot(int id, String status, String cityProjectId, double plotVersion, String mcVersion, @Nullable String createdBy, @Nullable String owner, byte[] completedSchematic) {
        this.id = id;
        this.status = status;
        this.cityProjectId = cityProjectId;
        this.plotVersion = plotVersion;
        this.mcVersion = mcVersion;
        this.createdBy = createdBy;
        this.owner = owner;
        this.completedSchematic = completedSchematic;
    }
}
