package com.alpsbte.plotsystemterra.core.plotsystem;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.model.CityProject;
import com.alpsbte.plotsystemterra.utils.ExpiringCacheMap;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static net.kyori.adventure.text.Component.text;

/**
 * Handles caching of city project data, either as a permanent cache
 * or with automatic expiration and periodic refresh.
 *
 * <p>For expiring caches, this class schedules an asynchronous task to re-fetch
 * project data shortly before each cache entry's expiration time.</p>
 */
public class CityProjectData {

    private static final long CACHE_MARGIN_MILLIS = TimeUnit.SECONDS.toMillis(5);
    private final ExpiringCacheMap<String, CityProject> cache;
    private @Nullable BukkitTask expiryTask;
    private int expiryMinute;

    private void putExpiring(String cityProjectID, CityProject cityProject, long expiryTime) {
        synchronized (cache) {
            cache.putExpiring(cityProjectID, cityProject, expiryTime);
        }
    }

    /**
     * Creates a permanent cache for all city project data at construction time.
     */
    public CityProjectData() {
        this.cache = new ExpiringCacheMap<>(CACHE_MARGIN_MILLIS);

        this.fetchDataAsync(cityProjects -> cityProjects.forEach(cityProject -> {
            String id = cityProject.getId();
            this.cache.putNotExpiring(id, cityProject);
        }));
    }

    /**
     * Creates an expiring cache that automatically refreshes project data on a fixed interval.
     *
     * <p>This sets up an asynchronous task to refresh city project entries shortly before they expire.</p>
     *
     * @param plugin        the plugin instance used to schedule background tasks
     * @param expiryMinute  the time (in minutes) after which cached entries should expire and be refreshed
     * @throws IllegalArgumentException if {@code expiryMinute} is less than or equal to 0
     */
    public CityProjectData(Plugin plugin, int expiryMinute) {
        if(expiryMinute <= 0) throw new IllegalArgumentException("Cannot have CityProjectData cache of 0 minute of expiry.");

        this.expiryMinute = expiryMinute;
        final long expiryTimeOnline = TimeUnit.MINUTES.toMillis(expiryMinute);
        final long taskIntervalMillis = expiryTimeOnline - CACHE_MARGIN_MILLIS;

        this.cache = new ExpiringCacheMap<>(expiryTimeOnline);

        this.expiryTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            final long currentTime = System.currentTimeMillis();
            final long newExpiry = currentTime + expiryTimeOnline;

            this.fetchDataAsync(cityProjects -> cityProjects.forEach(project -> {
                try {
                    long expiry = cache.getExpiryTime(project.getId());
                    if (expiry - currentTime <= CACHE_MARGIN_MILLIS)
                        this.putExpiring(project.getId(), project, newExpiry);
                } catch (IllegalArgumentException ex) {
                    this.putExpiring(project.getId(), project, newExpiry);
                }
            }));
        }, 1L, taskIntervalMillis / 50);
    }

    /**
     * Forces a full refresh of the city project data cache by reconstructing the {@link CityProjectData} instance.
     *
     * @param data the existing {@link CityProjectData} instance to refresh
     * @return a new {@linkplain  CityProjectData} instance with freshly loaded data
     */
    @Contract("_ -> new")
    public static @NotNull CityProjectData refresh(@NotNull CityProjectData data) {
        if(data.expiryTask != null) {
            data.expiryTask.cancel();
            return new CityProjectData(data.expiryTask.getOwner(), data.expiryMinute);
        }
        else return new CityProjectData();
    }


    /**
     * Fetch city projects data asynchronously and handle for any error.
     *
     * @param retriever Callback to retrieve fetched data
     */
    private void fetchDataAsync(Consumer<List<CityProject>> retriever) {
        CompletableFuture.supplyAsync(() -> PlotSystemTerra.getDataProvider().getCityProjectDataProvider().getCityProjects())
            .exceptionally(e -> {
                PlotSystemTerra.getPlugin().getComponentLogger().error(text("An error occurred fetching city project data"), e);
                return List.of();
            }).thenAccept((cityProjects) -> {
                if (cityProjects.isEmpty()) {
                    PlotSystemTerra.getPlugin().getComponentLogger().error(text("Fetched city project data is empty!"));
                    retriever.accept(List.of());
                    return;
                }

                retriever.accept(cityProjects);
            });
    }

    public CityProject getFromCache(String id) {
        return this.cache.get(id);
    }

    public Collection<CityProject> getCache() {
        return this.cache.values();
    }

    public Set<String> getCachedID() {
        return this.cache.keySet();
    }

    public boolean hasProjectID(String id) {
        return this.cache.containsKey(id);
    }
}
