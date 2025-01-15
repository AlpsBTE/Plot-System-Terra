package com.alpsbte.plotsystemterra.core.api;

import com.alpsbte.plotsystemterra.core.config.ConfigPaths;
import com.alpsbte.plotsystemterra.core.config.ConfigUtil;

public class ApiConstants {
    private static String API_URL = "";
    private static String API_KEY = "";

    public static void updateApiConstants() {
        API_URL = ConfigUtil.getInstance().configs[0].getString(ConfigPaths.API_URL);
        API_KEY = ConfigUtil.getInstance().configs[0].getString(ConfigPaths.API_KEY);
    }

    public static String getApiUrl() {
        return API_URL;
    }

    public static String getApiKey() {
        return API_KEY;
    }
}
