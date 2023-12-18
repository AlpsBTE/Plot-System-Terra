package com.alpsbte.plotsystemterra.core.plotsystem;

import com.sk89q.worldedit.Vector;

public class Plot {

    public Plot(int id, String status, int city_project_id, Vector mc_coordinates, int pasted, double version){
        this.status=status;
        this.id = id;
        this.city_project_id = city_project_id;
        this.mc_coordinates = mc_coordinates;
        this.pasted = pasted;
        if (version <= 0)
            this.version = 2;
        else
            this.version = version;
    }

    public final String status;
    public final int id;
    public final int city_project_id;
    public final Vector mc_coordinates;
    public final double version;
    public final int pasted;

}