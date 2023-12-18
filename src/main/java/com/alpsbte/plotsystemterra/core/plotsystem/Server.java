package com.alpsbte.plotsystemterra.core.plotsystem;

public class Server {
    public final int id;
    public final int ftp_configuration_id;
    public final String name;

    public Server(int id, int ftp_configuration_id, String name){
        this.id=id;
        this.ftp_configuration_id = ftp_configuration_id;
        this.name = name;
    }

}
