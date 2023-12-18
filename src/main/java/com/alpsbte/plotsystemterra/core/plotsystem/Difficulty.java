package com.alpsbte.plotsystemterra.core.plotsystem;

public class Difficulty {
    public final int difficultyID;
    public final float multiplier;
    public final String name;
    public final int score_requirment;



    public Difficulty(String name, int id, float multiplier, int score_requirment)
    {
        this.name = name;
        this.difficultyID= id;
        this.multiplier = multiplier;
        this.score_requirment = score_requirment;
    }

}
