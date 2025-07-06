package com.alpsbte.plotsystemterra.core.plotsystem;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.model.CityProject;
import com.alpsbte.plotsystemterra.utils.ExpiringHashMap;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static net.kyori.adventure.text.Component.text;

/**
 * Handles caching of city project data, with support for both static (non-expiring)
 * and expiring cache configurations.
 *
 * @see #getCache()
 */
public class CityProjectData {
    private final Cache cache;
    private CompletableFuture<Cache> refresh = null;

    /**
     * Fetch for city project data asynchronously using the plugin's data provider.
     *
     * <p>This handle exceptions and log to the plugin's logger.</p>
     *
     * @return A {@link CompletableFuture} that complete with the city project data.
     * @see com.alpsbte.plotsystemterra.core.data.CityProjectDataProvider#getCityProjects()
     */
    public static CompletableFuture<List<CityProject>> fetchDataAsync() {
        Supplier<List<CityProject>> action = () -> PlotSystemTerra.getDataProvider().getCityProjectDataProvider().getCityProjects();

        return CompletableFuture.supplyAsync(action).orTimeout((long) 60.0, TimeUnit.SECONDS).handle((cityProjects, error) -> {
            if(error != null) {
                PlotSystemTerra.getPlugin().getComponentLogger().error(text("An error occurred fetching city project data"), error);
                return List.of();
            }

            if (cityProjects.isEmpty())
                PlotSystemTerra.getPlugin().getComponentLogger().error(text("Fetched city project data is empty!"));

            PlotSystemTerra.getPlugin().getComponentLogger().info(text("Fetched #" + cityProjects.size()));

            return cityProjects;
        });
    }

    /**
     * Creates a static cache for all city project data.
     *
     * <p>Calling {@link #getCache()} will fetch for the city project data every call.</p>
     */
    public CityProjectData() {
        this.cache = new Cache();
    }

    /**
     * Creates an expiring cache that gets expired on X minutes of lifetime.
     *
     * <p>Calling {@link #getCache()} will return the cached data and refresh if expired.</p>
     *
     * @param expiryMinute  the time (in minutes) after which cached entries should expire and be refreshed
     * @throws IllegalArgumentException if {@code expiryMinute} is less than or equal to 0
     */
    public CityProjectData(int expiryMinute) {
        this.cache = new Cache(expiryMinute);

        ExpiringHashMap.runExpiryThread();
    }

    /**
     * Retrieve the current city project cache, refreshing it if needed.
     *
     * @return A {@link CompletableFuture} that will complete with the up-to-date {@link Cache}.
     */
    public CompletableFuture<Cache> getCache() {
        if (!needsRefresh()) // Valid cache is returned instantly
            return CompletableFuture.completedFuture(this.cache);

        // Thread-safe access to the cache refresh logic.
        synchronized (this) {
            if (this.refresh != null)
                return this.refresh;

            // Trigger new async fetch and cache refresh
            this.refresh = CityProjectData.fetchDataAsync().thenApply(this.cache::refresh);

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
    public Cache refreshCache(List<CityProject> newData) {
        return this.cache.refresh(newData);
    }

    /**
     * Internal cache wrapper for city project data.
     *
     * <p>Manages either a static (non-expiring) or expiring cache of {@link CityProject} entries.
     * The cache refresh behavior is explicitly updated via {@link #refresh(List)}.</p>
     * 
     * @see #refresh(List) 
     */
    public static class Cache {
        private final ExpiringHashMap<String, CityProject> cache;
        private Long expiry = null;

        private void putExpiring(CityProject cityProject, long expiryTime) {
            synchronized (cache) {
                cache.putExpiring(cityProject.getId(), cityProject, expiryTime);
            }
        }

        /** Create a static (non-expiring) cache. */
        private Cache() {
            this.cache = new ExpiringHashMap<>();
        }

        /**
         * Create an expiring cache.
         *
         * @param expiryMinute Expiry duration in minutes; must be positive.
         */
        private Cache(int expiryMinute) {
            this.cache = new ExpiringHashMap<>(TimeUnit.MINUTES.toMillis(expiryMinute));
            this.expiry = System.currentTimeMillis();
        }

        /**
         * Get the current expiry timestamp of the cache, if any.
         *
         * @return Optional expiry time in milliseconds since epoch.
         */
        private Optional<Long> expiry() {
            return Optional.ofNullable(this.expiry);
        }

        /**
         * Refresh the cache with new city project data.
         *
         * <p>If the cache is expiring, the expiry time of all entries is updated based on the cache's expiry delay.
         * If the cache is static, the entire cache is cleared and reloaded.
         *
         * @param newData The new list of {@link CityProject} instances to cache.
         * @return This cache instance after refresh (for chaining).
         */
        private Cache refresh(List<CityProject> newData) {

            // Refresh the expiry if we're using expiry cache
            if(expiry().isPresent()) {
                this.expiry = System.currentTimeMillis() + this.cache.getExpiryDelay();
                PlotSystemTerra.getPlugin().getComponentLogger().info(text("Refreshing expiry cache"));

                for(CityProject data : newData)
                    this.putExpiring(data, this.expiry);

                return this;
            }

            // Clear all cache to override new one with static cache
            this.cache.clear();
            PlotSystemTerra.getPlugin().getComponentLogger().info(text("Refreshing static cache"));

            for(CityProject project : newData)
                this.cache.putNotExpiring(project.getId(), project);

            return this;
        }

        /**
         * Check if the cache is empty.
         *
         * @return {@code true} if no entries are cached; {@code false} otherwise.
         */
        public boolean isEmpty() {
            return this.cache.isEmpty();
        }

        /**
         * Get a city project from the cache by its ID.
         *
         * @param id The project ID.
         * @return The {@link CityProject} if present or {@code null}
         */
        public CityProject get(String id) {
            return this.cache.get(id);
        }

        /**
         * Get a view of all cached {@link CityProject} values.
         *
         * @return A collection of cached projects (may be empty).
         */
        public Collection<CityProject> get() {
            return this.cache.values();
        }

        /**
         * Check whether a project with the given ID is currently cached.
         *
         * @param id The project ID.
         * @return {@code true} if the project is present in the cache.
         */
        public boolean hasProjectID(String id) {
            return this.cache.containsKey(id);
        }
    }
}
