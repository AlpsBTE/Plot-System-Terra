package com.alpsbte.plotsystemterra.core.config;

public abstract class ConfigPaths {

    public static final String CHECK_FOR_UPDATES = "check-for-updates";
    public static final String DEV_MODE = "dev-mode";


    // Database
    private static final String DATABASE = "database.";
    public static final String DATABASE_URL = DATABASE + "url";
    public static final String DATABASE_NAME = DATABASE + "dbname";
    public static final String DATABASE_USERNAME = DATABASE + "username";
    public static final String DATABASE_PASSWORD = DATABASE + "password";


    // FORMATTING
    public static final String MESSAGE_PREFIX = "message-prefix";
    public static final String MESSAGE_INFO_COLOUR = "info-colour";
    public static final String MESSAGE_ERROR_COLOUR = "error-colour";


    // CONFIG VERSION
    public static final String CONFIG_VERSION = "config-version";
}
