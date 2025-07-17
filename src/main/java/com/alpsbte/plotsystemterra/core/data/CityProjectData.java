package com.alpsbte.plotsystemterra.core.data;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.model.CityProject;
import com.alpsbte.plotsystemterra.utils.ExpiringHashMap;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static net.kyori.adventure.text.Component.text;

/**
 * Handles caching of city project data, with support for both static (non-expiring)
 * and expiring cache configurations.
 *
 * @see #getCache()
 */
public class CityProjectData {
    private final CityProjectDataProvider provider;
    private final Cache<String, CityProject> cache;
    private CompletableFuture<Cache<String, CityProject>> refresh = null;

    /**
     * Fetch for city project data asynchronously using the plugin's data provider.
     *
     * <p>This handle exceptions and log to the plugin's logger.</p>
     *
     * @return A {@link CompletableFuture} that complete with the city project data.
     * @see com.alpsbte.plotsystemterra.core.data.CityProjectDataProvider#getCityProjects()
     */
    public CompletableFuture<List<CityProject>> fetchDataAsync() {
        return CompletableFuture.supplyAsync(this.provider::getCityProjects).orTimeout(60L, TimeUnit.SECONDS).handle((cityProjects, error) -> {
            if(error != null) {
                PlotSystemTerra.getPlugin().getComponentLogger().error(text("An error occurred fetching city project data"), error);
                return List.of();
            }

            if (cityProjects.isEmpty())
                PlotSystemTerra.getPlugin().getComponentLogger().error(text("Fetched city project data is empty!"));

            return cityProjects;
        });
    }

    /**
     * Creates a {@link CityProjectData} instance with optional expiration-based caching.
     *
     * <p>Calling {@link #getCache()} will return the cached data and refresh if expired.</p>
     *
     * @param provider The data provider used to load city project data.
     * @param expiryMinute The cache expiry duration in minutes. Use any value less than 0 for a static cache.
     */
    public CityProjectData(CityProjectDataProvider provider, int expiryMinute) {
        this.provider = provider;

        if (expiryMinute < 0) {
            this.cache = new Cache<>(); // Static cache
        } else {
            this.cache = new Cache<>(expiryMinute); // Expiring cache
            ExpiringHashMap.runExpiryThread();      // Background expiry thread
        }
    }

    /**
     * Retrieve the current city project cache, refreshing it if needed.
     *
     * @return A {@link CompletableFuture} that will complete with the up-to-date {@link Cache}.
     */
    public CompletableFuture<Cache<String, CityProject>> getCache() {
        if (!needsRefresh()) // Valid cache is returned instantly
            return CompletableFuture.completedFuture(this.cache);

        // Thread-safe access to the cache refresh logic.
        synchronized (this) {
            if (this.refresh != null)
                return this.refresh;

            // Trigger new async fetch and cache refresh
            this.refresh = this.fetchDataAsync().thenApply(this::refreshCache);

            this.refresh.whenComplete((cached, error) -> {
                synchronized (this) {
                    this.refresh = null;
                }
            });

            return this.refresh;
        }
    }

    /**
     * Determine whether the cache should be refreshed before use.
     *
     * <p>Expiry cache will return {@code true} only if the current (expired) cache has been cleared.
     * <p>Static cache always returns {@code true}, to force a refresh on every access.
     *
     * @return {@code true} if the cache should be refreshed, {@code false} otherwise.
     */
    public boolean needsRefresh() {
        if(this.cache.expiry().isPresent())
            return this.cache.isEmpty() && System.currentTimeMillis() >= this.cache.expiry().get();

        return true;
    }

    /**
     * Forcefully refresh the cache using the given list of {@link CityProject} entries.
     *
     * @param newData The new project data to insert into the cache.
     * @return The updated {@link Cache} instance.
     */
    public Cache<String, CityProject> refreshCache(List<CityProject> newData) {
        return this.cache.refresh(newData, CityProject::getId);
    }
}
