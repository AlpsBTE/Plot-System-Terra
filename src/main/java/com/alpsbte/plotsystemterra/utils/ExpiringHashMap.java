package com.alpsbte.plotsystemterra.utils;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * HashMap cache with time-based expiration of entries.
 *
 * <p>Entries can be added with an expiration time, after which they will be
 * automatically removed by a background cleanup thread if not refreshed.</p>
 *
 * <p>{@link ExpiringHashMap#runExpiryThread()} must be called
 * once during application startup to clear expired cache automatically.</p>
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of cached values
 */
public class ExpiringHashMap<K, V> extends HashMap<K, V> {

    private static final long CACHE_REFRESH_MILLIS = 5000;
    private static final ExpiryThread expiryThread = new ExpiryThread();

    private final HashMap<K, Long> expiryTimes = new HashMap<>();
    private final long expiryDelay;

    /**
     * Starts the global expiration thread for cleaning up expired entries
     * from all active {@link ExpiringHashMap} instances.
     *
     * <p>Note: If {@link ExpiringHashMap} is used without calling this method,
     * expired entries will remain in memory and will not be cleaned automatically.</p>
     */
    public static void runExpiryThread() {
        if(!expiryThread.isAlive())
            expiryThread.start();
    }

    /**
     * Create a new expiry map with default expiry duration.
     */
    public ExpiringHashMap() {
        this.expiryDelay = CACHE_REFRESH_MILLIS;
        ExpiryThread.references.add(new WeakReference<>(this));
    }

    /**
     * Create a new expiry map with an expiry duration.
     *
     * @param expiryDelayMillis The default expiry time in milliseconds
     */
    public ExpiringHashMap(long expiryDelayMillis) {
        this.expiryDelay = expiryDelayMillis;
        ExpiryThread.references.add(new WeakReference<>(this));
    }

    /**
     * Put new key-value pair with the default expiry duration.
     */
    @Override
    public V put(K key, V value) {
        synchronized (expiryTimes) {
            expiryTimes.put(key, System.currentTimeMillis() + expiryDelay);
        }
        return super.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V remove(Object key) {
        synchronized (expiryTimes) {
            expiryTimes.remove(key);
        }
        return super.remove(key);
    }

    /**
     * Inserts a key-value pair into the cache without setting an expiration time.
     *
     * @param key   the key to put
     * @param value the value to put
     * @return the previous value associated with the key, or {@code null} if there was none
     */
    public V putNotExpiring(K key, V value) {
        return super.put(key, value);
    }

    /**
     * Inserts a key-value pair into the cache with a specific absolute expiration time.
     *
     * @param key         the key to cache
     * @param value       the value to cache
     * @param expiryTime  the absolute time (in millis since epoch) at which the entry should expire
     * @return the previous value associated with the key, or {@code null} if there was none
     * @throws IllegalArgumentException if {@code expiryTime} is in the past
     */
    public V putExpiring(K key, V value, long expiryTime) {
        if (expiryTime < System.currentTimeMillis())
            throw new IllegalArgumentException("The expiry time must be in the future");

        synchronized (expiryTimes) {
            expiryTimes.put(key, expiryTime);
        }
        return super.put(key, value);
    }

    /**
     * Retrieves the expiration timestamp for a given key.
     *
     * @param key the key to check
     * @return the expiration time in milliseconds since epoch
     * @throws IllegalArgumentException if the given key is not present in the map
     */
    public long getExpiryTime(K key) {
        if (!containsKey(key)) throw new IllegalArgumentException("The given key is not in the map");
        return expiryTimes.get(key);
    }

    /**
     * Set new expiry time for an existing key.
     *
     * @param key Existing key to modify its expiry time
     * @param expiryTimeMillis New expiry time in milliseconds
     * @throws IllegalArgumentException If this map does not contain given key.
     */
    public void setExpiryTime(K key, long expiryTimeMillis) {
        if (!containsKey(key)) throw new IllegalArgumentException("The given key is not in the map");
        expiryTimes.put(key, expiryTimeMillis);
    }

    /**
     * Get the default expiry duration of this map.
     *
     * @return The expiry duration in milliseconds
     */
    public long getExpiryDelay() {
        return expiryDelay;
    }

    @SuppressWarnings({"SuspiciousMethodCalls"})
    private void keyExpired(Object key) {
        remove(key);
        expiryTimes.remove(key);
    }

    /**
     * Expiry thread that runs every 5 second to clear any expired cache.
     */
    private static class ExpiryThread extends Thread {

        private static final Set<WeakReference<ExpiringHashMap<?, ?>>> references = new HashSet<>();

        private ExpiryThread() {
            super( "PlotSystem-Terra " + ExpiryThread.class.getSimpleName());
            Runtime.getRuntime().addShutdownHook(new Thread(this::interrupt));
        }

        @Override
        @SuppressWarnings("BusyWait")
        public void run() {
            while (!isInterrupted()) {
                long currentTime = System.currentTimeMillis();
                for (WeakReference<ExpiringHashMap<?, ?>> reference : new HashSet<>(references)) {
                    final ExpiringHashMap<?, ?> collection = reference.get();
                    if (collection == null) {
                        references.remove(reference);
                        continue;
                    }
                    Map<?, Long> expiryTimes;
                    synchronized (collection.expiryTimes) {
                        expiryTimes = new HashMap<>(collection.expiryTimes);
                    }
                    List<Object> removals = new ArrayList<>();
                    expiryTimes.entrySet().stream()
                            .filter(entry -> entry.getValue() < currentTime)
                            .forEach(entry -> removals.add(entry.getKey()));
                    synchronized (collection) {
                        removals.forEach(collection::keyExpired);
                    }
                }

                try {
                    Thread.sleep(CACHE_REFRESH_MILLIS);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }
    }
}
