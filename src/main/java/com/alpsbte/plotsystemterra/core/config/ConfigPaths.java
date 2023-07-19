package com.alpsbte.plotsystemterra.core.config;

public abstract class ConfigPaths {

    public static final String CHECK_FOR_UPDATES = "check-for-updates";
    public static final String SERVER_NAME = "server-name";
    public static final String DEV_MODE = "dev-mode";


    // Data Mode
    public static final String DATA_MODE = "data-mode";

    // API
    public static final String API = "api.";
    public static final String API_URL = API + "api-url";
    public static final String API_PORT = API + "port";
    public static final String API_KEY = API + "api-key";

    // Database
    private static final String DATABASE = "database.";
    public static final String DATABASE_URL = DATABASE + "db-url";
    public static final String DATABASE_NAME = DATABASE + "dbname";
    public static final String DATABASE_USERNAME = DATABASE + "username";
    public static final String DATABASE_PASSWORD = DATABASE + "password";


    // PLOT SCANNING
    private static final String ENVIRONMENT = "environment.";
    public static final String ENVIRONMENT_ENABLED = ENVIRONMENT + "enabled";
    public static final String ENVIRONMENT_RADIUS = ENVIRONMENT + "radius";


    // PLOT PASTING
    public static final String WORLD_NAME = "world-name";
    public static final String FAST_MODE = "fast-mode";
    public static final String PASTING_INTERVAL = "pasting-interval";
    public static final String BROADCAST_INFO = "broadcast-info";


    // FORMATTING
    public static final String MESSAGE_PREFIX = "message-prefix";
    public static final String MESSAGE_INFO_COLOUR = "info-colour";
    public static final String MESSAGE_ERROR_COLOUR = "error-colour";


    // CONFIG VERSION
    public static final String CONFIG_VERSION = "config-version";
}
