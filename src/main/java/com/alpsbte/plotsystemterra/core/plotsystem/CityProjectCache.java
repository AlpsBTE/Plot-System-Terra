package com.alpsbte.plotsystemterra.core.plotsystem;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.model.CityProject;
import com.alpsbte.plotsystemterra.utils.ExpiringCacheMap;

import java.util.concurrent.TimeUnit;

public class CityProjectCache {


    private final ExpiringCacheMap<String, CityProject> cache = new ExpiringCacheMap<>(TimeUnit.SECONDS.toMillis(10));
    private final static long EXPIRY_TIME_ONLINE = TimeUnit.MINUTES.toMillis(3);

    private void putExpiring(String cityProjectID, CityProject cityProject, long expiryTime) {
        synchronized (cache) {
            cache.putExpiring(cityProjectID, cityProject, expiryTime);
        }
    }

    public CityProjectCache(PlotSystemTerra plugin) {

    }
}
