package com.alpsbte.plotsystemterra.core.plotsystem;

public class FTPConfiguration {

    public final int id;
    public final  String schematics_path;
    public final  String address;
    public final  int port;
    public final  boolean isSFTP;
    public final  String username;
    public final  String password;

    public FTPConfiguration(int id, String schematics_path, String address, int port, boolean isSFTP, String username, String password) {
        this.id = id;

        if (schematics_path != null) {
            schematics_path = schematics_path.startsWith("/") ? schematics_path.substring(1, schematics_path.length()) : schematics_path;
            schematics_path = schematics_path.endsWith("/") ? schematics_path.substring(0, schematics_path.length() - 1) : schematics_path;
        }
        this.schematics_path = schematics_path;
        this.address = address;
        this.port = port;
        this.isSFTP = isSFTP;
        this.username = username;
        this.password = password;
    }

}
