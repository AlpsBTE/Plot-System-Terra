package com.alpsbte.plotsystemterra.core.config;

public abstract class ConfigPaths {

    public static final String CHECK_FOR_UPDATES = "check-for-updates";
    public static final String SERVER_NAME = "server-name";
    public static final String DEV_MODE = "dev-mode";
    public static final String ABSOLUTE_SCHEMATIC_PATH = "absolute-schematic-path";


    // Database
    private static final String DATABASE = "database.";
    public static final String DATABASE_URL = DATABASE + "url";
    public static final String DATABASE_NAME = DATABASE + "dbname";
    public static final String DATABASE_USERNAME = DATABASE + "username";
    public static final String DATABASE_PASSWORD = DATABASE + "password";


    // PLOT PASTING
    public static final String WORLD_NAME = "world-name";
    public static final String PASTING_INTERVAL = "pasting-interval";
    public static final String BROADCAST_INFO = "broadcast-info";


    // FORMATTING
    public static final String MESSAGE_PREFIX = "message-prefix";
    public static final String MESSAGE_INFO_COLOUR = "info-colour";
    public static final String MESSAGE_ERROR_COLOUR = "error-colour";


    // CONFIG VERSION
    public static final String CONFIG_VERSION = "config-version";
}
