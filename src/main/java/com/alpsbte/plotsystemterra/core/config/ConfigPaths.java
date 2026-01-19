/*
 *  The MIT License (MIT)
 *
 *  Copyright © 2021-2025, Alps BTE <bte.atchli@gmail.com>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.alpsbte.plotsystemterra.core.config;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ConfigPaths {
    public static final String DEV_MODE = "dev-mode";

    // Caching
    public static final String CACHE_DURATION_MINUTES = "cache-duration-minutes";

    // Data Mode
    public static final String DATA_MODE = "data-mode";

    // API
    public static final String API = "api.";
    public static final String API_URL = API + "api-url";
    public static final String API_KEY = API + "api-key";

    // PLOT SCANNING
    private static final String ENVIRONMENT = "environment.";
    public static final String ENVIRONMENT_ENABLED = ENVIRONMENT + "enabled";
    public static final String ENVIRONMENT_RADIUS = ENVIRONMENT + "radius";

    // PLOT PASTING
    public static final String SERVER_NAME = "server-name";
    public static final String WORLD_NAME = "world-name";
    public static final String PASTING_INTERVAL = "pasting-interval";
    public static final String BROADCAST_INFO = "broadcast-info";

    // FORMATTING
    public static final String CHAT_FORMAT = "chat-format.";
    public static final String CHAT_FORMAT_INFO_PREFIX = CHAT_FORMAT + "info-prefix";
    public static final String CHAT_FORMAT_ALERT_PREFIX = CHAT_FORMAT + "alert-prefix";
}
