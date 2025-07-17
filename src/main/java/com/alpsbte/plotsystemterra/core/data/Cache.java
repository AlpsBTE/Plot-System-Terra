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

package com.alpsbte.plotsystemterra.core.data;

import com.alpsbte.plotsystemterra.utils.ExpiringHashMap;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Wrapper for managing {@linkplain Collection} of data as a {@linkplain ExpiringHashMap HashMap} cache.
 *
 * <p>For either a static (non-expiring) or expiring cache of any data entries.
 * The cache refresh behavior is explicitly updated via {@linkplain #refresh(Collection, Function)}.</p>
 *
 * <p>Note: Refreshing/Managing individual entry on the caching collection is not supported.</p>
 *
 * @see Cache#Cache() Create a static cache.
 * @see Cache#Cache(int) Create an expiring cache.
 * @see #refresh(Collection, Function)
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of cached values
 */
public class Cache<K, V> {
    private final ExpiringHashMap<K, V> cache;
    private Long expiry = null;

    private void putExpiring(K key, V value, long expiryTime) {
        synchronized (cache) {
            cache.putExpiring(key, value, expiryTime);
        }
    }

    /**
     * Create a static (non-expiring) cache.
     *
     * <ul>
     *     <li>Using {@link #expiry()} will always returns {@linkplain Optional#empty() empty}.</li>
     *     <li>Refreshing cache with {@link #refresh(Collection, Function)} will never expire its data.</li>
     * </ul>
     */
    public Cache() {
        this.cache = new ExpiringHashMap<>();
    }

    /**
     * Create an expiring cache.
     *
     * <ul>
     *     <li>Refreshed data will expires in X minute, defined by this constructor.</li>
     *     <li>Using {@link #expiry()} will returns the expiry timestamp each time it is refreshed.</li>
     * </ul>
     * @param expiryMinute Expiry duration in minutes; must be positive.
     */
    public Cache(int expiryMinute) {
        this.cache = new ExpiringHashMap<>(TimeUnit.MINUTES.toMillis(expiryMinute));
        this.expiry = System.currentTimeMillis();
    }

    /**
     * Get the current expiry timestamp of the cache, if any.
     *
     * <p>Non-empty value define the absolute time
     * in which the entire cache collection will expire.</p>
     *
     * @return Optional expiry time in milliseconds since epoch.
     */
    public Optional<Long> expiry() {
        return Optional.ofNullable(this.expiry);
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
     * Get a value from the cache by its ID.
     *
     * @param key The key to retrieve.
     * @return The cached value if present or {@code null}
     */
    public V get(K key) {
        return this.cache.get(key);
    }

    /**
     * Get a view of all cached values.
     *
     * @return A collection of cached projects (may be empty).
     */
    public Collection<V> get() {
        return this.cache.values();
    }

    /**
     * Check whether the given key is currently cached.
     *
     * @param key The key to retrieve.
     * @return {@code true} if the key is present in the cache.
     */
    public boolean hasCache(K key) {
        return this.cache.containsKey(key);
    }

    /**
     * Refresh the cache with new city project data.
     *
     * <p>If the cache is expiring, the expiry time of all entries is updated based on the cache's expiry delay.
     * If the cache is static, the entire cache is cleared and reloaded.</p>
     *
     * <p>Example: Caching Collection of Plots</p>
     * <blockquote>{@snippet :
     * import com.alpsbte.plotsystemterra.core.model.Plot;
     *
     * public void cachePlot(Cache<Integer, Plot> cache,
     *                       List<Plot> plots) {
     *     cache.refresh(plots, Plot::getId);
     * }}</blockquote>
     *
     * <p>Example: Caching Collection of City Projects</p>
     * <blockquote>{@snippet :
     * import com.alpsbte.plotsystemterra.core.model.CityProject;
     *
     * public void cacheCityProject(Cache<String, CityProject> cache,
     *                              List<CityProject> cityProjects) {
     *     cache.refresh(cityProjects, CityProject::getId);
     * }}</blockquote>
     *
     * @param newData The new collection of data instances to cache.
     * @param applier Applier on each data to retrieve its unique key.
     * @return This cache instance after refresh (for chaining).
     */
    public Cache<K, V> refresh(Collection<V> newData, Function<V, K> applier) {

        // Refresh the expiry if we're using expiry cache
        if (expiry().isPresent()) {
            this.expiry = System.currentTimeMillis() + this.cache.getExpiryDelay();

            for (V data : newData)
                this.putExpiring(applier.apply(data), data, this.expiry);

            return this;
        }

        // Clear all cache to override new one with static cache
        this.cache.clear();

        for (V data : newData)
            this.cache.putNotExpiring(applier.apply(data), data);

        return this;
    }
}
