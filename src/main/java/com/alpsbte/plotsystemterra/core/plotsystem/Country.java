package com.alpsbte.plotsystemterra.core.plotsystem;

public class Country {
    
    public final int id;
    public final int server_id;
    public final String head_id;    
    public final String continent;
    public final String name;

    public Country(int id, String head_id, String continent, String name, int server_id){
        this.id = id;
        this.head_id = head_id;
        this.continent = continent;
        this.name = name;
        this.server_id = server_id;
    }


}
